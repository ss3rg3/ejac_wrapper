package ejachelpers._testutils;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.TermVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComplexModel {

    @Id
    public String id;

    @Field(type = FieldType.Nested, termVector = TermVector.with_positions_offsets_payloads)
    public List<Object> nestedField = new ArrayList<>();

    @Field(type = FieldType.Object, store = false, excludeFromSource = true)
    public Map<String, Integer> objectField;

    @Field(type = FieldType.Text, analyzer = "html_field_analyzer", searchAnalyzer = "html_field_analyzer")
    private List<String> arrayField;

}
