package ejacwrapper.utils;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchResponse;
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
import java.util.Base64;
import java.util.List;

/**
 * Utility class for commons issues, e.g. Elasticsearch mapping via Spring Data Elasticsearch.
 */
public class EjacUtils {


    // ------------------------------------------------------------------------------------------ //
    // MAPPING
    // ------------------------------------------------------------------------------------------ //

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


}
