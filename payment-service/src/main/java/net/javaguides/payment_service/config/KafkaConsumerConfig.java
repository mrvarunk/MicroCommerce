package net.javaguides.payment_service.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);
    
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;
    
    /**
     * Error handler for Kafka consumer failures
     * - Implements exponential backoff for retries
     * - Logs errors instead of crashing the application
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        logger.info("Configuring Kafka error handler with retry policy");
        
        // Backoff: max 3 retries with 5-second fixed delay between retries
        FixedBackOff backOff = new FixedBackOff(5000L, 3);
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
        
        // Log errors without crashing
        errorHandler.setLogLevel(org.springframework.kafka.KafkaException.Level.WARN);
        
        return errorHandler;
    }
}
