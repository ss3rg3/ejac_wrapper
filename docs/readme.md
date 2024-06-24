{:toc}

# :pencil2: Understanding ejac_wrapper

// todo

- [ ] Comes with the Java Client bundled, 

  - [ ] depends on [Spring Data Elasticsearch](https://spring.io/projects/spring-data-elasticsearch) for mapping

  - [ ] [compatibility matrix](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/versions.html)

- [ ] How to ignore certs (move class to main package)

- [ ] How to start Elasticsearch (before running tests)

  

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

  



# Development



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

  

