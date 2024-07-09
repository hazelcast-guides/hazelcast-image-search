package hazelcast.platform.labs.image.similarity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.jet.datamodel.Tuple2;

import java.util.Arrays;
import java.util.Base64;

/*
 * This class is responsible for unpacking a json document of the following format
 *
 * TODO
 *
 * into q float []
 */
public class EmbeddingServiceCodec {
    // safe for concurrent use
    private final ObjectMapper mapper;
    // safe for concurrent use
    private final Base64.Encoder b64Encoder;

    public EmbeddingServiceCodec(){
        this.b64Encoder = Base64.getEncoder();
        this.mapper = new ObjectMapper();
    }

    public Tuple2<String, float[]> decodeOutput(String json) throws JsonProcessingException {
        JsonNode root = mapper.readTree(json);
        String filename = root.get("metadata").asText();

        float []vector = new float[root.get("content").size()];
        int i=0;
        for (JsonNode node : root.get("content")) {
            vector[i++] = (float) node.asDouble();
        }
        return Tuple2.tuple2(filename, vector);
    }

    public String encodeInput(Tuple2<String, byte[]> input){
        String base64Str = b64Encoder.encodeToString(input.f1());
        return "{ \"metadata\": \"" + input.f0() + " , \"content\": \"" + base64Str + "\" }";
    }

    /*
     *
     */
    public static void main(String []args){
        String test1 = "{ \"metadata\": \"myfile.jpg\", \"content\": [22.1, 1, 0.123456789, 0.00123456789, 1e-09]}";
        EmbeddingServiceCodec decoder = new EmbeddingServiceCodec();
        try {
            Tuple2<String, float[]> t = decoder.decodeOutput(test1);
            System.out.println("input: "+ test1 + "\noutput: " + t.f0() + " -> " + Arrays.toString(t.f1()) + "\n");
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
