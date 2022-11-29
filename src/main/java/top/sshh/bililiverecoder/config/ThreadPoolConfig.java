package top.sshh.bililiverecoder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class ThreadPoolConfig {

    @Bean("myAsyncPool")
    public TaskExecutor myAsyncThreadPool() {
        return new SimpleAsyncTaskExecutor();
    }
}
