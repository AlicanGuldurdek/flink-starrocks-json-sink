package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrocks.connector.flink.StarRocksSink;
import com.starrocks.connector.flink.table.sink.StarRocksSinkOptions;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.ArrayList;
import java.util.List;

public class FlinkToStarRocksJob {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();


        StarRocksSinkOptions sinkOptions = StarRocksSinkOptions.builder()
                .withProperty("jdbc-url", "jdbc:mysql://192.168.1.103:9030")//endi ip adresni yaz
                .withProperty("load-url", "192.168.1.103:8030")//kendi ip adresini yaz
                .withProperty("username", "root")
                .withProperty("password", "")
                .withProperty("table-name", "user_data")
                .withProperty("database-name", "Star_db")
                .withProperty("sink.properties.format", "json")
                .build();


        List<UserData> userDataList = new ArrayList<>();
        userDataList.add(new UserData("Alican", "Güldürdek", "1234"));


        DataStream<UserData> userStream = env.fromCollection(userDataList);


        SingleOutputStreamOperator<String> jsonStream = userStream.map(new MapFunction<UserData, String>() {
            private static final long serialVersionUID = 1L;
            private transient ObjectMapper objectMapper;

            @Override
            public String map(UserData user) throws Exception {
                if (objectMapper == null) {
                    objectMapper = new ObjectMapper();
                }
                return objectMapper.writeValueAsString(user);
            }
        });


        jsonStream.addSink(StarRocksSink.sink(sinkOptions));

        env.execute("Flink JSON to StarRocks Job");
    }
}