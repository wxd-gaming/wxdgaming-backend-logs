package wxdgaming.backends.admin.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.admin.login.LoginService;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ThreadContext;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.http.HttpContext;
import wxdgaming.boot2.starter.net.server.http.HttpListenerFactory;

import java.lang.reflect.Method;

/**
 * 检查登陆权限
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-19 15:02
 **/
@Slf4j
@Singleton
public class Authority_ByUser_Filter_10_Admin extends Authority_ByUser_Filter {

    @Inject
    public Authority_ByUser_Filter_10_Admin(LoginService loginService, GameService gameService, HttpListenerFactory httpListenerFactory) {
        super(loginService, gameService, httpListenerFactory);
    }

    @Override public Object doFilter(HttpRequest httpRequest, Method method, String url, HttpContext httpContext) {
        if (httpRequest != null && Objects.checkHave(httpRequest.authority(), 10)) {
            Object object = checkUser(httpContext, url);
            if (object != null) {
                return object;
            }
            User context = ThreadContext.context(User.class);
            if (context.isAdmin() & context.isRoot()) {
                return null;
            }
            return RunResult.error("权限不足");
        }
        return null;
    }


}
