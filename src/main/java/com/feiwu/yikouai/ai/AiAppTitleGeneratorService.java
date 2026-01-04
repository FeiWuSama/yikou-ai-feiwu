package com.feiwu.yikouai.ai;

import dev.langchain4j.service.SystemMessage;

/**
 * ClassName:AiAppTitleGeneratorService
 * Package:com.feiwu.yikouai.ai
 * Description:
 *
 * @Author: 绯雾sama
 * @Create:2026/1/4 19:28
 * Version: 1.0
 */
public interface AiAppTitleGeneratorService {

    @SystemMessage(fromResource = "prompt/app-title-gen-system-prompt.txt")
    String generateAppTitle(String userMessage);
}
