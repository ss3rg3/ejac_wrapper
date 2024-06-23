package ejachelpers.indices;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import ejachelpers.mapping.EjacMappingUtil;

import java.io.IOException;

public class EjacIndices {

    private final ElasticsearchClient esc;

    public EjacIndices(ElasticsearchClient esc) {
        this.esc = esc;
    }

    public void createIndexOrUpdateMapping(String indexName, Class<?> modelClass) throws IOException {
        BooleanResponse exists = this.esc.indices().exists(req -> req.index(indexName));
        if (!exists.value()) {
            this.esc.indices().create(req -> req.index(indexName).withJson(EjacMappingUtil.asInputStream(modelClass, true)));
            return;
        }
        this.esc.indices().putMapping(req -> req.index(indexName).withJson(EjacMappingUtil.asInputStream(modelClass, false)));
    }

}
