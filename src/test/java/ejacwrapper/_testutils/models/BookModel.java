package ejacwrapper._testutils.models;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class BookModel {

    public static final String INDEX_NAME = "book_model_index";

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Object)
    private MetaData metaData;

    @Field(type = FieldType.Object)
    private Author author;

    public BookModel(String name, MetaData metaData, Author author) {
        this.name = name;
        this.metaData = metaData;
        this.author = author;
    }

    /**
     * Default constructor for Jackson
     */
    public BookModel() {
    }

    public static class MetaData {
        @Field(type = FieldType.Keyword)
        private String releaseYear;
        @Field(type = FieldType.Keyword)
        private String isbn;

        public MetaData(String releaseYear, String isbn) {
            this.releaseYear = releaseYear;
            this.isbn = isbn;
        }

        public MetaData() {
        }

        public String getReleaseYear() {
            return this.releaseYear;
        }

        public void setReleaseYear(String releaseYear) {
            this.releaseYear = releaseYear;
        }

        public String getIsbn() {
            return this.isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }
    }

    public static class Author {
        @Field(type = FieldType.Text)
        private String name;
        @Field(type = FieldType.Integer)
        private Integer age;

        public Author(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public Author() {
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return this.age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    @Override
    public String toString() {
        return "BookModel{" +
                ", name='" + this.name + '\'' +
                ", metaData=" + this.metaData +
                ", author=" + this.author +
                '}';
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MetaData getMetaData() {
        return this.metaData;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public Author getAuthor() {
        return this.author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }
}
