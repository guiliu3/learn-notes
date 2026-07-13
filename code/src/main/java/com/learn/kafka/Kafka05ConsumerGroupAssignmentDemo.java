package com.learn.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 第五步：用 Java 代码观察 Consumer Group 的 Partition 分配。
 *
 * 使用方式：
 * 1. 先启动 Kafka。
 * 2. 启动两个 Kafka03ConsumerGroupDemo 实例，不要关闭。
 * 3. 再运行本类。
 *
 * 你会看到：
 * - 当前 group 里有哪些 consumer 实例。
 * - 每个 consumer 被分配了哪些 TopicPartition。
 */
public class Kafka05ConsumerGroupAssignmentDemo {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaLabConfig.BOOTSTRAP_SERVERS);

        try (AdminClient adminClient = AdminClient.create(props)) {
            ConsumerGroupDescription group = adminClient
                    .describeConsumerGroups(Collections.singleton(KafkaLabConfig.GROUP_ID))
                    .all()
                    .get()
                    .get(KafkaLabConfig.GROUP_ID);

            printGroupBaseInfo(group);
            printMemberAssignments(group.members());
            printPartitionView(group.members());
        }
    }

    private static void printGroupBaseInfo(ConsumerGroupDescription group) {
        System.out.println("==== Consumer Group ====");
        System.out.println("groupId: " + group.groupId());
        System.out.println("state: " + group.state());
        System.out.println("partitionAssignor: " + group.partitionAssignor());
        System.out.println("members: " + group.members().size());
        System.out.println();
    }

    private static void printMemberAssignments(Collection<MemberDescription> members) {
        System.out.println("==== Consumer -> Partitions ====");

        if (members.isEmpty()) {
            System.out.println("No active consumer members. Start Kafka03ConsumerGroupDemo first.");
            return;
        }

        List<MemberDescription> sortedMembers = new ArrayList<>(members);
        sortedMembers.sort(Comparator.comparing(MemberDescription::consumerId));

        for (MemberDescription member : sortedMembers) {
            System.out.println("consumerId: " + member.consumerId());
            System.out.println("clientId: " + member.clientId());
            System.out.println("host: " + member.host());

            List<TopicPartition> partitions = sortedPartitions(member.assignment());
            if (partitions.isEmpty()) {
                System.out.println("assignedPartitions: []");
            } else {
                System.out.println("assignedPartitions:");
                for (TopicPartition partition : partitions) {
                    System.out.println("  - topic=" + partition.topic() + ", partition=" + partition.partition());
                }
            }
            System.out.println();
        }
    }

    private static void printPartitionView(Collection<MemberDescription> members) {
        System.out.println("==== Partition -> Consumer ====");

        Map<TopicPartition, String> partitionOwner = new LinkedHashMap<>();
        for (MemberDescription member : members) {
            for (TopicPartition partition : sortedPartitions(member.assignment())) {
                partitionOwner.put(partition, member.consumerId());
            }
        }

        List<TopicPartition> partitions = new ArrayList<>(partitionOwner.keySet());
        partitions.sort(topicPartitionComparator());

        for (TopicPartition partition : partitions) {
            System.out.println("topic=" + partition.topic()
                    + ", partition=" + partition.partition()
                    + " -> consumerId=" + partitionOwner.get(partition));
        }

        System.out.println();
        System.out.println("理解重点：");
        System.out.println("1. Consumer Group 的分配单位是 Partition，不是单条消息。");
        System.out.println("2. 同一个 Partition 同一时刻只会分配给组内一个 Consumer。");
        System.out.println("3. 如果消息都落在某一个 Partition，只有负责该 Partition 的 Consumer 能消费到消息。");
    }

    private static List<TopicPartition> sortedPartitions(MemberAssignment assignment) {
        List<TopicPartition> partitions = new ArrayList<>(assignment.topicPartitions());
        partitions.sort(topicPartitionComparator());
        return partitions;
    }

    private static Comparator<TopicPartition> topicPartitionComparator() {
        return Comparator
                .comparing(TopicPartition::topic)
                .thenComparingInt(TopicPartition::partition);
    }
}
