package ejachelpers.mapping;

import ejachelpers._testutils.ComplexModel;
import ejachelpers._testutils.NoModel;
import ejachelpers._testutils.SimpleModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EjacMappingTest {

    @Test
    void asString() {
        String mapping;

        mapping = EjacMapping.asString(NoModel.class);
        assertEquals("""
                {"properties":{"_class":{"type":"keyword","index":false,"doc_values":false}}}
                """.trim(), mapping);

        mapping = EjacMapping.asString(SimpleModel.class);
        assertEquals("""
                {"properties":{"_class":{"type":"keyword","index":false,"doc_values":false},"stringField":{"type":"text"},"integerField":{"type":"integer"},"arrayField":{"type":"text"}}}
                """.trim(), mapping);

        mapping = EjacMapping.asString(ComplexModel.class);
        assertEquals("""
                {"properties":{"_class":{"type":"keyword","index":false,"doc_values":false},"nestedField":{"type":"nested","term_vector":"with_positions_offsets_payloads"},"objectField":{"type":"object"},"arrayField":{"type":"text","analyzer":"html_field_analyzer","search_analyzer":"html_field_analyzer"}},"_source":{"excludes":["objectField"]}}
                """.trim(), mapping);
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
