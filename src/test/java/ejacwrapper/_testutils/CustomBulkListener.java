package ejacwrapper._testutils;

import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public record CustomBulkListener(AtomicInteger documentCounter,
                                 AtomicInteger requestCounter) implements BulkListener<Void> {

    private static final Logger logger = LoggerFactory.getLogger(CustomBulkListener.class);

    @Override
    public void beforeBulk(long executionId, BulkRequest request, List<Void> contexts) {
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, List<Void> contexts, BulkResponse response) {
        // The request was accepted, but may contain failed items.
        // The "context" list gives the file name for each bulk item.
        logger.info("Bulk request " + executionId + " completed. Took " + response.took() + "ms, with " + response.items().size() + " items.");
        this.requestCounter.incrementAndGet();
        this.documentCounter.addAndGet(response.items().size());
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
