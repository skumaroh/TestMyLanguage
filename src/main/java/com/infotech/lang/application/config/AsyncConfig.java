package com.infotech.lang.application.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated, bounded executor for background handwriting-recognition work so it
 * never competes unbounded with request-handling threads.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "handwritingRecognitionExecutor")
    public Executor handwritingRecognitionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("handwriting-recognition-");
        executor.initialize();
        return executor;
    }
}
