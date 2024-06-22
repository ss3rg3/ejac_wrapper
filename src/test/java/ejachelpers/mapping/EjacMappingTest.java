package ejachelpers.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ejachelpers._testutils.ComplexModel;
import ejachelpers._testutils.NoModel;
import ejachelpers._testutils.SimpleModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static ejachelpers._testutils.TestUtils.minifyJson;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EjacMappingTest {

    @Test
    void noModel() {
        String mapping = EjacMapping.asString(NoModel.class);
        assertEquals(minifyJson("""
                {
                    "properties": {
                        "_class": {
                            "type": "keyword",
                            "index": false,
                            "doc_values": false
                        }
                    }
                }
                """), mapping);
    }

    @Test
    void simpleModel() {
        String mapping = EjacMapping.asString(SimpleModel.class);
        assertEquals(minifyJson("""
                {
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
                """), mapping);
    }

    @Test
    void complexModel() {
        String mapping = EjacMapping.asString(ComplexModel.class);
        assertEquals(minifyJson("""
                {
                    "properties": {
                        "_class": {
                            "type": "keyword",
                            "index": false,
                            "doc_values": false
                        },
                        "nestedField": {
                            "type": "nested",
                            "term_vector": "with_positions_offsets_payloads"
                        },
                        "objectField": {
                            "type": "object"
                        },
                        "arrayField": {
                            "type": "text",
                            "analyzer": "html_field_analyzer",
                            "search_analyzer": "html_field_analyzer"
                        }
                    },
                    "_source": {
                        "excludes": [
                            "objectField"
                        ]
                    }
                }
                """), mapping);
    }

    @Test
    void asInputStream() throws Exception {
        try(InputStream stream = EjacMapping.asInputStream(SimpleModel.class)) {
            String asString = EjacMapping.asString(SimpleModel.class);
            String fromStream = new String(stream.readAllBytes());
            assertEquals(asString, fromStream);
        }
    }

}
