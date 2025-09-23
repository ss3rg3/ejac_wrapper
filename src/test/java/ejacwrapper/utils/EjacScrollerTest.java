package ejacwrapper.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import ejacwrapper._testutils.CustomBulkListener;
import ejacwrapper._testutils.DummyEjacWrapper;
import ejacwrapper._testutils.TestUtils;
import ejacwrapper._testutils.models.RandomDataModel;
import ejacwrapper.core.EjacWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static co.elastic.clients.elasticsearch._types.SortOptionsBuilders.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EjacScrollerTest {

    private static final EjacWrapper ejacWrapper = new DummyEjacWrapper();
    private static final ElasticsearchClient esc = ejacWrapper.get();
    private static final AtomicInteger documentCounter = new AtomicInteger(0);
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    private static final BulkIngester<Void> ingester = BulkIngester.of(b -> b
            .client(esc)
            .maxOperations(10)
            .listener(new CustomBulkListener(documentCounter, requestCounter))
    );

    @Test
    void scroll() throws Exception {
        TestUtils.tryToDeleteIndex(RandomDataModel.INDEX_NAME, esc);
        ejacWrapper.createIndexOrUpdateMapping(RandomDataModel.INDEX_NAME, TestUtils.indexSettingsDummy, RandomDataModel.class);

        // Add documents to the bulk ingester
        IntStream.range(0, 120).forEach(i -> {
            ingester.add(op -> op
                    .index(req -> req
                            .index(RandomDataModel.INDEX_NAME)
                            .document(new RandomDataModel())
                    ));
        });
        ingester.flush(); // Flush document #21
        Thread.sleep(1000); // Because it's async

        EjacScroller ejacScroller = new EjacScroller(_0 -> _0
                .ejacWrapper(ejacWrapper)
                .indexName(RandomDataModel.INDEX_NAME)
                .query(QueryBuilders.matchAll(m -> m))
                .batchSize(20)
                .maxResults(100)
                .sortOptions(List.of(
                        field(f -> f
                                .field("count")
                                .order(SortOrder.Asc))
                ))
        );

        AtomicInteger hitCount = new AtomicInteger(0);
        ejacScroller.run(RandomDataModel.class, response -> {
            System.out.println("Next batch:");
            response.hits().hits().forEach(hit -> {
                if (hit.source() == null) {
                    throw new IllegalStateException("`hit.source()` is null, that shouldn't happen");
                }
                hitCount.incrementAndGet();
                System.out.println(hit.source().getCount() + ". " + hit.id());
            });
        });

        assertEquals(100, hitCount.get()); // 120 documents but maxResults cancels after 5 batches
        assertTrue(ejacScroller.report().contains("Total Hits: 100"));
    }


}
