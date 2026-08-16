package com.pushkar.codereview.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    void testTaskExecutorBean() {
        AsyncConfig asyncConfig = new AsyncConfig();
        Executor executor = asyncConfig.taskExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;

        assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(4);
        assertThat(threadPoolExecutor.getMaxPoolSize()).isEqualTo(8);
        assertThat(threadPoolExecutor.getThreadNamePrefix()).isEqualTo("ai-review-");
    }
}
