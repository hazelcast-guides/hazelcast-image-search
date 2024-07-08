package hazelcast.platform.labs.image.similarity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;

/*
 * This class is responsible for unpacking a json document of the following format
 *
 * TODO
 *
 * into q float []
 */
public class FloatArrayDecoder {
    private final ObjectMapper mapper;

    public FloatArrayDecoder(){
        this.mapper = new ObjectMapper();
    }

    public float[]decode(String json) throws JsonProcessingException {
        JsonNode root = mapper.readTree(json);
        float []result = new float[root.size()];
        int i=0;
        for (JsonNode node : root) {
            result[i++] = (float) node.asDouble();
        }
        return result;
    }

    /*
     *
     */
    public static void main(String []args){
        String test1 = "[22.1, 1, 0.123456789, 0.00123456789, 1e-09]";
        FloatArrayDecoder decoder = new FloatArrayDecoder();
        try {
            System.out.println("input: "+ test1 + "\noutput: " + Arrays.toString(decoder.decode(test1)) + "\n");
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
