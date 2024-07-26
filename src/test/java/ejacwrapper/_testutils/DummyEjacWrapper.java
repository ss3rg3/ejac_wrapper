package ejacwrapper._testutils;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import ejacwrapper.core.EjacWrapper;
import ejacwrapper.utils.EjacUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.util.List;

public class DummyEjacWrapper extends EjacWrapper {

    public DummyEjacWrapper() {
        super(null);
    }

    @Override
    public synchronized ElasticsearchClient get() {
        if (this.esc != null) {
            return this.esc;
        }

        HttpHost[] httpHosts = EjacUtils.httpHostsListToArray(List.of("https://localhost:9200"));
        RestClient restClient = RestClient
                .builder(httpHosts)
                .setDefaultHeaders(EjacUtils.authorizationHeader("elastic", "elastic"))
                .setHttpClientConfigCallback(EjacUtils::unsafeClientBuilder)
                .build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        this.esc = new ElasticsearchClient(transport);
        this.escAsync = new ElasticsearchAsyncClient(transport);

        return this.esc;
    }

    @Override
    public synchronized ElasticsearchAsyncClient getAsync() {
        return this.escAsync;
    }

}
