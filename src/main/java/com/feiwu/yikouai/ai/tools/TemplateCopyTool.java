package com.feiwu.yikouai.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.feiwu.yikouai.constant.AppConstant;
import com.feiwu.yikouai.model.enums.CodeGenTypeEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 模板复制工具
 * 用于将预定义的项目模板复制到指定目录
 */
@Slf4j
@Component
public class TemplateCopyTool extends BaseTool {

    @Override
    public String getToolName() {
        return "copyTemplate";
    }

    @Override
    public String getDisplayName() {
        return "复制项目模板";
    }

    /**
     * 复制Vue项目模板到指定目录（空目录不会被复制）
     *
     * @param templateType 模板类型，如 "vue-project"
     * @param targetDir    目标目录的相对路径
     * @param appId        应用ID
     * @return 复制结果信息
     */
    @Tool("将预定义的项目模板复制到指定目录，用于快速创建项目结构（空目录不会被复制）")
    public String copyTemplate(
            @P("模板类型，目前支持 'vue_project'") String templateType,
            @P("目标目录的相对路径") String targetDir,
            @ToolMemoryId Long appId
    ) {
        try {
            // 验证模板类型
            if (!CodeGenTypeEnum.VUE_PROJECT.getValue().equals(templateType)) {
                return "错误：不支持的模板类型：" + templateType + "，目前只支持 'vue-project'";
            }

            // 获取模板资源路径
            ClassPathResource templateResource = new ClassPathResource("template/" + templateType);
            if (!templateResource.exists()) {
                return "错误：模板资源不存在：" + templateType;
            }

            // 处理目标路径
            Path targetPath = Paths.get(targetDir);
            if (!targetPath.isAbsolute()) {
                // 相对路径处理，创建基于 appId 的项目目录
                String projectDirName = CodeGenTypeEnum.VUE_PROJECT.getValue() + "_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                targetPath = projectRoot.resolve(targetDir);
            }

            // 复制模板内容
            File templateDir = templateResource.getFile();
            File targetDirectory = targetPath.toFile();

            // 创建目标目录
            if (!targetDirectory.exists()) {
                Files.createDirectories(targetDirectory.toPath());
            }

            // 复制文件
            FileUtil.copyContent(templateDir, targetDirectory, true);

            log.info("成功复制模板 {} 到目录: {}", templateType, targetDirectory.getAbsolutePath());
            return "模板复制成功: " + targetDir;
        } catch (IOException e) {
            String errorMessage = "模板复制失败: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return "模板复制工具执行完成";
    }
}