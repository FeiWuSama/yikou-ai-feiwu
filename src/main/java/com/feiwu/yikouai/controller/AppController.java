package com.feiwu.yikouai.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.feiwu.yikouai.annotation.AuthCheck;
import com.feiwu.yikouai.annotation.RateLimit;
import com.feiwu.yikouai.common.BaseResponse;
import com.feiwu.yikouai.common.DeleteRequest;
import com.feiwu.yikouai.common.ResultUtils;
import com.feiwu.yikouai.constant.AppConstant;
import com.feiwu.yikouai.constant.UserConstant;
import com.feiwu.yikouai.exception.BusinessException;
import com.feiwu.yikouai.exception.ErrorCode;
import com.feiwu.yikouai.exception.ThrowUtils;
import com.feiwu.yikouai.langgraph4j.model.enums.RateLimitType;
import com.feiwu.yikouai.model.dto.app.*;
import com.feiwu.yikouai.model.entity.User;
import com.feiwu.yikouai.model.enums.CodeGenTypeEnum;
import com.feiwu.yikouai.model.vo.app.AppVO;
import com.feiwu.yikouai.service.ProjectDownloadService;
import com.feiwu.yikouai.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import com.feiwu.yikouai.model.entity.App;
import com.feiwu.yikouai.service.AppService;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用 控制层。
 *
 * @author <a href="https://github.com/feiwusama">绯雾sama</a>
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    // 使用 ConcurrentHashMap 类型能避免明显的并发问题
    private final Map<Long, Disposable> disposableMap = new ConcurrentHashMap<>();

    /**
     * 应用聊天生成代码（流式 SSE）
     *
     * @param appId   应用 ID
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI 对话请求过于频繁，请稍后再试")
    @AuthCheck(mustRole = UserConstant.USER_LOGIN_STATE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务生成代码（流式）
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);

        // 使用Flux.create创建可控流(Spring WebFlux 会自动订阅流，所以避免直接调用subscribe())
        return Flux.create(sink -> {
            // 转换为 ServerSentEvent 格式并订阅
            Disposable disposable = contentFlux
                    .map(chunk -> {
                        // 将内容包装成JSON对象
                        Map<String, String> wrapper = Map.of("d", chunk);
                        String jsonData = JSONUtil.toJsonStr(wrapper);
                        return ServerSentEvent.<String>builder()
                                .data(jsonData)
                                .build();
                    })
                    .concatWith(Mono.just(
                            // 发送结束事件
                            ServerSentEvent.<String>builder()
                                    .event("done")
                                    .data("")
                                    .build()
                    ))
                    .subscribe(
                            // 正常处理
                            sink::next,
                            // 错误处理
                            sink::error,
                            // 完成处理
                            sink::complete
                    );

            // 保存disposable，用于后续中断
            disposableMap.put(appId, disposable);

            // 当流取消或结束时从map中移除
            sink.onCancel(() -> {
                disposable.dispose();
                disposableMap.remove(appId);
            });

            sink.onDispose(() -> {
                disposableMap.remove(appId);
            });
        });
    }

    /**
     * 中断应用聊天生成代码（流式 SSE）
     *
     * @param appId   应用 ID
     * @param request 请求对象
     * @return 生成结果流
     */
    @GetMapping("/chat/gen/stop")
    @AuthCheck(mustRole = UserConstant.USER_LOGIN_STATE)
    public BaseResponse<Boolean> stopToGenCode(@RequestParam Long appId, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 验证应用权限
        AppQueryDto appQueryDto = new AppQueryDto();
        appQueryDto.setId(appId);
        appQueryDto.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryDto);
        long count = appService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id错误或无权限操作");
        }

        // 一次性获取并移除disposable，保证该操作的原子性，避免并发问题
        Disposable disposable = disposableMap.remove(appId);
        if (disposable == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有正在进行的代码生成任务");
        }

        // 中断流（检查是否已经被中断，避免重复中断）
        if (!disposable.isDisposed()) {
            disposable.dispose();
        }

        return ResultUtils.success(true);
    }

    /**
     * 应用部署
     *
     * @param appDeployDto 部署请求
     * @param request      请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    @AuthCheck(mustRole = UserConstant.USER_LOGIN_STATE)
    public BaseResponse<String> deployApp(@RequestBody AppDeployDto appDeployDto, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployDto == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployDto.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 取消应用部署
     *
     * @param appDeployCancelDto 部署取消请求
     * @param request            请求
     * @return 部署 URL
     */
    @PostMapping("/deploy/cancel")
    @AuthCheck(mustRole = UserConstant.USER_LOGIN_STATE)
    public BaseResponse<Boolean> deployAppCancel(@RequestBody AppDeployCancelDto appDeployCancelDto, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployCancelDto == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployCancelDto.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        Boolean isSuccess = appService.deployAppCancel(appId, loginUser);
        ThrowUtils.throwIf(!isSuccess, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 下载应用代码
     *
     * @param appId    应用ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    @AuthCheck(mustRole = UserConstant.USER_LOGIN_STATE)
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验：只有应用创建者可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 6. 生成下载文件名（不建议添加中文内容）
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }


    /**
     * 创建应用
     *
     * @param appAddDto 创建应用请求
     * @param request   请求
     * @return 应用 id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.USER_LOGIN_STATE)
    public BaseResponse<Long> addApp(@RequestBody AppAddDto appAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddDto == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Long appId = appService.createApp(appAddDto, loginUser);
        return ResultUtils.success(appId);
    }


    /**
     * 更新应用（用户只能更新自己的应用名称）
     *
     * @param appUpdateDto 更新请求
     * @param request      请求
     * @return 更新结果
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.USER_LOGIN_STATE)
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateDto appUpdateDto, HttpServletRequest request) {
        if (appUpdateDto == null || appUpdateDto.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = appUpdateDto.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可更新
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(id);
        app.setAppName(appUpdateDto.getAppName());
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除应用（用户只能删除自己的应用）
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.USER_LOGIN_STATE)
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类（包含用户信息）
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页获取当前用户创建的应用列表
     *
     * @param appQueryDto 查询请求
     * @param request     请求
     * @return 应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryDto appQueryDto, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryDto == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 限制每页最多 20 个
        long pageSize = appQueryDto.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryDto.getPageNum();
        // 只查询当前用户的应用
        appQueryDto.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryDto);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页获取精选应用列表
     *
     * @param appQueryDto 查询请求
     * @return 精选应用列表
     */
    @PostMapping("/good/list/page/vo")
    @Cacheable(
            value = "good_app_page",
            key = "T(com.feiwu.yikouai.utils.CacheKeyUtils).generateKey(#appQueryDto)",
            condition = "#appQueryDto.pageNum <= 10"
    )
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryDto appQueryDto) {
        ThrowUtils.throwIf(appQueryDto == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryDto.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryDto.getPageNum();
        // 只查询精选的应用
        appQueryDto.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryDto);
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员删除应用
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(oldApp.getCodeGenType());
        // 删除相关的源码资源和部署资源
        FileUtil.del(AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator +
                codeGenTypeEnum.getValue() + "_" + oldApp.getId());
        FileUtil.del(AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + oldApp.getDeployKey());
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新应用
     *
     * @param appAdminUpdateDto 更新请求
     * @return 更新结果
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateDto appAdminUpdateDto) {
        if (appAdminUpdateDto == null || appAdminUpdateDto.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateDto.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateDto, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页获取应用列表
     *
     * @param appQueryDto 查询请求
     * @return 应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryDto appQueryDto) {
        ThrowUtils.throwIf(appQueryDto == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryDto.getPageNum();
        long pageSize = appQueryDto.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryDto);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAppVO(app));
    }

}
