package ejacwrapper.utils;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import jakarta.annotation.Nullable;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Utility class to generate Elasticsearch mapping via Spring Data Elasticsearch.
 */
public class EjacUtils {

    private static final SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
    private static final MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
    private static final MappingBuilder mappingBuilder = new MappingBuilder(converter);

    /**
     * `wrapInMappings=true` is needed when creating a new index. When updating an existing index, it must be `false`.
     */
    public static String mappingAsString(Class<?> clazz, boolean wrapInMappings) {
        if (wrapInMappings) {
            return "{\"mappings\":" + mappingBuilder.buildPropertyMapping(clazz) + "}";
        }
        return mappingBuilder.buildPropertyMapping(clazz);
    }

    /**
     * `wrapInMappings=true` is needed when creating a new index. When updating an existing index, it must be `false`.
     */
    public static InputStream mappingAsInputStream(Class<?> clazz, boolean wrapInMappings) {
        return new ByteArrayInputStream(mappingAsString(clazz, wrapInMappings).getBytes(Charset.defaultCharset()));
    }

    /**
     * `sort` field from the search response for `search_after` pagination.
     */
    @Nullable
    public static <T> List<FieldValue> getLastSortValue(SearchResponse<T> response) {
        if (response.hits().hits().isEmpty()) {
            return null;
        }
        return response.hits().hits().get(response.hits().hits().size() - 1).sort();
    }

    public static <T> boolean hasHits(SearchResponse<T> response) {
        return !response.hits().hits().isEmpty();
    }

}
