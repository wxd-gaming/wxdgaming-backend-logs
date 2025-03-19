package wxdgaming.backends.admin.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.admin.login.LoginService;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.http.HttpContext;
import wxdgaming.boot2.starter.net.server.http.HttpListenerFactory;

import java.lang.reflect.Method;

/**
 * 路由 xxx/list
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-10 16:46
 **/
@Slf4j
@Singleton
public class Authority_ByUser_Filter_PathList extends Authority_ByUser_Filter {

    @Inject
    public Authority_ByUser_Filter_PathList(LoginService loginService, GameService gameService, HttpListenerFactory httpListenerFactory) {
        super(loginService, gameService, httpListenerFactory);
    }

    @Override public Object doFilter(HttpRequest httpRequest, Method method, String url, HttpContext httpContext) {
        if (httpContext.getRequest().getUriPath().endsWith("/list")) {
            return checkUser(httpContext, url);
        }
        return null;
    }

}
