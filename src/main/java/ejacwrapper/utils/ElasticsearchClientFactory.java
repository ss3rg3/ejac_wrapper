package ejacwrapper.utils;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import ejacwrapper.core.EjacWrapper;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

public interface ElasticsearchClientFactory {

    ElasticsearchClient get();

    ElasticsearchAsyncClient getAsync();

    EjacWrapper getEjacWrapper();

    void createOrUpdateIndex(String indexName, IndexSettings indexSettings, Class<?> model) throws IOException;

    static String asBase64(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        return "Basic " + new String(encodedAuth);
    }

    static HttpAsyncClientBuilder unsafeClientBuilder(HttpAsyncClientBuilder builder) {
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

    static Header[] authorizationHeader(String user, String password) {
        return new Header[]{
                new BasicHeader("Authorization", ElasticsearchClientFactory.asBase64(user, password))
        };
    }

    static HttpHost[] httpHostsListToArray(List<String> httpHosts) {
        return httpHosts.stream()
                .map(HttpHost::create)
                .toList()
                .toArray(HttpHost[]::new);
    }

}
