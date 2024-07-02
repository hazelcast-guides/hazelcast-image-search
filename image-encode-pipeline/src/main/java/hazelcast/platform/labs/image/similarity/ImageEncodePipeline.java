package hazelcast.platform.labs.image.similarity;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.pipeline.file.FileFormat;
import com.hazelcast.jet.pipeline.file.FileSourceBuilder;
import com.hazelcast.jet.pipeline.file.FileSources;
import com.hazelcast.vector.VectorDocument;
import com.hazelcast.vector.VectorValues;
import com.hazelcast.vector.jet.VectorSinks;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.util.List;

/*
 *
 */
public class ImageEncodePipeline {


    public static void main(String []args){
        if (args.length != 1){
            System.err.println("Please download https://www.kaggle.com/datasets/dheerajmpai/us-patent-abstracts-vector-indexed and  supply the name of the csv formatted file on the command line.");
            System.exit(1);
        }

        String patentFilename = args[0];
        File patentFile = new File(patentFilename);
        String dir = patentFile.getParent();
        String glob = patentFile.getName();

        HazelcastInstance hz = Hazelcast.bootstrappedInstance();

        Pipeline pipeline = createPipeline(dir, glob);
        pipeline.setPreserveOrder(false);   // nothing in here requires order
        hz.getJet().newJob(pipeline);
    }

    private static Pipeline createPipeline(String dir, String glob){
        Pipeline pipeline = Pipeline.create();

        /*
         * Create a local file source pointing to the csv file containing patent abstracts.  The
         * source emits one event per line in the file.
         */
        FileSourceBuilder<String> fileSourceBuilder = FileSources.files(dir);
        BatchSource<String[]> patentFile = fileSourceBuilder.glob(glob).format(FileFormat.csv((List<String>) null)).build();
        BatchStage<String[]> lines = pipeline.readFrom(patentFile);


        ServiceFactory<?, AllMiniLmL6V2EmbeddingModel> embeddingService =
                ServiceFactories.nonSharedService(ctx -> new AllMiniLmL6V2EmbeddingModel());

        BatchStage<Tuple3<String, String, float[]>> vectors = lines.mapUsingService(embeddingService,
                (model, line) -> {
                    if (line == null || line.length != 3) {
                        LogManager.getLogger(ImageEncodePipeline.class).warn("Ignoring malformed line: " + line);
                        return null;
                    }
                    String key = line[0];
                    if (key == null || key.length()  == 0){
                        LogManager.getLogger(ImageEncodePipeline.class).warn("Ignoring line without publication number: " + line);
                        return null;
                    }
                    String patentAbstract = line[1];
                    if (patentAbstract == null || patentAbstract.length() == 0){
                        LogManager.getLogger(ImageEncodePipeline.class).warn("Ignoring line npo abstract: " + line);
                        return null;
                    }

                    try {
                        float[] embedding = model.embed(patentAbstract).content().vector();
                        return Tuple3.tuple3(key, patentAbstract, embedding);
                    } catch(Exception x){
                        return null;
                    }
                });

        vectors.writeTo(VectorSinks.vectorCollection(
                "patents",
                t3 -> t3.f0(),
                t3 -> VectorDocument.of(t3.f1(), VectorValues.of(t3.f2()))));

        return pipeline;
    }

}
