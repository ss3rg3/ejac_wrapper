package ejacwrapper.crud;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.IndexState;
import ejacwrapper._testutils.EjacClientFactory;
import ejacwrapper._testutils.TestUtils;
import ejacwrapper._testutils.models.SimpleModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IndexSettingsTest {

    private static final EjacClientFactory ejacClientFactory = new EjacClientFactory();
    private static final ElasticsearchClient esc = ejacClientFactory.get();

    @Test
    void createIndexWithCustomAnalyzer() throws Exception {
        TestUtils.tryToDeleteIndex(SimpleModel.INDEX_NAME, esc);

        IndexSettings indexSettings = IndexSettings.of(is -> is
                .numberOfReplicas("5")
                .numberOfShards("5")
                .analysis(an -> an
                        .analyzer("html_field_analyzer", a -> a
                                .custom(b -> b
                                        .tokenizer("standard")
                                        .filter("lowercase")
                                        .charFilter("replace_special_chars")))
                        .charFilter("replace_special_chars", c -> c
                                .definition(d -> d.patternReplace(p -> p
                                        .pattern("[^\\p{L}\\d\\s\\.]|(?<=\\D)\\.|\\.(?=\\D)")
                                        .replacement(" "))))));

        esc.indices().create(req -> req
                .index(SimpleModel.INDEX_NAME)
                .settings(indexSettings)
        );

        GetIndicesSettingsResponse gisr = esc.indices().getSettings(s -> s
                .index(SimpleModel.INDEX_NAME)
        );
        IndexState indexState = gisr.get(SimpleModel.INDEX_NAME);
        assertNotNull(indexState);
        assertNotNull(indexState.settings());
        assertNotNull(indexState.settings().index());
        assertNotNull(indexState.settings().index().analysis());
        assertEquals("5", indexState.settings().index().numberOfShards());
        assertEquals("5", indexState.settings().index().numberOfReplicas());
        assertEquals("Analyzer: {\"type\":\"custom\",\"char_filter\":[\"replace_special_chars\"],\"filter\":[\"lowercase\"],\"tokenizer\":\"standard\"}", indexState.settings().index().analysis().analyzer().get("html_field_analyzer").toString());
        assertEquals("CharFilter: {\"type\":\"pattern_replace\",\"pattern\":\"[^\\\\p{L}\\\\d\\\\s\\\\.]|(?<=\\\\D)\\\\.|\\\\.(?=\\\\D)\",\"replacement\":\" \"}", indexState.settings().index().analysis().charFilter().get("replace_special_chars").toString());
        System.out.println();
    }
}
