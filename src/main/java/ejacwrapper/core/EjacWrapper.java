package ejacwrapper.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import ejacwrapper.utils.EjacUtils;

import java.io.IOException;

public class EjacWrapper {

    private final ElasticsearchClient esc;

    public EjacWrapper(ElasticsearchClient esc) {
        this.esc = esc;
    }

    public void createIndexOrUpdateMapping(String indexName, Class<?> modelClass) throws IOException {
        BooleanResponse exists = this.esc.indices().exists(req -> req.index(indexName));
        if (!exists.value()) {
            this.esc.indices().create(req -> req.index(indexName).withJson(EjacUtils.mappingAsInputStream(modelClass, true)));
            return;
        }
        this.esc.indices().putMapping(req -> req.index(indexName).withJson(EjacUtils.mappingAsInputStream(modelClass, false)));
    }

}
