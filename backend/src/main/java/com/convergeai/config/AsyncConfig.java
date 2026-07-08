package com.convergeai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async execution setup. With {@code spring.threads.virtual.enabled=true} the
 * default {@code @Async} executor already uses virtual threads; the dedicated
 * {@code agentExecutor} is used for fanning out the three concurrent LLM calls
 * inside a debate round, where each call blocks on network I/O for seconds —
 * exactly the workload Java 21 virtual threads are built for.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "agentExecutor", destroyMethod = "close")
    public ExecutorService agentExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("agent-call-", 0).factory());
    }
}
