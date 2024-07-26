package ejacwrapper._testutils;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import ejacwrapper.core.EjacWrapper;
import ejacwrapper.utils.ElasticsearchClientFactory;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.List;

public class EjacClientFactory implements ElasticsearchClientFactory {

    private final ElasticsearchClient esc;
    private final ElasticsearchAsyncClient escAsync;
    private final EjacWrapper ejacWrapper;

    public EjacClientFactory() {
        HttpHost[] httpHosts = ElasticsearchClientFactory.httpHostsListToArray(List.of("https://localhost:9200"));
        RestClient restClient = RestClient
                .builder(httpHosts)
                .setDefaultHeaders(ElasticsearchClientFactory.authorizationHeader("elastic", "elastic"))
                .setHttpClientConfigCallback(ElasticsearchClientFactory::unsafeClientBuilder)
                .build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        this.esc = new ElasticsearchClient(transport);
        this.escAsync = new ElasticsearchAsyncClient(transport);
        this.ejacWrapper = new EjacWrapper(this.esc);
    }

    @Override
    public ElasticsearchClient get() {
        return this.esc;
    }

    @Override
    public ElasticsearchAsyncClient getAsync() {
        return this.escAsync;
    }

    @Override
    public EjacWrapper getEjacWrapper() {
        return this.ejacWrapper;
    }

    @Override
    public void createOrUpdateIndex(String indexName, IndexSettings indexSettings, Class<?> model) throws IOException {

    }

}
