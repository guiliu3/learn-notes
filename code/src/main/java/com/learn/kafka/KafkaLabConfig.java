package com.learn.kafka;

/**
 * Kafka 本地实验公共配置。
 *
 * 启动本地 Kafka 后，这里的 bootstrap servers 对应 component-lab/kafka/config/server-local.properties
 * 中的 advertised.listeners=PLAINTEXT://localhost:9092。
 */
public final class KafkaLabConfig {

    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String TOPIC = "government-sync";
    public static final String GROUP_ID = "sync-service-group";

    private KafkaLabConfig() {
    }
}
