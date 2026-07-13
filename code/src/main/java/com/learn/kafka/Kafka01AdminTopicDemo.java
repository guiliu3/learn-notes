package com.learn.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * 第一步：用 Java 代码理解 Topic、Partition、Leader、Replicas、ISR。
 *
 * 先启动本地 Kafka，再运行本类。
 */
public class Kafka01AdminTopicDemo {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaLabConfig.BOOTSTRAP_SERVERS);

        try (AdminClient adminClient = AdminClient.create(props)) {
            createTopicIfAbsent(adminClient);
            listTopics(adminClient);
            describeTopic(adminClient);
        }
    }

    private static void createTopicIfAbsent(AdminClient adminClient) throws Exception {
        Set<String> topics = adminClient.listTopics().names().get();
        if (topics.contains(KafkaLabConfig.TOPIC)) {
            System.out.println("Topic already exists: " + KafkaLabConfig.TOPIC);
            return;
        }

        NewTopic topic = new NewTopic(KafkaLabConfig.TOPIC, 3, (short) 1);
        adminClient.createTopics(Collections.singleton(topic)).all().get();
        System.out.println("Created topic: " + KafkaLabConfig.TOPIC);
    }

    private static void listTopics(AdminClient adminClient) throws Exception {
        System.out.println("\n==== All Topics ====");
        for (String topic : adminClient.listTopics().names().get()) {
            System.out.println(topic);
        }
    }

    private static void describeTopic(AdminClient adminClient) throws InterruptedException, ExecutionException {
        System.out.println("\n==== Topic Description ====");
        Map<String, TopicDescription> topicMap = adminClient
                .describeTopics(Collections.singleton(KafkaLabConfig.TOPIC))
                .allTopicNames()
                .get();

        TopicDescription description = topicMap.get(KafkaLabConfig.TOPIC);
        System.out.println("Topic: " + description.name());

        for (TopicPartitionInfo partitionInfo : description.partitions()) {
            Node leader = partitionInfo.leader();
            System.out.println("Partition: " + partitionInfo.partition());
            System.out.println("  Leader: " + nodeText(leader));
            System.out.println("  Replicas: " + partitionInfo.replicas());
            System.out.println("  ISR: " + partitionInfo.isr());
        }

        System.out.println("\n理解重点：");
        System.out.println("1. Topic 是逻辑分类，真正存储消息的是 Partition。");
        System.out.println("2. 当前本地单节点，所以 Leader、Replicas、ISR 都指向同一个 Broker。");
        System.out.println("3. 3 个 Partition 表示这个 Topic 最多可以被同一个 Consumer Group 内 3 个消费者并行消费。");
    }

    private static String nodeText(Node node) {
        if (node == null) {
            return "none";
        }
        return "id=" + node.id() + ", host=" + node.host() + ", port=" + node.port();
    }
}
