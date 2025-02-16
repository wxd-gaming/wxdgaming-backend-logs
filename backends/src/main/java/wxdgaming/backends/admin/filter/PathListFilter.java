package wxdgaming.backends.admin.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import wxdgaming.backends.admin.login.LoginService;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.server.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.http.HttpContext;
import wxdgaming.boot2.starter.net.server.http.HttpFilter;

import java.lang.reflect.Method;

/**
 * 路由 xxx/list
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-10 16:46
 **/
@Singleton
public class PathListFilter extends HttpFilter {

    LoginService loginService;

    @Inject
    public PathListFilter(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override public boolean doFilter(HttpRequest httpRequest, Method method, String url, HttpContext httpContext) {
        if ((httpRequest != null && httpRequest.authority() > -1) || httpContext.getRequest().getUriPath().endsWith("/list")) {
            RunResult runResult = loginService.checkLogin(httpContext);
            if (runResult.code() != 1) {
                httpContext.getResponse().responseJson(runResult);
                return false;
            }
        }
        return true;
    }

}
