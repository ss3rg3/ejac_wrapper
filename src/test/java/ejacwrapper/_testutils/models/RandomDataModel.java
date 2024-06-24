package ejacwrapper._testutils.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.UUID;

public class RandomDataModel {

    public static String INDEX_NAME = "random_data_index";

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String uuid = UUID.randomUUID().toString();

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
