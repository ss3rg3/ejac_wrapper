package ejacwrapper.core;

/**
 * Wrapper for storing the Elasticsearch document _id with the _source object, because Response doesn't provide them
 * inside a single object.
 */
public class EjacModel<T> {

    private final String id;
    private final T source;

    public EjacModel(String id, T source) {
        this.id = id;
        this.source = source;
    }

    public String getId() {
        return this.id;
    }

    public T getSource() {
        return this.source;
    }
}
