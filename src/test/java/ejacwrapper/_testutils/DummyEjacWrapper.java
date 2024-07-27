package ejacwrapper._testutils;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import ejacwrapper.core.EjacWrapper;
import ejacwrapper.utils.EjacUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.util.List;

public class DummyEjacWrapper extends EjacWrapper {

    /**
     * Instead of initializing everything in the constructor, we do that in `get()` because some dependency frameworks
     * (like Quarkus) complain when you do anything outside variable initialization in the constructor.
     */
    public DummyEjacWrapper() {

    }

    /**
     * Initialization should happen in the constructor, but we do it as example.
     */
    @Override
    public synchronized ElasticsearchClient get() {
        if (this.elasticsearchClient != null) {
            return this.elasticsearchClient;
        }

        HttpHost[] httpHosts = EjacUtils.httpHostsListToArray(List.of("https://localhost:9200"));
        RestClient restClient = RestClient
                .builder(httpHosts)
                .setDefaultHeaders(EjacUtils.authorizationHeader("elastic", "elastic"))
                .setHttpClientConfigCallback(EjacUtils::unsafeClientBuilder)
                .build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        this.elasticsearchClient = new ElasticsearchClient(transport);
        this.elasticsearchAsyncClient = new ElasticsearchAsyncClient(transport);
        this.bulkIngester = BulkIngester.of(b -> b
                .client(this.elasticsearchClient)
                .maxOperations(1000)
        );

        return this.elasticsearchClient;
    }

    @Override
    public synchronized ElasticsearchAsyncClient getAsync() {
        return this.elasticsearchAsyncClient;
    }

    @Override
    public BulkIngester<Void> getBulkIngester() {
        throw new IllegalStateException("Not used in tests, we create another one");
    }


}
