package com.suraj.rag.query;

import com.suraj.rag.query.config.EmbeddingServiceProperties;
import com.suraj.rag.query.config.LlmProperties;
import com.suraj.rag.query.config.QueryProperties;
import com.suraj.rag.query.config.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    EmbeddingServiceProperties.class,
    LlmProperties.class,
    QueryProperties.class,
    SecurityProperties.class
})
public class RagQueryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagQueryServiceApplication.class, args);
    }
}
