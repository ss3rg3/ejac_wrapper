package ejacwrapper.crud;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import ejacwrapper._testutils.CustomBulkListener;
import ejacwrapper._testutils.EjacClientFactory;
import ejacwrapper._testutils.TestUtils;
import ejacwrapper._testutils.models.RandomDataModel;
import ejacwrapper.core.EjacWrapper;
import ejacwrapper.utils.EjacUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchAfterTest {

    private static final ElasticsearchClient esClient = EjacClientFactory.create();
    private static final EjacWrapper ejacWrapper = new EjacWrapper(esClient);
    private static final AtomicInteger documentCounter = new AtomicInteger(0);
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    private static final BulkIngester<Void> ingester = BulkIngester.of(b -> b
            .client(esClient)
            .maxOperations(1000)
            .listener(new CustomBulkListener(documentCounter, requestCounter))
    );
    private static final String SEARCH_AFTER_INDEX = "search_after_index";
    private static final Logger logger = LoggerFactory.getLogger(SearchAfterTest.class);

    @BeforeAll
    static void indexDocuments() throws Exception {
        TestUtils.tryToDeleteIndex(SEARCH_AFTER_INDEX, esClient);
        ejacWrapper.createIndexOrUpdateMapping(SEARCH_AFTER_INDEX, TestUtils.indexSettingsDummy, RandomDataModel.class);
        IntStream.range(0, 12_500).forEach(i -> {
            ingester.add(op -> op
                    .index(req -> req
                            .index(SEARCH_AFTER_INDEX)
                            .document(new RandomDataModel())
                    ));
        });
        ingester.flush(); // Flush document #21
        Thread.sleep(1000); // Because it's async
    }

    /**
     * See <a href='https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html#search-after'>here</a> on how to use `searchAfter()`.
     * 1. Get some result page<br>
     * 2. Get the LAST `sort` value in `hits()`<br>
     * 3. Use that value in the next search<br>
     * 4. Repeat until no more hits<br>
     */
    @Test
    void runSearchAfter() throws Exception {
        CountResponse countResponse = esClient.count(c -> c
                .index(SEARCH_AFTER_INDEX)
                .query(q -> q.matchAll(t -> t))
        );
        logger.info("Total documents in index: " + countResponse.count());

        // Make initial search to get the first page
        AtomicInteger documentCounter = new AtomicInteger(0);
        SearchResponse<RandomDataModel> response = esClient.search(search -> search
                .index(SEARCH_AFTER_INDEX)
                .size(750)
                .query(query -> query.matchAll(t -> t))
                .sort(s -> s
                        .field(f -> f
                                .field("count")
                                .order(SortOrder.Asc)
                        )
                ), RandomDataModel.class
        );
        documentCounter.addAndGet(response.hits().hits().size());

        // Make subsequent searches as long as there are hits
        while (EjacUtils.hasHits(response)) {
            // Use `lastSort` for `searchAfter()`
            List<FieldValue> lastSort = EjacUtils.getLastSortValue(response);
            if (lastSort != null) {
                logger.info("`lastSort` value: " + lastSort.get(0).longValue());
            } else {
                logger.info("`lastSort` is null");
            }

            response = esClient.search(search -> search
                    .index(SEARCH_AFTER_INDEX)
                    .size(750)
                    .query(query -> query.matchAll(t -> t))
                    .searchAfter(lastSort)
                    .sort(s -> s
                            .field(f -> f
                                    .field("count")
                                    .order(SortOrder.Asc)
                            )
                    ), RandomDataModel.class
            );
            documentCounter.addAndGet(response.hits().hits().size());
        }
        logger.info("Total documents in retrieved via scroll: " + documentCounter.get());
        assertEquals(countResponse.count(), documentCounter.get());
    }
}
