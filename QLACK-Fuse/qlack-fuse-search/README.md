# Elasticsearch integration using Qlack

## Docker Elasticsearch 

In a cmd enter the following :

`docker run --name=A_NAME_OF_YOUR_FLAVOR -p 9400:9200  -p 9401:9300 -e "http.host=0.0.0.0" -e "transport.host=0.0.0.0"  -e "xpack.security.enabled=false" -d docker.elastic.co/elasticsearch/elasticsearch:6.4.2`



## Spring boot Elasticsearch integration


### Elasticsearch configuration

Add at application.properties :
```properties
################################################################################
# Elasticsearch configuration
################################################################################
# Qlack uses 2 different Elasticsearch clients:

# RestHighLevelClient ES client 
qlack.fuse.search.es_hosts=http:localhost:9401

# Repo ES client (org.elasticsearch.client.Client)
qlack.fuse.search.host.name=localhost
qlack.fuse.search.host.port=9401
qlack.fuse.search.cluster.name=docker-cluster


# Spring boot 2.1.0 migration see link below.
spring.main.allow-bean-definition-overriding=true


```
> `docker-cluster` is the docker default cluster.name, it will be different in any other Elasticsearch environment.

[Spring boot 2.1.0 migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.0-Migration-Guide)

### Enable Elasticsearch Spring boot repositories


At Add qlack-fuse-search at your pom.xml:
```xml

    <properties>
<!-- ... -->
    <qlack.version>3.0.0-SNAPSHOT</qlack.version>
  </properties>

<!-- ... -->

    <dependency>
	  <groupId>com.eurodyn.qlack.fuse</groupId>
	  <artifactId>qlack-fuse-search</artifactId>
	  <version>${qlack.version}</version>
	</dependency>

```


At App.java:

```java
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
// ..


@SpringBootApplication
@EnableAsync
@EnableJpaRepositories({
    "com.eurodyn.qlack.fuse.search",
    // ..

})
@EnableElasticsearchRepositories({
    "com.eurodyn.qlack.fuse.search",
// + The location of ElasticsearchRepositories:
    "domain.appName.repository.es" 
})
@EnableCaching
@ComponentScan({
  "com.eurodyn.qlack.fuse.search",
    //..
})

```


ElasticsearchRepository declaration example:

```java

package domain.appName.repository.es;

//...

public interface ApplicationAnimalsRepository extends ElasticsearchRepository<Animal, String> {

    // custom query declaration for the field 
    // int Age  of Animal class
    //no further implemetation nedeed
    List<Animal>  findByAge(int age);
    
```


ElasticsearchRepository call example:


```java
    // index creation for the class Animals:
    List<Animal> animals = getAllAnimals();
    
    applicationAnimalsRepository.saveAll(animals);
    //...

    // simple search with like:
    applicationAnimalsRepository.findByNameLike(text);

```

ES Document example:
```java

import static org.springframework.data.elasticsearch.annotations.FieldType.Text;
//..

@Document(indexName = "animal")
public class Animal  {

 
  @Field(type = Text, index = true)
  private String id;

 // Searchable with the french analyzer and Retrievable
  @Field(type = Text, index = true, searchAnalyzer="french", analyzer = "french")
  private String name;

  // Retrievable but not searchable
  @Field(type = Text, index = false)
  private String type;

 // Searchable and Retrievable 
  @Field(type = Text, index = true)
  private int age;

//...
}
```
