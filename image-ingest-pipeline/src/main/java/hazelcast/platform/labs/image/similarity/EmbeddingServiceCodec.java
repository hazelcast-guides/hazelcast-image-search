package hazelcast.platform.labs.image.similarity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.jet.datamodel.Tuple2;
import hazelcast.platform.labs.jet.connectors.DirectoryWatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * This class is responsible for unpacking a json document of the following format
 *
 * TODO
 *
 * into q float []
 */
public class EmbeddingServiceCodec {
    private final Logger log = LogManager.getLogger();

    // safe for concurrent use
    private final ObjectMapper mapper;
    private String wwwServer;

    public EmbeddingServiceCodec(String wwwServer){
        this.wwwServer = wwwServer;
        this.mapper = new ObjectMapper();
    }

    public Tuple2<String, float[]> decodeOutput(String json) throws JsonProcessingException {
        JsonNode root = mapper.readTree(json);
        if (root.has("exception")){
            log.error(root.get("exception").asText());
            return null;  // RETURN
        }

        String filename = root.get("metadata").asText();

        float []vector = new float[root.get("vector").size()];
        int i=0;
        for (JsonNode node : root.get("vector")) {
            vector[i++] = (float) node.asDouble();
        }
        return Tuple2.tuple2(filename, vector);
    }

    public String encodeInput(Tuple2<DirectoryWatcher.EventType, String> input){
        return wwwServer + "/" + input.f1();
    }

}
