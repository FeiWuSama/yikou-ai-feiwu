package com.feiwu.yikouai.service;

import com.feiwu.yikouai.model.dto.chatHistory.ChatHistoryQueryDto;
import com.feiwu.yikouai.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.feiwu.yikouai.model.entity.ChatHistory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/feiwusama">绯雾sama</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加聊天消息的方法
     *
     * @param appId       应用ID，用于标识不同的应用
     * @param message     聊天消息内容
     * @param messageType 消息类型，用于区分不同种类的消息
     * @param userId      用户ID，标识发送消息的用户
     * @return 返回一个布尔值，表示消息添加是否成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用ID删除相关记录
     *
     * @param appId 应用的唯一标识符
     * @return 删除操作是否成功执行，true表示成功，false表示失败
     */
    boolean deleteByAppId(Long appId);

    /**
     * 根据聊天历史查询数据传输对象获取查询包装器
     *
     * @param chatHistoryQueryDto 聊天历史查询数据传输对象，包含查询条件
     * @return 返回一个QueryWrapper对象，用于构建数据库查询条件
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryDto chatHistoryQueryDto);

    /**
     * 分页查询应用聊天记录
     *
     * @param appId          应用ID
     * @param pageSize       每页大小
     * @param lastCreateTime 上一次查询的创建时间（用于分页）
     * @param loginUser      登录用户信息
     * @return 分页后的聊天记录列表
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);
}
