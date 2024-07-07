package hazelcast.platform.labs.image.similarity;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.vector.VectorValues;
import com.hazelcast.vector.jet.VectorSinks;

import java.util.UUID;

/*
 *
 */
public class ImageEncodePipeline {


    public static void main(String []args){
        if (args.length != 1){
            System.err.println("Please provide the path to a directory containing .jpg files to be ingested");
            System.exit(1);
        }

        String dir = args[0];

        HazelcastInstance hz = Hazelcast.bootstrappedInstance();

        Pipeline pipeline = createPipeline(dir, "*.jpg", 2,true, false);
        pipeline.setPreserveOrder(false);   // nothing in here requires order
        JobConfig config = new JobConfig();
        config.setName("Ingest: " + dir);
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
     * Setting distributeEncoding to true will insert a rebalance step into the pipeline between reading an
     * input file and performing vector encoding.  This will cause the encoding task to be distributed, but it
     * will require the raw image bytes to be moved across nodes.  If distributeReads is true then no
     * additional rebalance step will be added regardless of this setting.
     */
    private static Pipeline createPipeline(
            String dir,
            String glob,
            int fileSourceBatchSize,
            boolean distributeReads,
            boolean distributeEncoding){
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
        if ( !distributeReads && distributeEncoding)
            imageBytes = imageBytes.rebalance().setName("Distribute Images");


        /*
         * encode the image as a vector using the CLIP model
         */
        ServiceFactory<?, ImageEncoder> imageEncoderServiceFactory =
                ServiceFactories.nonSharedService(ctx -> ImageEncoder.newInstance(), ImageEncoder::close);

        BatchStage<Tuple2<String, float[]>> imageVectors = imageBytes.mapUsingService(
                        imageEncoderServiceFactory,
                        (encoder, t) -> Tuple2.tuple2(t.f0(), encoder.encodeImage(t.f1()))
                )
                .setName("Encode Image");

        Sink<Tuple2<String, float[]>> vectorCollection =
                VectorSinks.vectorCollection(
                        "images",
                        t -> UUID.randomUUID().toString(),
                        Tuple2::f0,
                        t -> VectorValues.of(t.f1()));

        imageVectors.writeTo(vectorCollection);

        return pipeline;
    }

}
