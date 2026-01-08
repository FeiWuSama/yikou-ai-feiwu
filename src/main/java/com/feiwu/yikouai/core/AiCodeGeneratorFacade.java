package com.feiwu.yikouai.core;

import cn.hutool.json.JSONUtil;
import com.feiwu.yikouai.ai.AiCodeGeneratorService;
import com.feiwu.yikouai.ai.AiCodeGeneratorServiceFactory;
import com.feiwu.yikouai.ai.model.HtmlCodeResult;
import com.feiwu.yikouai.ai.model.MultiFileCodeResult;
import com.feiwu.yikouai.ai.model.message.AiResponseMessage;
import com.feiwu.yikouai.ai.model.message.ToolExecutedMessage;
import com.feiwu.yikouai.ai.model.message.ToolRequestMessage;
import com.feiwu.yikouai.constant.AppConstant;
import com.feiwu.yikouai.core.builder.VueProjectBuilder;
import com.feiwu.yikouai.core.parse.CodeParserExecutor;
import com.feiwu.yikouai.core.saver.CodeFileSaverExecutor;
import com.feiwu.yikouai.exception.BusinessException;
import com.feiwu.yikouai.exception.ErrorCode;
import com.feiwu.yikouai.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;
    
    // 用于跟踪所有正在执行的流和它们的执行线程
    private final Map<String, Thread> activeStreams = new ConcurrentHashMap<>();
    
    // 生成唯一的流ID
    private String generateStreamId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用id
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用id
     * @return Flux<String> 流式响应
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream,appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @param appId Long 应用id
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream,Long appId) {
        return Flux.create(sink -> {
            // 创建一个原子布尔值来跟踪是否已取消
            AtomicBoolean isCancelled = new AtomicBoolean(false);
            
            // 创建一个线程引用，用于存储实际执行AiService方法的线程
            Thread[] aiServiceThread = new Thread[1];
            
            // 生成唯一的流ID
            String streamId = generateStreamId();
            
            tokenStream.onPartialResponse((String partialResponse) -> {
                        if(sink.isCancelled() || isCancelled.get()){
                            sink.complete();
                            return;
                        }
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        if(sink.isCancelled() || isCancelled.get()){
                            sink.complete();
                            return;
                        }
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        if(sink.isCancelled() || isCancelled.get()){
                            sink.complete();
                            return;
                        }
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        if(sink.isCancelled() || isCancelled.get()){
                            sink.complete();
                            return;
                        }
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath);
                    })
                    .onError((Throwable error) -> {
                        if(sink.isCancelled() || isCancelled.get()){
                            sink.complete();
                            return;
                        }
                        log.error("转换失败: {}", error.getMessage());
                        sink.error(error);
                    })
                    .start();
            
            // 保存实际执行AiService方法的线程
            aiServiceThread[0] = Thread.currentThread();
            
            // 将流ID和线程关联起来，以便在取消时中断
            activeStreams.put(streamId, aiServiceThread[0]);
            
            sink.onCancel(() -> {
                isCancelled.set(true);
                log.info("中断流: {}", streamId);
                
                // 获取并中断执行AiService方法的线程
                Thread thread = activeStreams.remove(streamId);
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }
                
                // 完成 Flux
                sink.complete();
            });
            
            // 当Flux完成或错误时，清理资源
            sink.onDispose(() -> {
                activeStreams.remove(streamId);
            });
        });
    }


    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用id
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段
        return codeStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                // 使用执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 使用执行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，路径为：{}", savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }

}
