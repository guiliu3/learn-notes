package com.learn.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * 第二步：用 Java 代码理解 Producer、Key、Partition。
 *
 * 重点观察：
 * - 相同 key 的消息通常会进入同一个 Partition。
 * - 这就是“同一个办件 bizId 的消息保持局部有序”的基础。
 */
public class Kafka02ProducerKeyPartitionDemo {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaLabConfig.BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 学习阶段先用 all，理解“生产端等待 ISR 确认”这个可靠性概念。
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        String[][] messages = {
                {"AQ-1001", "event=CREATE,bizId=AQ-1001"},
                {"AQ-1002", "event=CREATE,bizId=AQ-1002"},
                {"AQ-1001", "event=APPROVE,bizId=AQ-1001"},
                {"AQ-1003", "event=CREATE,bizId=AQ-1003"},
                {"AQ-1002", "event=FINISH,bizId=AQ-1002"},
                {"AQ-1005", "event=ARCHIVE,bizId=AQ-1005"},
                {"AQ-1006", "event=ARCHIVE,bizId=AQ-1006"},
                {"AQ-1007", "event=ARCHIVE,bizId=AQ-1007"},
                {"AQ-1008", "event=ARCHIVE,bizId=AQ-1008"}
        };

        CountDownLatch latch = new CountDownLatch(messages.length);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (String[] message : messages) {
                String key = message[0];
                String value = message[1];

                ProducerRecord<String, String> record = new ProducerRecord<>(KafkaLabConfig.TOPIC, key, value);
                producer.send(record, new PrintSendResultCallback(key, value, latch));
            }

            latch.await();
            producer.flush();
        }

        System.out.println("\n理解重点：");
        System.out.println("1. Producer 把消息发送到 Topic。");
        System.out.println("2. Key 会参与 Partition 选择，相同 key 通常落到同一个 Partition。");
        System.out.println("3. 如果同一个办件要求顺序处理，就应该使用 bizId 作为 key。");
    }

    private static class PrintSendResultCallback implements Callback {
        private final String key;
        private final String value;
        private final CountDownLatch latch;

        private PrintSendResultCallback(String key, String value, CountDownLatch latch) {
            this.key = key;
            this.value = value;
            this.latch = latch;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            try {
                if (exception != null) {
                    System.out.println("Send failed, key=" + key + ", value=" + value);
                    exception.printStackTrace(System.out);
                    return;
                }

                System.out.println("Send success"
                        + ", key=" + key
                        + ", partition=" + metadata.partition()
                        + ", offset=" + metadata.offset()
                        + ", value=" + value);
            } finally {
                latch.countDown();
            }
        }
    }
}
