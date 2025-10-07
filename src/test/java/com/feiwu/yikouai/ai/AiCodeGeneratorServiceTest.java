package com.feiwu.yikouai.ai;

import com.feiwu.yikouai.ai.model.HtmlCodeResult;
import com.feiwu.yikouai.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassName:AiCodeGeneratorServiceTest
 * Package:com.feiwu.yikouai.ai
 * Description:
 *
 * @Author: 绯雾sama
 * @Create:2025/10/7 12:01
 * Version: 1.0
 */
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult generateHtmlCode = aiCodeGeneratorService.generateHtmlCode("帮我生成一个小型的博客,不超过20行");
        Assertions.assertNotNull(generateHtmlCode);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult generateMultiFileCode = aiCodeGeneratorService.generateMultiFileCode("帮我生成一个小小型的留言板，不超过50行");
        Assertions.assertNotNull(generateMultiFileCode);
    }
}