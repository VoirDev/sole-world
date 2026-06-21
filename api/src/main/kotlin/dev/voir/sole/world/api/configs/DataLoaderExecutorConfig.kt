package dev.voir.sole.world.api.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/** Provides a bounded executor for blocking GraphQL DataLoader database work. */
@Configuration
class DataLoaderExecutorConfig {
    @Bean("graphqlDataLoaderExecutor")
    fun graphqlDataLoaderExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 4
            maxPoolSize = 16
            queueCapacity = 200
            setThreadNamePrefix("graphql-dataloader-")
            initialize()
        }
    }
}
