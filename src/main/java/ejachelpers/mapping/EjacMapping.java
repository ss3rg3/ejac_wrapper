package ejachelpers.mapping;

import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

public class EjacMapping {

    private static final SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
    private static final MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
    private static final MappingBuilder mappingBuilder = new MappingBuilder(converter);

    public static String asString(Class<?> clazz) {
        return "{\"mappings\":" + mappingBuilder.buildPropertyMapping(clazz) + "}";
    }

    public static InputStream asInputStream(Class<?> clazz) {
        return new ByteArrayInputStream(asString(clazz).getBytes(Charset.defaultCharset()));
    }

}
