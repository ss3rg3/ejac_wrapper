package ejacwrapper.crud;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import ejacwrapper._testutils.EjacClientFactory;
import ejacwrapper._testutils.TestUtils;
import ejacwrapper._testutils.models.BookModel;
import ejacwrapper._testutils.models.BookModel.Author;
import ejacwrapper._testutils.models.BookModel.MetaData;
import ejacwrapper.core.EjacModel;
import ejacwrapper.core.EjacWrapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PartialUpdateTest {

    private static final ElasticsearchClient esc = EjacClientFactory.create();
    private static final EjacWrapper ejacWrapper = new EjacWrapper(esc);

    @Test
    void indexAndThenUpdate() throws Exception {
        TestUtils.tryToDeleteIndex(BookModel.INDEX_NAME, esc);
        ejacWrapper.createIndexOrUpdateMapping(BookModel.INDEX_NAME, TestUtils.indexSettingsDummy, BookModel.class);

        String id = "123";
        BookModel bookModel;
        EjacModel<BookModel> ejacModel;

        // Index a new document
        bookModel = new BookModel(
                "Harry Potter",
                new MetaData("2000", "1234567890"),
                new Author("J.K. Rowling", 55)
        );
        indexDocument(id, bookModel);
        ejacModel = getById(id);
        assertEquals("Harry Potter", ejacModel.getSource().getName());
        assertEquals("J.K. Rowling", ejacModel.getSource().getAuthor().getName());
        assertEquals(55, ejacModel.getSource().getAuthor().getAge());

        // Update the document
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("author", new Author("J.K. Rolling", 56));
        updateDocument(id, updateMap);
        ejacModel = getById(id);
        assertEquals("Harry Potter", ejacModel.getSource().getName());
        assertNotNull(ejacModel.getSource().getMetaData());
        assertEquals("J.K. Rolling", ejacModel.getSource().getAuthor().getName());
        assertEquals(56, ejacModel.getSource().getAuthor().getAge());
    }

    private static void indexDocument(String id, BookModel bookModel) throws Exception {
        esc.index(req -> req
                .index(BookModel.INDEX_NAME)
                .id(id)
                .document(bookModel)
        );
        Thread.sleep(1000);
    }

    private static void updateDocument(String id, Map<String, Object> updateMap) throws Exception {
        esc.update(req -> req
                .index(BookModel.INDEX_NAME)
                .id(id)
                .doc(updateMap), BookModel.class
        );
        Thread.sleep(1000);
    }

    private static EjacModel<BookModel> getById(String id) throws IOException {
        GetResponse<BookModel> response = esc.get(g -> g
                .index(BookModel.INDEX_NAME)
                .id(id), BookModel.class
        );
        return new EjacModel<>(response.id(), response.source());
    }


}
