package ejachelpers._testutils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public abstract class EjacClientFactory {

    private static String asBase64(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        return "Basic " + new String(encodedAuth);
    }

    public static ElasticsearchClient create() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{UnsafeX509ExtendedTrustManager.INSTANCE}, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }

        RestClient restClient = RestClient
                .builder(HttpHost.create("https://localhost:9200"))
                .setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", asBase64("elastic", "elastic"))
                })
                .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier((hostNameVerifier, sslSession) -> true))
                .build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
