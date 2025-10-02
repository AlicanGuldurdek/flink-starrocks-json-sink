# StarRocks ve Apache Flink Entegrasyonu

---

## 1. StarRocks Kurulumu
- Tek node üzerinde StarRocks FE ve BE servislerini başlatın.
- Varsayılan portlar:
  - **8030** → HTTP Query
  - **9030** → MySQL protokolü
  - **8040** → Backend Web UI  
  

StarRocks'u Başlat:
```
docker run -p 9030:9030 -p 8030:8030 -p 8040:8040 -itd \
--name quickstart starrocks/allin1-ubuntu
```
Mysql CLI'yi kullanmanın en kolay yolu, onu StarRocks konteyneri starrocks-fe çalıştırmaktır:

```
docker exec -it quickstart \
mysql -P 9030 -h 127.0.0.1 -u root --prompt="StarRocks > "
```
Bağlantı testi:

```
mysql -h 127.0.0.1 -P9030 -uroot

```
---
## 2. Apache Flink Kurulumu
Apache flink'in uygun versiyonunu indirrebilrisin:
https://dlcdn.apache.org/flink/flink-1.19.3/flink-1.19.3-bin-scala_2.12.tgz

 -Flink 2.1.0 versiyonunu indirin.

 -Talos cluster üzerinde tek node olarak deploy edin.

 -Arayüze erişim:

---
## 3. StarRocks ile Database ve Tablo Oluşturma
StarRocks MySQL client üzerinden:
```
CREATE DATABASE <DataBase_name>;

USE <DataBase_name>;

CREATE TABLE <Table_name> (
    id INT,
    name STRING,
    surname STRING,
    card STRING
) DUPLICATE KEY(id)
DISTRIBUTED BY HASH(id) BUCKETS 1
PROPERTIES ("replication_num" = "1");
```
---
## 4. Java Maven Projesi Oluşturma

Yeni Java Maven projesi oluşturun:
```
mvn archetype:generate -DgroupId=org.example \
    -DartifactId=<Proje_name> \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false

```
---
## 5.Veri Modeli Oluşturma(UserData.java)
Burdaki modeliniz DataBase'teki tablonuzdaki sütünlar ile aynı olması gerek:
```

public class UserData implements Serializable {
    public String name;
    public String surname;
    public String card;

    public UserData() {}
        //this methodu kullanarak oluşturduğmuz objeye erişireke birden çok kullanma imkanı
    public UserData(String name, String surname, String card) {
        this.name = name;
        this.surname = surname;
        this.card = card;
```
---

## 6. pom.xml Tanımları 
```

    <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <flink.version>1.17.1</flink.version>
        <starrocks-flink-connector.version>1.2.9_flink-1.17</starrocks-flink-connector.version>
        <jackson.version>2.15.2</jackson.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-streaming-java</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-clients</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-table-api-java-bridge</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>com.starrocks</groupId>
            <artifactId>flink-connector-starrocks</artifactId>
            <version>${starrocks-flink-connector.version}</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
    </dependencies>

```
---
## 7.Main.java
### 1.Sink 
```
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

```
### 2. Manuel Veri Oluşturma
```
List<UserData> users = Arrays.asList(
    new UserData(1, "Alican", "Güldürdek", "1111"),

```
### 3. Listeyi Flink Veri Akışına Dönüştür
```
        DataStream<UserData> userStream = env.fromCollection(userDataList);
```
### 4. SerData Nesnelerini JSON Formatında Dönüştür

```
DataStream<String> jsonStream = userStream.map(user -> {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(user);
});

```

### 5. JSON String Akışını StarRocks’a Yazma
```
     StarRocksSinkOptions sinkOptions = StarRocksSinkOptions.builder()
                .withProperty("jdbc-url", "jdbc:mysql://192.168.A.AAA:9030"//A buralara kendi ip adresiniizi yaazın
                .withProperty("load-url", "192.168.A.AAA:8030")
                .withProperty("username", "root")
                .withProperty("password", "")
                .withProperty("table-name", "user_data")
                .withProperty("database-name", "Star_db")
                .withProperty("sink.properties.format", "json")
                .build();

```
### 6. Flink Başalt

```
env.execute("Flink to StarRocks Demo");

```
---
## 8. Çalıştırmak İçin
```
mvn clean package
```
Oluşan Jar dosyası Flink dosyası aynı dizinde olması gerek

```
./bin/flink run -c org.example.FlinkToStarRocksJob ./flink-starrocks-json-sink-1.0-SNAPSHOT.jar
```
