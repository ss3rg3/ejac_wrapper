package ejachelpers.indices;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import ejachelpers._testutils.EjacClientFactory;
import ejachelpers._testutils.SimpleModel;
import ejachelpers._testutils.TestUtils;
import ejachelpers._testutils.UpdatedSimpleModel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EjacIndicesTest {

    private static final ElasticsearchClient esc = EjacClientFactory.create();
    private static final EjacIndices ejacIndices = new EjacIndices(esc);

    @Test
    void createIndexOrUpdateMapping() throws Exception {
        TestUtils.tryToDeleteIndex(SimpleModel.INDEX_NAME, esc);

        ejacIndices.createIndexOrUpdateMapping(SimpleModel.INDEX_NAME, SimpleModel.class);
        assertTrue(TestUtils.indexExists(SimpleModel.INDEX_NAME, esc));

        // We create the index the mappings of `SimpleModel`
        Map<String, Property> properties = TestUtils.getMappingProperties(SimpleModel.INDEX_NAME, esc);
        assertEquals(4, properties.size());
        assertEquals("Property: {\"type\":\"text\"}",
                properties.get("stringField").toString());

        // Then we update the mappings of SimpleModel to `UpdatedSimpleModel` (add search_analyzer to stringField and add newField)
        ejacIndices.createIndexOrUpdateMapping(SimpleModel.INDEX_NAME, UpdatedSimpleModel.class);
        properties = TestUtils.getMappingProperties(SimpleModel.INDEX_NAME, esc);
        assertEquals(5, properties.size());
        assertEquals("Property: {\"type\":\"text\",\"analyzer\":\"default\",\"search_analyzer\":\"standard\"}",
                properties.get("stringField").toString());
        assertEquals("Property: {\"type\":\"text\"}",
                properties.get("newField").toString());

        // Then we run the method again with `UpdatedSimpleModel` to make sure it's idempotent
        ejacIndices.createIndexOrUpdateMapping(SimpleModel.INDEX_NAME, UpdatedSimpleModel.class);
        properties = TestUtils.getMappingProperties(SimpleModel.INDEX_NAME, esc);
        assertEquals(5, properties.size());
        assertEquals("Property: {\"type\":\"text\",\"analyzer\":\"default\",\"search_analyzer\":\"standard\"}",
                properties.get("stringField").toString());
        assertEquals("Property: {\"type\":\"text\"}",
                properties.get("newField").toString());
    }
}
