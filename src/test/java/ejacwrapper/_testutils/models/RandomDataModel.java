package ejacwrapper._testutils.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class RandomDataModel {

    public static String INDEX_NAME = "random_data_index";
    private static final AtomicInteger counter = new AtomicInteger(0);

    @Id
    private String id;

    @Field(type = FieldType.Text, index = false)
    private String uuid = UUID.randomUUID().toString();

    @Field(type = FieldType.Integer, index = false)
    private Integer count = counter.incrementAndGet();

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

    public Integer getCount() {
        return this.count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
