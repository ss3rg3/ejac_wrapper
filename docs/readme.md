{:toc}

# Import

- For package versions, see [here](https://github.com/ss3rg3/ejac_wrapper/packages/2185198)

- Just add the dependency to your `pom.xml`, e.g.

  ```xml
  <dependency>
    <groupId>io.github.ss3rg3</groupId>
    <artifactId>ejac_wrapper</artifactId>
    <version>0.0.1</version>
  </dependency>
  ```

- Make sure your `~/.m2/settings.xml` is properly set up and has the repository configured. Otherwise, Maven won't find it. E.g.

  ```xml
  <repository>
      <id>ss3rg3</id>
      <url>https://maven.pkg.github.com/ss3rg3/*</url>
      <snapshots>
          <enabled>true</enabled>
      </snapshots>
  </repository>
  ```

  



# Understanding ejac_wrapper

- This is just a wrapper which combines the [Elasticsearch Java API Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html) with [Spring Data Elasticsearch](https://spring.io/projects/spring-data-elasticsearch) to simplify mappings and provide some helper functions. 

- The wrapper comes with both bundled to simplify dependency management (might be a bad idea) and avoid compatibility issues, see [compatibility matrix](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/versions.html).

- There are just a few classes of interest:

  - **`EjacWrapper`**

    Abstract class which allows you to provide an `ElasticsearchClient`, `ElasticsearchAsyncClient` and `BulkIngester` with your implementation. It also provides utility methods like `createIndexOrUpdateMapping()` to automatically start with created indices and up-to-date mappings based on the Spring Data classes. See `DummyEjacWrapper` for an implementation example.

  - **`EjacUtils`** 

    Some utilities like `mappingAsInputStream()` to create mappings from classes with Spring Data annotations or `getLastSortValue()` to simplify `searchAfter` scolling. Also some utilities to create clients like `unsafeClientBuilder` to allow self-signed certs.

- Usage can be best deducted from the tests.



# Cookbook



## Working with document IDs

- Documents in Elasticsearch do have an `_id` field but it is separate from the `_source` field of the document. The `_source` field is where your model is deserialized from. 

- This means that without additional logic the `@Id String id;` field in your model will not be automatically populated.

- If you define an `@Id` field inside your model, then it will become part of the `_source` field, i.e. a duplication of data. You need additional logic to avoid this, e.g. via using `@JsonIgnore` on its getter and setter.

- **Best is just to leave it out in the model and handle it separately.** There's a wrapper to keep both together: `EjacModel`, e.g:

  ```java
  private static EjacModel<BookModel> getById(String id) throws IOException {
      GetResponse<BookModel> response = esc.get(g -> g
              .index(BookModel.INDEX_NAME)
              .id(id), BookModel.class
      );
      return new EjacModel<>(response.id(), response.source());
  }
  ```

  So you retrieve your document as usual and then use the response to create a `new EjacModel<>(response.id(), response.source())` instance to keep both together.



## Partial updates

- If you provide an ID to an `UpdateRequest` with a regular document model, the document will be replaced. I.e. null values will be serialized and existing values will be overwritten.

- Instead of an instance of the model of the index, you need to use a `Map<String, Object>` instead where the keys are the keys in the document's `_source` you want to overwrite. Elasticsearch will merge these two and only overwrite the provided keys.

- So if you have a model like this and want to overwrite the `author` field alone, while keeping the rest of the document as is:

  ```java
  public class BookModel {
      @Field(type = FieldType.Text)
      private String name;
      @Field(type = FieldType.Object)
      private Author author;
  ```

- Then you need to create a map with the field name as key and provide it in `.doc()` of the `UpdateRequest`

  ```java
  Map<String, Object> updateMap = new HashMap<>();
  updateMap.put("author", new Author("J.K. Rolling", 56));
  
  esc.update(req -> req
          .index(BookModel.INDEX_NAME)
          .id("123")
          .doc(updateMap), BookModel.class
  );
  ```

- If your model is already the document model which you use in the index and you want to only update fields which are not null, then you can simply map it to a `Map<String, Object>`:

  ```java
  // We map the object onto a Map for partial update (Include.NON_NULL only includes fields with non-null values)
  // I.e. all fields of object 'candidate' which are not null will be added to the map. The rest will be left out.
  ObjectMapper objectMapper = new ObjectMapper()
              .setSerializationInclusion(JsonInclude.Include.NON_NULL);
  Map<String, Object> partialUpdate = objectMapper.convertValue(candidate, new TypeReference<>() {});
  ```
  
  Note that if you have `Date` or other special fields then you need a serializer because Elasticsearch expects strings in ISO 8601 format (`yyyy-MM-dd'T'HH:mm:ss.SSSZ`). 
  
  ```java
  public static final ObjectMapper JSON = new ObjectMapper()
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .registerModule(new SimpleModule().addSerializer(Date.class, new IsoDateSerializer()));
  
  public class IsoDateSerializer extends JsonSerializer<Date> {
  
      private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  
      @Override
      public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
          String formattedDate = formatter.format(value);
          gen.writeString(formattedDate);
      }
  }
  ```
  
  



## Scrolling an index & pagination

- For details see [official docs](https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html#search-after). For a working implementation, see `SearchAfterTest`.

- For limited pagination (max 10k results) you can use `from` in the search query, e.g.

  ```json
  GET /_search
  {
    "from": 5,
    "size": 20,
    "query" { ... }
  }  
  ```

  This allows you to provide "page selection", e.g. page 5 is `from: 80` with `size: 20`.

- For more you need to use `searchAfter`. Scolling is not recommended anymore: "We no longer recommend using the scroll API for deep pagination. If you need to preserve the index state while paging through more than 10,000 hits, use the search_after parameter with a point in time (PIT)." ([src](https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html#scroll-search-results))

- `searchAfter` is much easier than scrolling. It's only as complicated as scrolling (where you had to use some kind of token) when you need to preserve the index state via a PIT (point in time)

- Without PIT, you just pass the `sort` value from the last document in the response in `.searchAfter()`, e.g:

  ```java
  // Make initial search to get the first page
  SearchResponse<RandomDataModel> response = esClient.search(search -> search
          .index(SEARCH_AFTER_INDEX)
          .size(750)
          .query(query -> query.matchAll(t -> t))
          .sort(s -> s
                  .field(f -> f
                          .field("count")
                          .order(SortOrder.Asc)
                  )
          ), RandomDataModel.class
  );
  
  // Make subsequent searches as long as there are hits
  while (EjacUtils.hasHits(response)) {
      // Use `lastSort` for `searchAfter()`
      List<FieldValue> lastSort = EjacUtils.getLastSortValue(response);
  
      response = esClient.search(search -> search
              .index(SEARCH_AFTER_INDEX)
              .size(750)
              .query(query -> query.matchAll(t -> t))
              .searchAfter(lastSort)
              .sort(s -> s
                      .field(f -> f
                              .field("count")
                              .order(SortOrder.Asc)
                      )
              ), RandomDataModel.class
      );
  }
  ```

  So it's just this:

  
  1. Get some result page
  2. Get the LAST `sort` value in `hits()` (via helper `EjacUtils.getLastSortValue(response)`)
  3. Use that value in the next search
  4. Repeat until no more hits via while loop, e.g. `while (EjacUtils.hasHits(response)) { ... }`

- The only drawback is that you probably can't calculate the amount of total pages in advance.



## Using BulkIngester

- See `BulkIngesterTest` for a working implementation.

- `BulkIngester` is pretty much the same as the old `BulkProcessor`. You can automatically flush after a certain threshold of documents or periodically. `BulkListener` is also pretty much the same.

  ```java
  BulkIngester<Void> ingester = BulkIngester.of(b -> b
              .client(esClient)
              .maxOperations(10)
              .listener(new CustomBulkListener()) // for handling errors or reporting
      );
  
  // Add documents to the bulk ingester. It will flush twice, because maxOperations=10.
  IntStream.range(0, 21).forEach(i -> {
      ingester.add(op -> op
              .index(req -> req
                      .index(RandomDataModel.INDEX_NAME)
                      .document(new RandomDataModel())
              ));
  });
  ingester.flush(); // Manual flush for document #21
  ```

- Update queries are a bit weird. You have to define an `action()` which can include a script for advanced use-cases.

  ```java
  // We map the object onto a Map for partial update (Include.NON_NULL only includes fields with non-null values)
  ObjectMapper objectMapper = new ObjectMapper()
              .setSerializationInclusion(JsonInclude.Include.NON_NULL);
  Map<String, Object> partialUpdate = objectMapper.convertValue(candidate, new TypeReference<>() {});
  
  this.bulkIngester.add(op -> op
          .update(idx -> idx
                  .index(DomainCandidate.indexName)
                  .id(candidate.getDomain())  // ID of the document
                  .action(doc -> doc
                          .doc(partialUpdate) 
                          .docAsUpsert(true)  // Create document if it doesn't exist
                  )
          )
  );
  ```

  




## Allow self-signed certs

- If you want to use Basic Auth, then you must enable `xpack.security` which does require you to also enable SSL. 

- When you start up an Elasticsearch cluster and no certs are provided, then Elasticsearch will create self-signed certs. But connecting with a client to the instance will fail because self-signed certs are rejected by default.

- In case you cannot be bothered with handling SSL properly, there's `UnsafeX509ExtendedTrustManager` which disables all checks.

  ```java
  public static ElasticsearchClient create() {
      SSLContext sslContext;
      try {
          sslContext = SSLContext.getInstance("TLS");
          sslContext.init(null, new TrustManager[]{UnsafeX509ExtendedTrustManager.INSTANCE}, null);
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
          throw new RuntimeException(e);
      }
  
      RestClient restClient = RestClient
              .builder(HttpHost.create("https://localhost:9200"))
              .setDefaultHeaders(new Header[]{
                      new BasicHeader("Authorization", asBase64("elastic", "elastic"))
              })
              .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder
                      .setSSLContext(sslContext)
                      .setSSLHostnameVerifier((hostNameVerifier, sslSession) -> true))
              .build();
      ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
      return new ElasticsearchClient(transport);
  }
  ```

  1. So you first create a `SSLContext` with this `UnsafeX509ExtendedTrustManager` and pass it to `setHttpClientConfigCallback()` when creating the client instance.
  2. Then you also need a supplier for `setSSLHostnameVerifier()` which never fails.



## Using Basic Auth

- You can pass headers to the client

  ```java
  RestClient restClient = RestClient
          .builder(HttpHost.create("https://localhost:9200"))
          .setDefaultHeaders(new Header[]{
                  new BasicHeader("Authorization", asBase64("elastic", "elastic"))
          })
          .build();
  ```

- The value must be Base64 encoded, e.g:

  ```java
  private static String asBase64(String username, String password) {
      String auth = username + ":" + password;
      byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
      return "Basic " + new String(encodedAuth);
  }
  ```

  

## Documents with special fields like `Date`

- The new client seems now to be able to handle `Date` out of the box. just make sure to use the appropriate field type

  ```java
  @Field(type = FieldType.Date)
  private Date dateField;
  ```

- Other fields like `Pattern` might need some custom serializer & deserializer implementation.



## Count documents in an index

- Using `.size(0)` only counts till 10000 documents. Using an aggregation should work but it's bothersome.

- Use `CountRequest`, e.g:

  ```java
  CountResponse countResponse = esClient.count(c -> c
          .index("some_index")
          .query(q -> q.matchAll(t -> t))
  );
  logger.info("Total documents in index: " + countResponse.count());
  ```



## IndexSettings with a custom analyzer

- For a working implementation see `IndexSettingsTest`.

- Everything is now handled via functional builders, e.g:

  ```java
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
  ```

  This will create the `html_field_analyzer` which uses `replace_special_chars` as additional filter.

- You also need to add the analyzer to the `@Field` which is supposed to use it, e.g:

  ```java
  @Field(type = FieldType.Text, analyzer = "html_field_analyzer")
  private String stringField;
  ```

  





# Development



## Testing

- Start the test cluster

  ```
  make up
  ```

- Run the test via IDE or Maven



## Starting the test cluster

- There's a `docker-compose.yml` in `assets/`

- Use it via the Makefile, e.g.

  ```
  make up
  ```

- Tests depend on a running cluster



## Manual queries to the REST API

- There's an insomnia export in `assets/` with some minimal requests
- E.g. for checking index settings or mappings



## Deploy package

- Packages are hosted on GitHub Packages

- Before deploying, make a clean commit where you only bump the version in the `pom.xml`, e.g:

  ```
  <version>0.0.2</version>
  ```

  Commit message should be clear, e.g. `release 0.0.2`

- Make sure your `~/.m2/settings.xml` is properly set up with the server ID `<id>ss3rg3</id>` (see `<distributionManagement>` in `pom.xml`) and an authorized `<username>`, e.g: 

  ```xml
  <profiles>
      <profile>
          <id>github</id>
          <repositories>
              // ...
              <repository>
                  <id>ss3rg3</id>
                  <url>https://maven.pkg.github.com/ss3rg3/*</url>
                  <snapshots>
                      <enabled>true</enabled>
                  </snapshots>
              </repository>
              // ...
          </repositories>
      </profile>
  </profiles>
  
  <servers>
      <server>
          <id>ss3rg3</id>
          <username>ss3rg3</username>
          <password>XXXXXXXXXXXXXXXXXXXX</password>
      </server>
  </servers>
  ```

- To deploy, just run this:

  ```
  mvn deploy
  ```

  

