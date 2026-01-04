package com.feiwu.yikouai.ai;

import com.feiwu.yikouai.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName:AiAppTitleGeneratorServiceFactory
 * Package:com.feiwu.yikouai
 * Description:
 *
 * @Author: 绯雾sama
 * @Create:2026/1/4 19:38
 * Version: 1.0
 */
@Configuration
@Slf4j
public class AiAppTitleGeneratorServiceFactory {

    public AiAppTitleGeneratorService createAiAppTitleGeneratorService() {
        ChatModel chatModel = SpringContextUtil.getBean("baseChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiAppTitleGeneratorService.class).chatModel(chatModel).build();
    }

    public AiAppTitleGeneratorService aiAppTitleGeneratorService() {
        return createAiAppTitleGeneratorService();
    }
}
