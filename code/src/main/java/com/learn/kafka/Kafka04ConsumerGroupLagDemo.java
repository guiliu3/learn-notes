package com.learn.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 第四步：用 Java 代码理解 Consumer Group 的 offset 和 lag。
 *
 * Lag = Partition 最新 offset - 消费组已提交 offset。
 */
public class Kafka04ConsumerGroupLagDemo {

    public static void main(String[] args) throws Exception {
        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaLabConfig.BOOTSTRAP_SERVERS);

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaLabConfig.BOOTSTRAP_SERVERS);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "lag-checker");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (AdminClient adminClient = AdminClient.create(adminProps);
             KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {

            List<TopicPartition> partitions = topicPartitions(consumer);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions, Duration.ofSeconds(5));
            Map<TopicPartition, OffsetAndMetadata> groupOffsets = adminClient
                    .listConsumerGroupOffsets(KafkaLabConfig.GROUP_ID)
                    .partitionsToOffsetAndMetadata()
                    .get();

            System.out.println("==== Consumer Group Lag ====");
            System.out.println("groupId=" + KafkaLabConfig.GROUP_ID);

            for (TopicPartition partition : partitions) {
                long logEndOffset = valueOrZero(endOffsets.get(partition));
                OffsetAndMetadata offsetAndMetadata = groupOffsets.get(partition);
                long currentOffset = offsetAndMetadata == null ? 0 : offsetAndMetadata.offset();
                long lag = logEndOffset - currentOffset;

                System.out.println("topic=" + partition.topic()
                        + ", partition=" + partition.partition()
                        + ", currentOffset=" + currentOffset
                        + ", logEndOffset=" + logEndOffset
                        + ", lag=" + lag);
            }

            System.out.println("\n理解重点：");
            System.out.println("1. currentOffset 是消费组已经提交的消费进度。");
            System.out.println("2. logEndOffset 是 Partition 当前最新位置。");
            System.out.println("3. lag 越大，表示积压消息越多。");
        }
    }

    private static List<TopicPartition> topicPartitions(KafkaConsumer<String, String> consumer) {
        List<PartitionInfo> partitionInfos = consumer.partitionsFor(KafkaLabConfig.TOPIC, Duration.ofSeconds(5));
        if (partitionInfos == null) {
            return Collections.emptyList();
        }

        List<TopicPartition> partitions = new ArrayList<>();
        for (PartitionInfo partitionInfo : partitionInfos) {
            partitions.add(new TopicPartition(partitionInfo.topic(), partitionInfo.partition()));
        }
        return partitions;
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }
}
