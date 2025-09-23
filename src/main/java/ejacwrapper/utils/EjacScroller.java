package ejacwrapper.utils;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import ejacwrapper.core.EjacWrapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class EjacScroller {

    private final EjacWrapper ejacWrapper;
    private final Query query;
    private final String indexName;
    private final List<SortOptions> sortOptions;
    private final SourceConfig sourceConfig;
    private final Integer maxResults;
    private final Integer batchSize;
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private long tookTime = -1;

    public EjacScroller(Function<EjacScrollerBuilder, EjacScrollerBuilder> functionalBuilder) {
        EjacScrollerBuilder builder = functionalBuilder.apply(new EjacScrollerBuilder());

        this.ejacWrapper = Objects.requireNonNull(builder.ejacWrapper, "'ejacWrapper' must not be NULL.");
        this.query = Objects.requireNonNull(builder.query, "'query' must not be NULL.");
        this.indexName = Objects.requireNonNull(builder.indexName, "'indexName' must not be NULL.");
        this.sortOptions = Objects.requireNonNull(builder.sortOptions, "'sortOptions' must not be NULL.");
        this.sourceConfig = builder.sourceConfig;
        this.maxResults = Optional.ofNullable(builder.maxResults).orElse(Integer.MAX_VALUE);
        this.batchSize = Optional.ofNullable(builder.batchSize).orElse(1000);
    }

    public String report() {
        float took = this.tookTime / 1000.0f;
        return "Total Hits: " + this.totalCount.get() + "\n" +
                "Took: " + took + " seconds";
    }

    /**
     * Resets `totalCount` & `tookTime`
     */
    public void reset() {
        this.totalCount.set(0);
        this.tookTime = -1;
    }

    public <T> void run(Class<T> clazz, Consumer<SearchResponse<T>> consumer) {
        try {

            Instant startTime = Instant.now();
            Builder searchRequestBuilder = new SearchRequest.Builder()
                    .index(this.indexName)
                    .size(this.batchSize)
                    .query(this.query);
            if (this.sourceConfig != null) {
                searchRequestBuilder.source(this.sourceConfig);
            }
            if (this.sortOptions != null) {
                searchRequestBuilder.sort(this.sortOptions);
            }
            SearchResponse<T> response = this.ejacWrapper.get().search(searchRequestBuilder.build(), clazz);

            while (EjacUtils.hasHits(response)) {

                consumer.accept(response);

                this.totalCount.addAndGet(response.hits().hits().size());
                if (this.totalCount.get() >= this.maxResults) {
                    break;
                }

                List<FieldValue> lastSort = EjacUtils.getLastSortValue(response);
                searchRequestBuilder = new SearchRequest.Builder()
                        .index(this.indexName)
                        .size(this.batchSize)
                        .searchAfter(lastSort)
                        .query(this.query);
                if (this.sourceConfig != null) {
                    searchRequestBuilder.source(this.sourceConfig);
                }
                if (this.sortOptions != null) {
                    searchRequestBuilder.sort(this.sortOptions);
                }
                response = this.ejacWrapper.get().search(searchRequestBuilder.build(), clazz);
            }

            this.tookTime = startTime.until(Instant.now(), ChronoUnit.MILLIS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Total number of hits processed (limited by `maxResults`).
     */
    public AtomicInteger totalCount() {
        return this.totalCount;
    }

    /**
     * Time the scroll took in seconds.
     */
    public long tookTime() {
        return this.tookTime;
    }

    public static class EjacScrollerBuilder {
        public EjacScrollerBuilder() {
            // Public for external usage
        }

        private EjacWrapper ejacWrapper;
        private String indexName;
        private Query query;
        private List<SortOptions> sortOptions;
        private SourceConfig sourceConfig;
        private Integer maxResults;
        private Integer batchSize;

        /**
         * Your configured `EjacWrapper` instance.
         */
        public EjacScrollerBuilder ejacWrapper(EjacWrapper mandatory) {
            this.ejacWrapper = mandatory;
            return this;
        }

        /**
         * The name of the index to scroll. Used for `SearchRequest.Builder().index()`.
         */
        public EjacScrollerBuilder indexName(String mandatory) {
            this.indexName = mandatory;
            return this;
        }

        /**
         * Any kind of query from `QueryBuilders`. Used for `SearchRequest.Builder().query()`.
         */
        public EjacScrollerBuilder query(Query mandatory) {
            this.query = mandatory;
            return this;
        }

        /**
         * List of sort options. Used for `SearchRequest.Builder().sort()`. Needed for `searchAfter` or otherwise you
         * get an infinite loop. Looks like this:
         * <pre>
         * List.of(
         *    field(f -> f
         *       .field("count")
         *       .order(SortOrder.Asc)
         * )</pre>
         */
        public EjacScrollerBuilder sortOptions(List<SortOptions> mandatory) {
            this.sortOptions = mandatory;
            return this;
        }

        /**
         * The fields you want to include in `hit.source()`. Used for `SearchRequest.Builder().source()`. If your documents
         * are big, then this drastically increases performance. Looks like this:
         * <pre>
         * SourceConfig sourceConfig = SourceConfig.of(s -> s
         *     .filter(f -> f
         *         .includes("nodeBrowser.classification")
         *     )
         * );</pre>
         */
        public EjacScrollerBuilder sourceConfig(SourceConfig optional) {
            this.sourceConfig = optional;
            return this;
        }

        /**
         * The maximum number of hits to process. If reached, then the scroll loop will be broken. Note that the current
         * batch is always completely processed. So your `totalHits` might be off depending on your `batchSize`.
         */
        public EjacScrollerBuilder maxResults(Integer defaultIsMaxInteger) {
            this.maxResults = defaultIsMaxInteger;
            return this;
        }

        /**
         * The number of hits to process in each batch. Used for `SearchRequest.Builder().size()`
         */
        public EjacScrollerBuilder batchSize(Integer defaultIs1000) {
            this.batchSize = defaultIs1000;
            return this;
        }

    }

}
