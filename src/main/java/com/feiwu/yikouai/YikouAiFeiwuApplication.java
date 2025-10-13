package com.feiwu.yikouai;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.feiwu.yikouai.mapper")
public class YikouAiFeiwuApplication {

    public static void main(String[] args) {
        SpringApplication.run(YikouAiFeiwuApplication.class, args);
    }

}
