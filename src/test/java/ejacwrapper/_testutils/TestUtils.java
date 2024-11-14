package ejacwrapper._testutils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class TestUtils {

    public static final IndexSettings indexSettingsDummy = IndexSettings.of(is -> is
            .numberOfReplicas("5")
            .numberOfShards("5"));

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
    public static void tryToDeleteIndex(String indexName, ElasticsearchClient esc) {
        try {
            esc.indices().delete(req -> req.index(indexName));
        } catch (IOException | ElasticsearchException e) {
            // NO OP
        }
    }

    public static boolean indexExists(String indexName, ElasticsearchClient esc) {
        try {
            return esc.indices().exists(req -> req.index(indexName)).value();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Property> getMappingProperties(String indexName, ElasticsearchClient esc) {
        try {
            GetMappingResponse response = esc.indices().getMapping(req ->
                    req.index(indexName));
            return response.result().values()
                    .iterator().next()
                    .mappings().properties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
