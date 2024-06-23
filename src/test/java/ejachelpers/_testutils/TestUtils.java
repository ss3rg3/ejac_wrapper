package ejachelpers._testutils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class TestUtils {

    public static String minifyJson(String prettyJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(prettyJson.trim());
            return objectMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Won't throw an exception if the index doesn't exist.
     */
    public static void deleteIndex(ElasticsearchClient esc, String indexName) {
        try {
            esc.indices().delete(req -> req.index(indexName));
        } catch (IOException | ElasticsearchException e) {
            // NO OP
        }
    }

}
