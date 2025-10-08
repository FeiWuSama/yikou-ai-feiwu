package com.feiwu.yikouai.core;

import com.feiwu.yikouai.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassName:AiCodeGeneratorFacadeTest
 * Package:com.feiwu.yikouai.core
 * Description:
 *
 * @Author: 绯雾sama
 * @Create:2025/10/7 12:50
 * Version: 1.0
 */
@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("帮我生成一个最多20行代码的登录页面", CodeGenTypeEnum.MULTI_FILE, 1L);
        Assertions.assertNotNull(file);
    }
}