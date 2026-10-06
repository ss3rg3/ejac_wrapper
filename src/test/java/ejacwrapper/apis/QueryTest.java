package ejacwrapper.apis;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.ScriptLanguage;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.bool;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.range;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Checks the Elasticsearch client version's query and script builders without a running cluster. */
class QueryTest {


    /**
     * We downgraded to `elasticsearch-java` 8.13.4 because it's the version in SpringData Elasticsearch 5.3.1.
     * Version 8.13.4 accepts a UTC timestamp string wrapped in JsonData. This must also render correctly
     * in diagnostic output, without depending on a host-local Date string or a configured mapper.
     * In newer versions a `date(d -> d)` builder accepts a timestamp string directly, e.g.
     * `.lt(Instant.now().toString())`
     */
    @Test
    void checkOldDateQueryApi() {
        // Execute the older generic range builder with an explicit absolute timestamp.
        Query query = bool(b -> b
                .should(range(r -> r
                                //.date(d -> d => needed when we upgrade `elasticsearch-java`
                                .field("someField")
                                .lt(JsonData.of(Instant.EPOCH.toString()))
                        // .lt(String.valueOf(Instant.now().toEpochMilli())))
                )));
        // Verify the complete query independently of the default timezone.
        assertEquals("""
                Query: {"bool":{"should":[{"range":{"someField":{"lt":"1970-01-01T00:00:00Z"}}}]}}
                """.trim(), query.toString());
    }

    /**
     * We downgraded to `elasticsearch-java` 8.13.4 because it's the version in SpringData Elasticsearch 5.3.1.
     * In the laters versions, the script API changed. In the versions the `.inline(i -> i)` builder was removed.
     */
    @Test
    void checkOldScriptApi() {
        Script script = Script.of(s -> s
                .inline(i -> i // remove when we upgrade `elasticsearch-java`
                        .lang(ScriptLanguage.Painless)
                        .source("ctx._source.remove('SOME_FIELD')")
                ));
        assertEquals("""
                Script: {"lang":"painless","source":"ctx._source.remove('SOME_FIELD')"}
                """.trim(), script.toString());
    }
}
