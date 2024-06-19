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
import com.hazelcast.nio.serialization.genericrecord.GenericRecordBuilder;

import java.io.File;

/*
 * Pass the file name of a file containing podcast in the following format.
 *
 * {"podcast_id":"4e5ce6a47e5d491aac3f588cfb3ece73","itunes_id":"1000016800","slug":"st-neots-evangelical-church-sermons","itunes_url":"https://podcasts.apple.com/us/podcast/st-neots-evangelical-church-sermons/id1000016800","title":null,"author":null,"description":null,"average_rating":null,"ratings_count":null,"scraped_at":null}
 * {"podcast_id":"6c476d3dd90c3fe5381153354b326952","itunes_id":"1000035657","slug":"idiotspeakshow","itunes_url":"https://podcasts.apple.com/us/podcast/idiotspeakshow/id1000035657","title":"IdiotSpeakShow","author":"IdiotSpeakShow","description":"Podcast by IdiotSpeakShow","average_rating":null,"ratings_count":null,"scraped_at":"2019-07-08 06:01:23.683147"}
 * {"podcast_id":"b9e7fdf5cd545fc8097055e2f2f1d760","itunes_id":"1000035702","slug":"conciertos-en-el-living","itunes_url":"https://podcasts.apple.com/us/podcast/conciertos-en-el-living/id1000035702","title":null,"author":null,"description":null,"average_rating":null,"ratings_count":null,"scraped_at":null}
 *
 *  The file will be parsed and loaded into the "podcasts" IMAP as GenericRecords
 */
public class PodcastIngestPipeline {

    private static final String PODCASTS_COMPACT_TYPE_NAME = "hazelcast.platform.labs.podcast.data.Podcast";
    private static final String PODCASTS_MAPPING_SQL = "CREATE OR REPLACE MAPPING podcasts (" +
            "podcast_id VARCHAR, " +
            "slug VARCHAR, " +
            "title VARCHAR, " +
            "author VARCHAR," +
            "description VARCHAR) " +
            "TYPE IMap OPTIONS (" +
            "'keyFormat' = 'java'," +
            "'keyJavaClass' = 'java.lang.String'," +
            "'valueFormat' = 'compact'," +
            "'valueCompactTypeName' = '" + PODCASTS_COMPACT_TYPE_NAME + "')";

    public static void main(String []args){
        if (args.length != 1){
            System.err.println("Please supply the name of the json-lines formatted podcast file on the command line");
            System.exit(1);
        }

        String podcastFilename = args[0];
        File podcastFile = new File(podcastFilename);
        String dir = podcastFile.getParent();
        String glob = podcastFile.getName();

        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        setup(hz);

        Pipeline pipeline = createPipeline(dir, glob);
        pipeline.setPreserveOrder(false);   // nothing in here requires order
        hz.getJet().newJob(pipeline);
    }

    private static void setup(HazelcastInstance hz){
        hz.getSql().execute(PODCASTS_MAPPING_SQL);
    }

    private static Pipeline createPipeline(String dir, String glob){
        Pipeline pipeline = Pipeline.create();

        /*
         * Create a local file source pointing to the json file containing podcasts.  The
         * source emits one event per line in the file.
         */
        FileSourceBuilder<String> fileSourceBuilder = FileSources.files(dir);
        BatchSource<String> podcastFile = fileSourceBuilder.glob(glob).format(FileFormat.lines()).build();
        BatchStage<String> lines = pipeline.readFrom(podcastFile);

        /*
         * We will use an ObjectMapper instance (from jackson-databind) to parse and manipulate
         * the input using the generic Tree model, so that we do not need domain classes.
         *
         * Perform a rebalance so that the parsing step will be distributed across all nodes.
         */
        ServiceFactory<?, ObjectMapper> objectMapperServiceFactory =
                ServiceFactories.sharedService(ctx -> new ObjectMapper());
        BatchStage<ObjectNode> jsonNode = lines.mapUsingService(
                objectMapperServiceFactory, (mapper, line) -> (ObjectNode) mapper.readTree(line));

        /*
         * Convert the json ObjectNode into a GenericRecord
         */
        BatchStage<GenericRecord> genericRecordPodcasts =
                jsonNode.map(PodcastIngestPipeline::buildPodcastGenericRecordFromJson);

        /*
         * Write it to the podcasts map
         */
        genericRecordPodcasts.writeTo(Sinks.map(
                "podcasts",
                record -> record.getString("podcast_id"),
                record -> record));

        return pipeline;
    }

    /*
     * Create a GenericRecord of hazelcast.platform.labs.podcast.data.Podcast
     * It uses the same field names for the generic record as are present in the json but only loads
     * the fields that are of interest.
     */
    private static GenericRecord buildPodcastGenericRecordFromJson(ObjectNode root){
        GenericRecordBuilder builder =  GenericRecordBuilder.compact(PODCASTS_COMPACT_TYPE_NAME);
        String []fields = new String[]{"podcast_id","slug","title","author","description"};
        for(String field: fields) {
            String val = root.get(field).asText();
            if (val != null && val.equals("null")) {
                val = null;
            } else if (field.equals("slug")) {
                val = val.replace("-", " ");
            }
            builder.setString(field, val);
        }
        return builder.build();
    }
}
