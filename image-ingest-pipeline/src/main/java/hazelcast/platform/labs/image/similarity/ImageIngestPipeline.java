package hazelcast.platform.labs.image.similarity;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.python.PythonServiceConfig;
import com.hazelcast.jet.python.PythonTransforms;
import com.hazelcast.vector.VectorValues;
import com.hazelcast.vector.jet.VectorSinks;

/*
 *
 */
public class ImageIngestPipeline {


    public static void main(String []args){
        // TODO make argument handling prettier
        if (args.length != 6){
            System.err.println("Please provide the following 6 parameters: inputDir, fileGlob, distributeReadFlag, distributeEmbeddingFlag, pythonServiceDir, pythonServiceModule");
            System.exit(1);
        }

        String inputDir = args[0];
        String fileGlob = args[1];
        boolean distributeReads = Boolean.parseBoolean(args[2]);
        boolean distributeEmbedding = Boolean.parseBoolean(args[3]);
        String pythonServiceDir = args[4];
        String pythonServiceModule = args[5];

        int fileSourceBatchSize = 2;

        HazelcastInstance hz = Hazelcast.bootstrappedInstance();

        Pipeline pipeline = createPipeline(inputDir, fileGlob, fileSourceBatchSize,distributeReads, distributeEmbedding, pythonServiceDir, pythonServiceModule);
        pipeline.setPreserveOrder(false);   // nothing in here requires order
        JobConfig config = new JobConfig();
        config.setName("Ingest: " + inputDir);
        hz.getJet().newJob(pipeline);
    }

    /*
     * Will encode jpg format image files stored in dir. The source could be run on any nodes
     * so all nodes need to have access to "dir" whether through a copy or a shared file  system.
     *
     * Setting distributeReads to true will cause the source to assume it is running on a shared file system and
     * will cause each node to read a different subset of the files in dir.  This will also implicitly cause
     * the encoding task to be distributed as well.
     *
     * Setting distributeEmbedding to true will insert a rebalance step into the pipeline between reading an
     * input file and performing vector encoding.  This will cause the encoding task to be distributed, but it
     * will require the raw image bytes to be moved across nodes.  If distributeReads is true then no
     * additional rebalance step will be added regardless of this setting.
     */
    private static Pipeline createPipeline(
            String dir,
            String glob,
            int fileSourceBatchSize,
            boolean distributeReads,
            boolean distributeEmbedding,
            String pythonServiceBaseDir,
            String pythonServiceModule){
        Pipeline pipeline = Pipeline.create();

        /*
         * Create a local file source pointing to the csv file containing patent abstracts.  The
         * source emits one event per line in the file.
         */
        BatchSource<Tuple2<String,byte[]>> directory;

        if (distributeReads)
            directory = NameAwareFileSourceBuilder.newDistributedFileSource(dir, glob, fileSourceBatchSize);
        else
            directory = NameAwareFileSourceBuilder.newFileSource(dir, glob, fileSourceBatchSize);

        BatchStage<Tuple2<String, byte[]>> imageBytes = pipeline.readFrom(directory).setName("Read Files");

        /*
         * distribute the stream to all nodes if indicated by the flags
         */
        if ( !distributeReads && distributeEmbedding)
            imageBytes = imageBytes.rebalance().setName("Distribute Images");

        ServiceFactory<?, EmbeddingServiceCodec> preProcessor =
                ServiceFactories.sharedService(ctx -> new EmbeddingServiceCodec());
        BatchStage<String> inputs = imageBytes.mapUsingService(preProcessor, EmbeddingServiceCodec::encodeInput)
                .setName("Format Input");

        // compute embeddings in python
        PythonServiceConfig pythonService =
                new PythonServiceConfig().setBaseDir(pythonServiceBaseDir).setHandlerModule(pythonServiceModule);
        BatchStage<String> outputs =
                inputs.apply(PythonTransforms.mapUsingPythonBatch(pythonService)).setName("Compute Embedding");

        ServiceFactory<?, EmbeddingServiceCodec> postProcessor =
                ServiceFactories.sharedService(ctx -> new EmbeddingServiceCodec());
        BatchStage<Tuple2<String, float[]>> vectors =
                outputs.mapUsingService(postProcessor, EmbeddingServiceCodec::decodeOutput).setName("Parse Output");

        Sink<Tuple2<String, float[]>> vectorCollection =
                VectorSinks.vectorCollection(
                        "images",    // collection name
                        Tuple2::f0,               // kee
                        Tuple2::f0,               // val
                        t -> VectorValues.of(t.f1()));  // vector

        vectors.writeTo(vectorCollection);

        return pipeline;
    }

}
