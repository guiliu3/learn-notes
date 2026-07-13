package com.learn.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * 第三步：用 Java 代码理解 Consumer Group 和 Offset。
 *
 * 可以在 IDE 中启动两个本类实例，观察同一个 Consumer Group 内消息会被分摊消费。
 * 停止程序后重新启动，观察它不会从头消费，而是从已提交 offset 后继续。
 */
public class Kafka03ConsumerGroupDemo {

    public static void main(String[] args) {
        String consumerName = args.length > 0 ? args[0] : "consumer-" + System.currentTimeMillis();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaLabConfig.BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaLabConfig.GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // 第一次没有 offset 时，从最早的消息开始读。已有 offset 后，这个配置不会让它重新从头读。
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // 学习阶段关闭自动提交，业务处理成功后手动提交。
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(KafkaLabConfig.TOPIC));

            System.out.println(consumerName + " started, groupId=" + KafkaLabConfig.GROUP_ID);
            System.out.println("Press Ctrl+C to stop.");

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(consumerName
                            + " received"
                            + ", partition=" + record.partition()
                            + ", offset=" + record.offset()
                            + ", key=" + record.key()
                            + ", value=" + record.value());

                    // 这里模拟你的数据同步平台：根据 bizId 查源库，然后执行责任链 Handler。
                    // 真正项目中，只有业务处理成功后才应该提交 offset。
                }

                consumer.commitSync();
                System.out.println(consumerName + " committed offset.");
            }
        }
    }
}
