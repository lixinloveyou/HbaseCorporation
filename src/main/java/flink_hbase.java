import org.apache.commons.net.ntp.TimeStamp;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ${xiniu}
 * @version $Id: flink_hbase.java,
 */
public class flink_hbase {


    private static String hbaseZookeeperQuorum = "192.168.202.129,192.168.202.130,192.168.202.131";
    private static String hbaseZookeeperClinentPort = "2181";
    private static TableName tableName = TableName.valueOf("dealResult3");
    private static final String columnFamily = "area";



    public static void main(String[] args) {
       // System.setProperty("hadoop.home.dir", "D:\\BaiduNetdiskDownload\\hadoop-2.7.3");

        final String ZOOKEEPER_HOST = "192.168.202.129:2181,192.168.202.130:2181,192.168.202.131:2181";
        final String KAFKA_HOST = "192.168.202.129:9092,192.168.202.120:9092,192.168.202.131:9092";
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(1000); // 非常关键，一定要设置启动检查点！！
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        Properties props = new Properties();
        props.setProperty("zookeeper.connect", ZOOKEEPER_HOST);
        props.setProperty("bootstrap.servers", KAFKA_HOST);
        props.setProperty("group.id", "test-consumer-group");

        DataStream<String> transction = env.addSource(new FlinkKafkaConsumer010<String>("allDataClean", new SimpleStringSchema(), props));
        //DataStream<String> transction1 = env.addSource(new FlinkKafkaConsumer010<String>("test3",new SimpleStringSchema(), props));

        transction.rebalance().map(new MapFunction<String, Object>() {
            public String map(String value)throws IOException {
                System.out.println("=========================================================================");
                System.out.println(value);
                System.out.println("=========================================================================");
                writeIntoHBase(value);
                return value;
            }

        }).print();
        //transction.writeAsText("/home/admin/log2");
        // transction.addSink(new HBaseOutputFormat();
        try {
            env.execute();
        } catch (Exception ex) {

            Logger.getLogger(flink_hbase.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    public static void writeIntoHBase(String m)throws IOException
    {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();

        config.set("hbase.zookeeper.quorum", hbaseZookeeperQuorum);
        config.set("hbase.master", "192.168.202.129:60000");
        config.set("hbase.zookeeper.property.clientPort", hbaseZookeeperClinentPort);
        config.setInt("hbase.rpc.timeout", 20000);
        config.setInt("hbase.client.operation.timeout", 30000);
        config.setInt("hbase.client.scanner.timeout.period", 200000);

        //config.set(TableOutputFormat.OUTPUT_TABLE, hbasetable);

        Connection c = ConnectionFactory.createConnection(config);

        Admin admin = c.getAdmin();
        if(!admin.tableExists(tableName)){
            admin.createTable(new HTableDescriptor(tableName).addFamily(new HColumnDescriptor(columnFamily)));
        }
        Table t = c.getTable(tableName);

        TimeStamp ts = new TimeStamp(new Date());

        Date date = ts.getDate();

        Put put = new Put(org.apache.hadoop.hbase.util.Bytes.toBytes(date.toString()));

        put.addColumn(org.apache.hadoop.hbase.util.Bytes.toBytes(columnFamily), org.apache.hadoop.hbase.util.Bytes.toBytes("test"),
                org.apache.hadoop.hbase.util.Bytes.toBytes(m));
        t.put(put);

        t.close();
        c.close();
    }
}
