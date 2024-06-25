package ejacwrapper._testutils.models;

import co.elastic.clients.elasticsearch.indices.IndexSettings;
import ejacwrapper._testutils.TestUtils;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

public class SpecialFieldsModel {


    public static final String INDEX_NAME = "special_fields_index";
    public static final IndexSettings INDEX_SETTINGS = TestUtils.indexSettingsDummy;

    @Field(type = FieldType.Text)
    private String stringField;

    @Field(type = FieldType.Date)
    private Date dateField;

    public String getStringField() {
        return this.stringField;
    }

    public void setStringField(String stringField) {
        this.stringField = stringField;
    }

    public Date getDateField() {
        return this.dateField;
    }

    public void setDateField(Date dateField) {
        this.dateField = dateField;
    }
}
