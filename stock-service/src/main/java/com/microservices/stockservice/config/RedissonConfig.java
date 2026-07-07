package com.microservices.stockservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        // Read from environment variables with fallback defaults for local development
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        String redisPortStr = System.getenv().getOrDefault("REDIS_PORT", "6379");
        String redisPassword = System.getenv().getOrDefault("REDIS_PASSWORD", "");
        
        int redisPort;
        try {
            redisPort = Integer.parseInt(redisPortStr);
        } catch (NumberFormatException e) {
            redisPort = 6379;
        }
        
        Config config = new Config();
        String connectionAddress = "redis://" + redisHost + ":" + redisPort;
        
        config.useSingleServer()
                .setAddress(connectionAddress)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword);
        
        return Redisson.create(config);
    }
}

