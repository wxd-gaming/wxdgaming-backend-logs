package wxdgaming.backends.admin.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import wxdgaming.backends.admin.login.LoginService;
import wxdgaming.boot.core.lang.RunResult;
import wxdgaming.boot.net.web.hs.HttpListenerAction;
import wxdgaming.boot.starter.net.filter.HttpFilter;

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

    @Override public boolean doFilter(HttpListenerAction httpListenerAction) {
        if (httpListenerAction.getSession().getUriPath().endsWith("/list")) {
            RunResult runResult = loginService.checkLogin(httpListenerAction.getSession());
            if (runResult.code() != 1) {
                httpListenerAction.getSession().responseJson(runResult);
                return false;
            }
        }
        return true;
    }

}
