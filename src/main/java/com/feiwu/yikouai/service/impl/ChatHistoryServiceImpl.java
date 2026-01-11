package com.feiwu.yikouai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.feiwu.yikouai.ai.AiBaseGeneratorService;
import com.feiwu.yikouai.ai.AiBaseGeneratorServiceFactory;
import com.feiwu.yikouai.constant.UserConstant;
import com.feiwu.yikouai.exception.ErrorCode;
import com.feiwu.yikouai.exception.ThrowUtils;
import com.feiwu.yikouai.model.dto.chatHistory.ChatHistoryQueryDto;
import com.feiwu.yikouai.model.entity.App;
import com.feiwu.yikouai.model.entity.User;
import com.feiwu.yikouai.model.enums.ChatHistoryMessageTypeEnum;
import com.feiwu.yikouai.service.AppService;
import com.feiwu.yikouai.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.feiwu.yikouai.model.entity.ChatHistory;
import com.feiwu.yikouai.mapper.ChatHistoryMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/feiwusama">绯雾sama</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

    @Resource
    private AiBaseGeneratorServiceFactory aiBaseGeneratorServiceFactory;

    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");

        // 获取当前最大的turnNumber
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("appId", appId);
        queryWrapper.eq("messageType", ChatHistoryMessageTypeEnum.USER.getValue());
        queryWrapper.orderBy("turnNumber", false);
        ChatHistory dbChatHistory = this.getOne(queryWrapper);
        if (dbChatHistory == null) {
            dbChatHistory = ChatHistory.builder().turnNumber(0L).build();
        }

        // 检查对话历史轮数，如果超过 10 轮，生成总结
        if (messageType.equals(ChatHistoryMessageTypeEnum.AI.getValue()) && dbChatHistory.getTurnNumber() == 1) {
            // 获取所有对话历史，用于生成总结
            QueryWrapper historyQueryWrapper = new QueryWrapper();
            historyQueryWrapper.eq("appId", appId);
            historyQueryWrapper.orderBy("createTime", true);
            List<ChatHistory> chatHistoryList = this.list(historyQueryWrapper);

            // 调用AI生成对话总结
            AiBaseGeneratorService aiBaseGeneratorService = aiBaseGeneratorServiceFactory.createAiBaseGeneratorService();
            String summary = aiBaseGeneratorService.generateChatHistorySummary(chatHistoryList);

            // 保存总结作为新的对话历史
            ChatHistory summaryHistory = ChatHistory.builder()
                    .appId(appId)
                    .message(summary)
                    .messageType(ChatHistoryMessageTypeEnum.SUMMARY.getValue())
                    .userId(userId)
                    .turnNumber(dbChatHistory.getTurnNumber())
                    .build();
            this.save(summaryHistory);
        }
        // 验证消息类型是否有效
        ChatHistory chatHistory = null;
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);
        if (messageType.equals(ChatHistoryMessageTypeEnum.AI.getValue())) {
            chatHistory = ChatHistory.builder()
                    .appId(appId)
                    .message(message)
                    .messageType(messageType)
                    .userId(userId)
                    .turnNumber(dbChatHistory.getTurnNumber())
                    .build();
        } else {
            chatHistory = ChatHistory.builder()
                    .appId(appId)
                    .message(message)
                    .messageType(messageType)
                    .userId(userId)
                    .turnNumber(dbChatHistory.getTurnNumber() + 1)
                    .build();
        }
        return this.save(chatHistory);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 根据聊天历史查询数据传输对象获取 ai 和 user 消息类型的查询包装器
     *
     * @return 返回一个QueryWrapper对象，用于构建数据库 ai 和 user 消息类型的查询条件
     */
    public QueryWrapper getChatHistoryQueryWrapper(ChatHistoryQueryDto chatHistoryQueryDto){
        QueryWrapper queryWrapper = this.getQueryWrapper(chatHistoryQueryDto);
        queryWrapper.in(ChatHistory::getMessageType, 
            ChatHistoryMessageTypeEnum.USER.getValue(), 
            ChatHistoryMessageTypeEnum.AI.getValue());
        return queryWrapper;
    }

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryDto
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryDto chatHistoryQueryDto) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryDto == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryDto.getId();
        String message = chatHistoryQueryDto.getMessage();
        String messageType = chatHistoryQueryDto.getMessageType();
        Long appId = chatHistoryQueryDto.getAppId();
        Long userId = chatHistoryQueryDto.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryDto.getLastCreateTime();
        String sortField = chatHistoryQueryDto.getSortField();
        String sortOrder = chatHistoryQueryDto.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryDto chatHistoryQueryDto = new ChatHistoryQueryDto();
        chatHistoryQueryDto.setAppId(appId);
        chatHistoryQueryDto.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getChatHistoryQueryWrapper(chatHistoryQueryDto);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .in(ChatHistory::getMessageType, 
                        ChatHistoryMessageTypeEnum.USER.getValue(), 
                        ChatHistoryMessageTypeEnum.AI.getValue())
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }


}
