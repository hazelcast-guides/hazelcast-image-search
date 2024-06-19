package hazelcast.platform.labs.podcast.reviews;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.pipeline.file.FileFormat;
import com.hazelcast.jet.pipeline.file.FileSourceBuilder;
import com.hazelcast.jet.pipeline.file.FileSources;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;

import java.io.File;

/*
 * Pass the file name of a file containing podcast in the following format.
 *
 * {"podcast_id":"52e3d2c4fab4e80a8bb75ad144671d96","title":"New listener","content":"I have been a Howard Stern listener for years. He mentioned that he’d be doing the Fresh Air show. I really enjoyed it and love Terri’s soothing voice and interview style. I saw one commenter trashing Howard Stern — I mean, wow, did you even listen to the interview? His show has evolved over the years. The interviews are better, his let his staff really drive the content, and he’s matured. Check it out if you have Sirius (or check out some clips from the HS Facebook page).\n\nAnyway, you have a new fan and will continue to listen to more episodes of Fresh Air!","rating":5,"author_id":"05453b5b6dcbf25","created_at":"2019-06-07 00:14:59+00"}
 * {"podcast_id":"52e3d2c4fab4e80a8bb75ad144671d96","title":"Adam Liptack","content":"Clear, reasoned, and non-political. One of your best guests in years. \nWe need to hear from him more often.","rating":5,"author_id":"82c2bbedf772cad","created_at":"2019-06-06 15:52:42+00"}
 * {"podcast_id":"52e3d2c4fab4e80a8bb75ad144671d96","title":"Thank you🙏🏻","content":"So I love you so so much","rating":5,"author_id":"8057d2e67d3b8ef","created_at":"2019-04-25 21:19:47+00"}
 * {"podcast_id":"52e3d2c4fab4e80a8bb75ad144671d96","title":"Please fix subscribe button","content":"Love this show. Always listen to a few episodes per week. Terry Gross is such an ethical & intelligent interviewer. She definitely deserved the presidential medal given to her by Obama. But please fix the subscribe button! It keeps subscribing me to Code Switch instead!","rating":5,"author_id":"6295e27f5cf056a","created_at":"2019-06-11 17:32:59+00"}
 *
 *  The file will be parsed and loaded into the "podcasts" IMAP as GenericRecords
 */
public class OriginalReviewIngestPipeline {

//    private static final String REVIEWS_COMPACT_TYPE_NAME = "hazelcast.platform.labs.podcast.data.Review";
//    private static final String PODCASTS_MAPPING_SQL = "CREATE OR REPLACE MAPPING reviews (" +
//            "podcast_id VARCHAR, " +
//            "title VARCHAR, " +
//            "content VARCHAR)" +
//            "TYPE IMap OPTIONS (" +
//            "'keyFormat' = 'java'," +
//            "'keyJavaClass' = 'java.lang.String'," +
//            "'valueFormat' = 'compact'," +
//            "'valueCompactTypeName' = '" + REVIEWS_COMPACT_TYPE_NAME + "')";

    public static void main(String []args){
        if (args.length != 1){
            System.err.println("Please supply the name of the json-lines formatted review file on the command line");
            System.exit(1);
        }

        String podcastFilename = args[0];
        File podcastFile = new File(podcastFilename);
        String dir = podcastFile.getParent();
        String glob = podcastFile.getName();

        HazelcastInstance hz = Hazelcast.bootstrappedInstance();

        Pipeline pipeline = createPipeline(dir, glob);
        pipeline.setPreserveOrder(false);   // nothing in here requires order
        hz.getJet().newJob(pipeline);
    }

//    private static void setup(HazelcastInstance hz){
//        hz.getSql().execute(PODCASTS_MAPPING_SQL);
//    }

    private static Pipeline createPipeline(String dir, String glob){
        Pipeline pipeline = Pipeline.create();

        /*
         * Create a local file source pointing to the json file containing podcasts.  The
         * source emits one event per line in the file.
         */
        FileSourceBuilder<String> fileSourceBuilder = FileSources.files(dir);
        BatchSource<String> reviewFile = fileSourceBuilder.glob(glob).format(FileFormat.lines()).build();
        BatchStage<String> lines = pipeline.readFrom(reviewFile);

        /*
         * We will use an ObjectMapper instance (from jackson-databind) to parse and manipulate
         * the input using the generic Tree model, so that we do not need domain classes.  Perform
         * a rebalance so that the parsing step will be distributed across all nodes.
         */
        ServiceFactory<?, ObjectMapper> objectMapperServiceFactory =
                ServiceFactories.sharedService(ctx -> new ObjectMapper());
        BatchStage<ObjectNode> jsonNode = lines.rebalance().mapUsingService(
                objectMapperServiceFactory, (mapper, line) -> (ObjectNode) mapper.readTree(line));

        BatchStage<ObjectNode> enhancedNodes = jsonNode.<String, GenericRecord, ObjectNode>mapUsingIMap(
                "podcasts",
                node -> node.get("podcast_id").asText(),
                (node, genericPodcast) -> {
                    for (String field : new String[]{"slug", "title", "author", "description"}) {
                        if (genericPodcast != null)
                            node.put(field, genericPodcast.getString(field));
                        else
                            node.put(field, "");
                    }
                    return node;
                });

        enhancedNodes.writeTo(Sinks.logger());
        return pipeline;
    }

}
