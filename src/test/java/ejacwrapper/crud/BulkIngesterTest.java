package ejacwrapper.crud;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import ejacwrapper._testutils.CustomBulkListener;
import ejacwrapper._testutils.EjacClientFactory;
import ejacwrapper._testutils.TestUtils;
import ejacwrapper._testutils.models.RandomDataModel;
import ejacwrapper.core.EjacWrapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BulkIngesterTest {

    private static final ElasticsearchClient esClient = EjacClientFactory.create();
    private static final EjacWrapper ejacWrapper = new EjacWrapper(esClient);
    private static final AtomicInteger documentCounter = new AtomicInteger(0);
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    private static final BulkIngester<Void> ingester = BulkIngester.of(b -> b
            .client(esClient)
            .maxOperations(10)
            .listener(new CustomBulkListener(documentCounter, requestCounter))
    );

    @Test
    void indexMultipleDocuments() throws Exception {
        TestUtils.tryToDeleteIndex(RandomDataModel.INDEX_NAME, esClient);
        ejacWrapper.createIndexOrUpdateMapping(RandomDataModel.INDEX_NAME, RandomDataModel.class);

        // Add documents to the bulk ingester
        IntStream.range(0, 21).forEach(i -> {
            ingester.add(op -> op
                    .index(req -> req
                            .index(RandomDataModel.INDEX_NAME)
                            .document(new RandomDataModel())
                    ));
        });
        ingester.flush(); // Flush document #21
        Thread.sleep(1000); // Because it's async

        SearchResponse<RandomDataModel> response = esClient.search(s -> s
                        .index(RandomDataModel.INDEX_NAME)
                        .size(0)
                        .query(q -> q.matchAll(t -> t)),
                RandomDataModel.class
        );

        // Counters from the CustomBulkListener
        assertEquals(3, requestCounter.get());
        assertEquals(21, documentCounter.get()); // 10 + 10 + 1
        // Documents in database
        assertNotNull(response.hits().total());
        assertEquals(21, response.hits().total().value());

    }


}
