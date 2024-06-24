package ejacwrapper.crud;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import ejacwrapper._testutils.EjacClientFactory;
import ejacwrapper._testutils.TestUtils;
import ejacwrapper._testutils.models.RandomDataModel;
import ejacwrapper.core.EjacWrapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BulkIngesterTest {

    private static final ElasticsearchClient esClient = EjacClientFactory.create();
    private static final EjacWrapper ejacWrapper = new EjacWrapper(esClient);
    private static final Logger logger = LoggerFactory.getLogger(BulkIngesterTest.class);
    private static final BulkIngester<Void> ingester = BulkIngester.of(b -> b
            .client(esClient)
            .maxOperations(10)
            .listener(new CustomBulkListener())
    );
    private static final AtomicInteger documentCounter = new AtomicInteger(0);
    private static final AtomicInteger requestCounter = new AtomicInteger(0);

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

    private static class CustomBulkListener implements BulkListener<Void> {
        @Override
        public void beforeBulk(long executionId, BulkRequest request, List<Void> contexts) {
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, List<Void> contexts, BulkResponse response) {
            // The request was accepted, but may contain failed items.
            // The "context" list gives the file name for each bulk item.
            logger.info("Bulk request " + executionId + " completed. Took " + response.took() + "ms, with " + response.items().size() + " items.");
            requestCounter.incrementAndGet();
            documentCounter.addAndGet(response.items().size());
            for (int i = 0; i < contexts.size(); i++) {
                BulkResponseItem item = response.items().get(i);
                if (item.error() != null) {
                    // Inspect the failure cause
                    logger.error("Failed to index file " + contexts.get(i) + " - " + item.error().reason());
                }
            }
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, List<Void> contexts, Throwable failure) {
            // The request could not be sent
            logger.error("Bulk request " + executionId + " failed", failure);
        }
    }

}
