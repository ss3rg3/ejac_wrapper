package ejacwrapper.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import ejacwrapper.utils.EjacUtils;

import java.io.IOException;

public class EjacWrapper {

    private final ElasticsearchClient esc;

    public EjacWrapper(ElasticsearchClient esc) {
        this.esc = esc;
    }

    /**
     * Create an index with the given settings and mapping. If the index already exists, update the mapping.<br>
     * <br>
     * When index exists, only the mapping is updated. The settings are NOT updated (this requires to close the indices).
     * So you have to do it manually, i.e. close the indices, apply the settings changes, and reopen the indices.
     */
    public void createIndexOrUpdateMapping(String indexName, IndexSettings indexSettings, Class<?> modelClass) throws IOException {
        BooleanResponse exists = this.esc.indices().exists(req -> req.index(indexName));
        if (!exists.value()) {
            this.esc.indices().create(req -> req
                    .index(indexName)
                    .settings(indexSettings)
                    .withJson(EjacUtils.mappingAsInputStream(modelClass, true)));
            return;
        }
        this.esc.indices().putMapping(req -> req
                .index(indexName)
                .withJson(EjacUtils.mappingAsInputStream(modelClass, false)));
    }

}
