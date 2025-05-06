package ejacwrapper.utils;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility class for commons issues, e.g. Elasticsearch mapping via Spring Data Elasticsearch.
 */
public class EjacUtils {

    private static final SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
    private static final MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
    private static final MappingBuilder mappingBuilder = new MappingBuilder(converter);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final DateFormat UTC_FORMAT;

    static {
        UTC_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.UK);
        UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setDateFormat(UTC_FORMAT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // ------------------------------------------------------------------------------------------ //
    // MAPPING
    // ------------------------------------------------------------------------------------------ //

    /**
     * This is the value you get from the `_mappings/` endpoint, i.e. `"{mappings": {"properties": {...}}}`.
     * It's needed when creating a new index.
     */
    public static String asMappingsNode(Class<?> clazz, DynamicMapping dynamicMapping) {
        try {
            return mapper.writeValueAsString(Map.of("mappings", convertToObjectNode(clazz, dynamicMapping)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This is the same as `asMappingsNode`, but without the `mappings` key wrapping it.
     * It's needed when updating an existing index.
     */
    public static String asValueForMappingsNode(Class<?> clazz, DynamicMapping dynamicMapping) {
        try {
            return mapper.writeValueAsString(convertToObjectNode(clazz, dynamicMapping));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectNode convertToObjectNode(Class<?> clazz, DynamicMapping dynamicMapping) {
        try {
            ObjectNode mappingsValue = (ObjectNode) mapper.readTree(mappingBuilder.buildPropertyMapping(clazz));
            mappingsValue.put("dynamic", dynamicMapping.jsonValue());
            return mappingsValue;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream asInputStream(String value) {
        return new ByteArrayInputStream(value.getBytes(Charset.defaultCharset()));
    }


    // ------------------------------------------------------------------------------------------ //
    // CLIENT
    // ------------------------------------------------------------------------------------------ //

    public static String asBase64(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        return "Basic " + new String(encodedAuth);
    }

    public static HttpAsyncClientBuilder unsafeClientBuilder(HttpAsyncClientBuilder builder) {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{UnsafeX509ExtendedTrustManager.INSTANCE}, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
        return builder
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier((hostNameVerifier, sslSession) -> true);
    }

    public static Header[] authorizationHeader(String user, String password) {
        return new Header[]{
                new BasicHeader("Authorization", asBase64(user, password))
        };
    }

    public static HttpHost[] httpHostsListToArray(List<String> httpHosts) {
        return httpHosts.stream()
                .map(HttpHost::create)
                .toList()
                .toArray(HttpHost[]::new);
    }


    // ------------------------------------------------------------------------------------------ //
    // SEARCH
    // ------------------------------------------------------------------------------------------ //

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


    // ------------------------------------------------------------------------------------------ //
    // INDEXING
    // ------------------------------------------------------------------------------------------ //

    /**
     * When you use `Map.of()` to create a document for indexing, then it will contain all fields including whose
     * values are null. This will bloat your `_source` field and create confusion (the fields won't exist in the index
     * but are shown in `_source` with value `null`, e.g. "does an `exists` query now work or not??").<br>
     * - This uses Jackson to map the given to a clean Map via `JsonInclude.Include.NON_NULL`<br>
     * - Date fields are turned into objects into string with format `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` (ISO 8601)
     */
    public static Map<String, Object> asMapWithoutNullValues(Object obj) {
        return objectMapper.convertValue(obj, new TypeReference<>() {
        });
    }


}
