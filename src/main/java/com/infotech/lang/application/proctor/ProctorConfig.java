package com.infotech.lang.application.proctor;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** A small dedicated executor so generating texts in the background never competes with web requests. */
@Configuration
public class ProctorConfig {

    @Bean(name = "proctorExecutor")
    public Executor proctorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("proctor-");
        executor.initialize();
        return executor;
    }
}
