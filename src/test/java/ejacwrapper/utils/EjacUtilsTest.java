package ejacwrapper.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EjacUtilsTest {

    @Test
    @SuppressWarnings("unchecked")
    void asMapWithoutNullValues() {
        Map<String, Object> cleanMap = Map.of("field", EjacUtils.asMapWithoutNullValues(
                new Pojo(
                        "value",
                        new Date(0),
                        new ArrayList<>(),
                        null,
                        null)
        ));
        Map<String, Object> obj = (Map<String, Object>) cleanMap.get("field");
        assertNotNull(obj);
        assertEquals("value", obj.get("stringField"));
        assertEquals("1970-01-01T00:00:00.000Z", obj.get("dateField"));
        assertEquals(0, ((List<String>) obj.get("listField1")).size());
        assertNull(obj.get("nullField1"));
        assertNull(obj.get("nullField2"));
    }

    private record Pojo(String stringField,
                        Date dateField,
                        List<String> listField1,
                        String nullField1,
                        List<String> nullField2) {

    }
}
