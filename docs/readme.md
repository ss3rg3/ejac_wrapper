{:toc}

# :pencil2: Understanding ejac_helpers

// todo



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

  

