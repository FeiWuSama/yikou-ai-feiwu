package com.feiwu.yikouai.service;

import com.feiwu.yikouai.model.dto.app.AppQueryDto;
import com.feiwu.yikouai.model.vo.app.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.feiwu.yikouai.model.entity.App;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/feiwusama">绯雾sama</a>
 */
public interface AppService extends IService<App> {

    AppVO getAppVO(App app);

    QueryWrapper getQueryWrapper(AppQueryDto appQueryRequest);

    List<AppVO> getAppVOList(List<App> appList);
}
