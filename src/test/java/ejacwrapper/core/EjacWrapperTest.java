package ejacwrapper.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import ejacwrapper._testutils.DummyEjacWrapper;
import ejacwrapper._testutils.TestUtils;
import ejacwrapper._testutils.models.SimpleModel;
import ejacwrapper._testutils.models.UpdatedSimpleModel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EjacWrapperTest {

    private static final EjacWrapper ejacWrapper = new DummyEjacWrapper();
    private static final ElasticsearchClient esc = ejacWrapper.get();

    @Test
    void createIndexOrUpdateMapping() throws Exception {
        TestUtils.tryToDeleteIndex(SimpleModel.INDEX_NAME, esc);

        ejacWrapper.createIndexOrUpdateMapping(SimpleModel.INDEX_NAME, TestUtils.indexSettingsDummy, SimpleModel.class);
        assertTrue(TestUtils.indexExists(SimpleModel.INDEX_NAME, esc));

        // We create the index the mappings of `SimpleModel`
        Map<String, Property> properties = TestUtils.getMappingProperties(SimpleModel.INDEX_NAME, esc);
        assertEquals(4, properties.size());
        assertEquals("Property: {\"type\":\"text\"}",
                properties.get("stringField").toString());

        // Then we update the mappings of SimpleModel to `UpdatedSimpleModel` (add search_analyzer to stringField and add newField)
        ejacWrapper.createIndexOrUpdateMapping(SimpleModel.INDEX_NAME, TestUtils.indexSettingsDummy, UpdatedSimpleModel.class);
        properties = TestUtils.getMappingProperties(SimpleModel.INDEX_NAME, esc);
        assertEquals(5, properties.size());
        assertEquals("Property: {\"type\":\"text\",\"analyzer\":\"default\",\"search_analyzer\":\"standard\"}",
                properties.get("stringField").toString());
        assertEquals("Property: {\"type\":\"text\"}",
                properties.get("newField").toString());

        // Then we run the method again with `UpdatedSimpleModel` to make sure it's idempotent
        ejacWrapper.createIndexOrUpdateMapping(SimpleModel.INDEX_NAME, TestUtils.indexSettingsDummy, UpdatedSimpleModel.class);
        properties = TestUtils.getMappingProperties(SimpleModel.INDEX_NAME, esc);
        assertEquals(5, properties.size());
        assertEquals("Property: {\"type\":\"text\",\"analyzer\":\"default\",\"search_analyzer\":\"standard\"}",
                properties.get("stringField").toString());
        assertEquals("Property: {\"type\":\"text\"}",
                properties.get("newField").toString());
    }
}
