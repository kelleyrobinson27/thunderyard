package com.thunderyard.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    public NewTopic appointmentEventsTopic() {
        return TopicBuilder.name(KafkaTopics.APPOINTMENT_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic smsJobsTopic() {
        return TopicBuilder.name(KafkaTopics.SMS_JOBS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic smsJobsDlqTopic() {
        return TopicBuilder.name(KafkaTopics.SMS_JOBS_DLQ).partitions(1).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (ConsumerRecord<?, ?> record, Exception ex) ->
                        log.error("Skipping Kafka record topic={} partition={} offset={}",
                                record.topic(), record.partition(), record.offset(), ex),
                new FixedBackOff(0L, 0L)
        );
        handler.addNotRetryableExceptions(DeserializationException.class);
        return handler;
    }
}
