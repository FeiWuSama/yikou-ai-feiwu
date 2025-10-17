package com.feiwu.yikouai.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * ClassName:ProjectDownloadService
 * Package:com.feiwu.yikouai.service
 * Description:
 *
 * @Author: 绯雾sama
 * @Create:2025/10/17 17:01
 * Version: 1.0
 */
public interface ProjectDownloadService {
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
