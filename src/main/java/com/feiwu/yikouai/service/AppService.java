package com.feiwu.yikouai.service;

import com.feiwu.yikouai.model.dto.app.AppAddDto;
import com.feiwu.yikouai.model.dto.app.AppQueryDto;
import com.feiwu.yikouai.model.entity.User;
import com.feiwu.yikouai.model.vo.app.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.feiwu.yikouai.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/feiwusama">绯雾sama</a>
 */
public interface AppService extends IService<App> {

    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    Long createApp(AppAddDto appAddDto, User loginUser);

    String deployApp(Long appId, User loginUser);

    void generateAppScreenshotAsync(Long appId, String appUrl);

    AppVO getAppVO(App app);

    QueryWrapper getQueryWrapper(AppQueryDto appQueryRequest);

    List<AppVO> getAppVOList(List<App> appList);
}
