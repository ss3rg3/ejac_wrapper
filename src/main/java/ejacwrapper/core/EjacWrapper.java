package ejacwrapper.core;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import ejacwrapper.utils.EjacUtils;

import java.io.IOException;

/**
 * If you're using a dependency injection framework which complain when you do anything outside variable
 * initialization, you can pass null and implement `get()` as a synchronized singleton creator and getter.
 */
public abstract class EjacWrapper {

    protected volatile ElasticsearchClient elasticsearchClient;
    protected volatile ElasticsearchAsyncClient elasticsearchAsyncClient;
    protected volatile BulkIngester<Void> bulkIngester;


    public abstract ElasticsearchClient get();

    public abstract ElasticsearchAsyncClient getAsync();

    public abstract BulkIngester<Void> getBulkIngester();

    /**
     * Create an index with the given settings and mapping. If the index already exists, update the mapping.<br>
     * <br>
     * When index exists, only the mapping is updated. The settings are NOT updated (this requires to close the indices).
     * So you have to do it manually, i.e. close the indices, apply the settings changes, and reopen the indices.
     */
    public void createIndexOrUpdateMapping(String indexName, IndexSettings indexSettings, Class<?> modelClass) throws IOException {
        BooleanResponse exists = this.elasticsearchClient.indices().exists(req -> req.index(indexName));
        if (!exists.value()) {
            this.elasticsearchClient.indices().create(req -> req
                    .index(indexName)
                    .settings(indexSettings)
                    .withJson(EjacUtils.mappingAsInputStream(modelClass, true)));
            return;
        }
        this.elasticsearchClient.indices().putMapping(req -> req
                .index(indexName)
                .withJson(EjacUtils.mappingAsInputStream(modelClass, false)));
    }

}
