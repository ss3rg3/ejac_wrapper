package ejacwrapper.crud;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import ejacwrapper._testutils.EjacClientFactory;
import ejacwrapper._testutils.TestUtils;
import ejacwrapper._testutils.models.SpecialFieldsModel;
import ejacwrapper.core.EjacWrapper;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SpecialFieldsTest {

    private static final EjacClientFactory ejacClientFactory = new EjacClientFactory();
    private static final ElasticsearchClient esc = ejacClientFactory.get();
    private static final EjacWrapper ejacWrapper = new EjacWrapper(esc);

    @Test
    void indexDocumentAndRetrieve() throws Exception {
        TestUtils.tryToDeleteIndex(SpecialFieldsModel.INDEX_NAME, esc);
        ejacWrapper.createIndexOrUpdateMapping(
                SpecialFieldsModel.INDEX_NAME,
                SpecialFieldsModel.INDEX_SETTINGS,
                SpecialFieldsModel.class);

        Date dateToIndex = new Date();
        SpecialFieldsModel model = new SpecialFieldsModel();
        model.setStringField("stringField");
        model.setDateField(dateToIndex);

        String id = "123";
        esc.index(req -> req
                .index(SpecialFieldsModel.INDEX_NAME)
                .id(id)
                .document(model));
        Thread.sleep(1000);

        GetResponse<SpecialFieldsModel> response = esc.get(g -> g
                .index(SpecialFieldsModel.INDEX_NAME)
                .id(id), SpecialFieldsModel.class);

        assertNotNull(response);
        assertNotNull(response.source());
        Date dateRetrieved = response.source().getDateField();
        assertEquals(dateToIndex.getTime(), dateRetrieved.getTime());

    }
}
