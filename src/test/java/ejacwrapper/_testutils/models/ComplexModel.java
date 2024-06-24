package ejacwrapper._testutils.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.TermVector;

import java.util.List;
import java.util.Map;

public class ComplexModel {

    public static final String INDEX_NAME = "complex_model_index";

    @Id
    public String id;

    @Field(type = FieldType.Nested, store = false)
    public List<NestedModel> nestedField;

    @Field(type = FieldType.Object, termVector = TermVector.with_positions_offsets_payloads, index = false)
    public List<ObjectModel> objectField;

    @Field(type = FieldType.Object, store = true, excludeFromSource = true)
    public Map<String, MapModel> mapField;

//    @Field(type = FieldType.Text, storeNullValue = true)
//    private List<String> arrayField;

    public static class NestedModel {
        @Field(type = FieldType.Text, termVector = TermVector.with_positions_offsets_payloads, index = false)
        public String nestedField;
    }

    public static class ObjectModel {
        @Field(type = FieldType.Text, index = false)
        public String name;

        @Field(type = FieldType.Integer, excludeFromSource = true)
        public Integer age;
    }

    public static class MapModel {
        @Field(type = FieldType.Text, store = true)
        public String name;

        @Field(type = FieldType.Text, searchAnalyzer = "standard")
        public Integer description;
    }

}
