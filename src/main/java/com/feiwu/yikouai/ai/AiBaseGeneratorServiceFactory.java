package com.feiwu.yikouai.ai;

import com.feiwu.yikouai.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
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
public class AiBaseGeneratorServiceFactory {

    public AiBaseGeneratorService createAiBaseGeneratorService() {
        ChatModel chatModel = SpringContextUtil.getBean("baseChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiBaseGeneratorService.class).chatModel(chatModel).build();
    }

    public AiBaseGeneratorService AiBaseGeneratorService() {
        return createAiBaseGeneratorService();
    }
}
