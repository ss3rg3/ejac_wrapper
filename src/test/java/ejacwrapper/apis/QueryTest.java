package ejacwrapper.apis;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.ScriptLanguage;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.bool;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.range;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryTest {


    /**
     * We downgraded to `elasticsearch-java` 8.13.4 because it's the version in SpringData Elasticsearch 5.3.1.
     * In the laters versions, the date range query API changed. In 8.13.4 you wrap `Date` in JsonData (e.g. `.lt(JsonData.of(new Date()))`),
     * and in the newer version you have a `date(d -> d)` builder allows you to just stringify an Instant, e.g.
     * `.lt(Instant.now().toString())`
     */
    @Test
    void checkOldDateQueryApi() {
        Query query = bool(b -> b
                .should(range(r -> r
                                //.date(d -> d => needed when we upgrade `elasticsearch-java`
                                .field("someField")
                                .lt(JsonData.of(new Date(0)))
                        // .lt(String.valueOf(Instant.now().toEpochMilli())))
                )));
        assertEquals("""
                Query: {"bool":{"should":[{"range":{"someField":{"lt":"Thu Jan 01 01:00:00 CET 1970"}}}]}}
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
