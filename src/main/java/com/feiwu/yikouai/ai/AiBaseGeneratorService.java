package com.feiwu.yikouai.ai;

import com.feiwu.yikouai.model.entity.ChatHistory;
import dev.langchain4j.service.SystemMessage;

import java.util.List;

/**
 * ClassName:AiAppTitleGeneratorService
 * Package:com.feiwu.yikouai.ai
 * Description:
 *
 * @Author: 绯雾sama
 * @Create:2026/1/4 19:28
 * Version: 1.0
 */
public interface AiBaseGeneratorService {

    @SystemMessage(fromResource = "prompt/app-title-gen-system-prompt.txt")
    String generateAppTitle(String userMessage);

    @SystemMessage(fromResource = "prompt/chat-history-summary-prompt.txt")
    String generateChatHistorySummary(List<ChatHistory> chatHistoryList);
}
