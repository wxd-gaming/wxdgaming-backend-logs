package wxdgaming.backends.admin.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.login.LoginService;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.http.HttpContext;
import wxdgaming.boot2.starter.net.server.http.HttpFilter;

import java.lang.reflect.Method;

/**
 * 检查登陆权限
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-19 15:02
 **/
@Slf4j
@Singleton
public class Authority_9_ByUser_Filter extends HttpFilter {

    final LoginService loginService;

    @Inject
    public Authority_9_ByUser_Filter(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override public Object doFilter(HttpRequest httpRequest, Method method, String url, HttpContext httpContext) {
        if (httpRequest != null && Objects.checkHave(httpRequest.authority(), 9)) {
            RunResult runResult = loginService.checkLogin(httpContext);
            if (runResult.code() != 1) {
                return runResult;
            }
        }
        return null;
    }

}
