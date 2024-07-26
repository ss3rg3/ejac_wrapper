package ejacwrapper.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import ejacwrapper._testutils.EjacClientFactory;
import ejacwrapper._testutils.TestUtils;
import ejacwrapper._testutils.models.ComplexModel;
import ejacwrapper._testutils.models.NoModel;
import ejacwrapper._testutils.models.SimpleModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static ejacwrapper._testutils.TestUtils.minifyJson;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EjacMappingTest {

    private static final EjacClientFactory ejacClientFactory = new EjacClientFactory();
    private static final ElasticsearchClient esc = ejacClientFactory.get();

    @Test
    void noModel() {
        String mapping = EjacUtils.mappingAsString(NoModel.class, true);
        assertEquals(minifyJson("""
                {
                    "mappings": {
                        "properties": {
                            "_class": {
                                "type": "keyword",
                                "index": false,
                                "doc_values": false
                            }
                        }
                    }
                }
                """), mapping);
    }

    @Test
    void simpleModel() {
        String mapping = EjacUtils.mappingAsString(SimpleModel.class, true);
        assertEquals(minifyJson("""
                {
                    "mappings": {
                        "properties": {
                            "_class": {
                                "type": "keyword",
                                "index": false,
                                "doc_values": false
                            },
                            "stringField": {
                                "type": "text"
                            },
                            "integerField": {
                                "type": "integer"
                            },
                            "arrayField": {
                                "type": "text"
                            }
                        }
                    }
                }
                """), mapping);
    }

    @Test
    void complexModel() {
        String mapping = EjacUtils.mappingAsString(ComplexModel.class, true);
        assertEquals(minifyJson("""
                {
                    "mappings": {
                        "properties": {
                            "_class": {
                                "type": "keyword",
                                "index": false,
                                "doc_values": false
                            },
                            "nestedField": {
                                "type": "nested",
                                "properties": {
                                    "_class": {
                                        "type": "keyword",
                                        "index": false,
                                        "doc_values": false
                                    },
                                    "nestedField": {
                                        "type": "text",
                                        "index": false,
                                        "term_vector": "with_positions_offsets_payloads"
                                    }
                                }
                            },
                            "objectField": {
                                "type": "object",
                                "properties": {
                                    "_class": {
                                        "type": "keyword",
                                        "index": false,
                                        "doc_values": false
                                    },
                                    "name": {
                                        "type": "text",
                                        "index": false
                                    },
                                    "age": {
                                        "type": "integer"
                                    }
                                }
                            },
                            "mapField": {
                                "type": "object",
                                "properties": {
                                    "_class": {
                                        "type": "keyword",
                                        "index": false,
                                        "doc_values": false
                                    },
                                    "name": {
                                        "store": true,
                                        "type": "text"
                                    },
                                    "description": {
                                        "type": "text",
                                        "search_analyzer": "standard"
                                    }
                                }
                            }
                        },
                        "_source": {
                            "excludes": [
                                "objectField.age",
                                "mapField"
                            ]
                        }
                    }
                }
                """), mapping);
    }

    @Test
    void asInputStream() throws Exception {
        try (InputStream stream = EjacUtils.mappingAsInputStream(SimpleModel.class, true)) {
            String asString = EjacUtils.mappingAsString(SimpleModel.class, true);
            String fromStream = new String(stream.readAllBytes());
            assertEquals(asString, fromStream);
        }
    }

    @Test
    void mappingInIndexSimpleModel() {
        Map<String, Property> properties = recreateIndexGetProperties(SimpleModel.INDEX_NAME, SimpleModel.class);
        assertEquals("Property: {\"type\":\"text\"}", properties.get("arrayField").toString());
        assertEquals("Property: {\"type\":\"integer\"}", properties.get("integerField").toString());
        assertEquals("Property: {\"type\":\"keyword\",\"doc_values\":false,\"index\":false}", properties.get("_class").toString());
        assertEquals("Property: {\"type\":\"text\"}", properties.get("stringField").toString());
        assertEquals(4, properties.size());
    }

    @Test
    void mappingInIndexComplexModel() {
        Map<String, Property> properties = recreateIndexGetProperties(ComplexModel.INDEX_NAME, ComplexModel.class);
        assertEquals("Property: {\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"text\",\"index\":false},\"_class\":{\"type\":\"keyword\",\"doc_values\":false,\"index\":false},\"age\":{\"type\":\"integer\"}}}",
                properties.get("objectField").toString());
        assertEquals("Property: {\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"text\",\"store\":true},\"description\":{\"type\":\"text\",\"analyzer\":\"default\",\"search_analyzer\":\"standard\"},\"_class\":{\"type\":\"keyword\",\"doc_values\":false,\"index\":false}}}",
                properties.get("mapField").toString());
        assertEquals("Property: {\"type\":\"nested\",\"properties\":{\"nestedField\":{\"type\":\"text\",\"index\":false,\"term_vector\":\"with_positions_offsets_payloads\"},\"_class\":{\"type\":\"keyword\",\"doc_values\":false,\"index\":false}}}",
                properties.get("nestedField").toString());
        assertEquals(4, properties.size());
    }

    private static Map<String, Property> recreateIndexGetProperties(String indexName, Class<?> clazz) {
        try {
            TestUtils.tryToDeleteIndex(indexName, esc);
            esc.indices().create(req -> req
                    .index(indexName)
                    .withJson(EjacUtils.mappingAsInputStream(clazz, true))
            );
            return TestUtils.getMappingProperties(indexName, esc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
