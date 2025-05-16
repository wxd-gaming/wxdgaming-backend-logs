package wxdgaming.backends.admin.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.admin.login.LoginService;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.executor.ThreadContext;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.http.HttpContext;
import wxdgaming.boot2.starter.net.server.http.HttpFilter;
import wxdgaming.boot2.starter.net.server.http.HttpListenerFactory;
import wxdgaming.boot2.starter.net.server.http.HttpMapping;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * 检查登陆权限
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-19 15:02
 **/
@Slf4j
@Singleton
public class Authority_ByUser_Filter extends HttpFilter {

    final LoginService loginService;
    final GameService gameService;
    final HttpListenerFactory httpListenerFactory;

    @Inject
    public Authority_ByUser_Filter(LoginService loginService, GameService gameService, HttpListenerFactory httpListenerFactory) {
        this.loginService = loginService;
        this.gameService = gameService;
        this.httpListenerFactory = httpListenerFactory;
    }

    @Override public Object doFilter(HttpRequest httpRequest, Method method, String url, HttpContext httpContext) {
        if (httpRequest != null && Objects.checkHave(httpRequest.authority(), 9)) {
            return checkUser(httpContext, url);
        }
        return null;
    }

    public RunResult checkUser(HttpContext httpContext, String url) {
        RunResult runResult = loginService.checkLogin(httpContext);
        if (runResult.code() != 1) {
            return runResult;
        }
        int gameId = httpContext.getRequest().getReqParams().getIntValue("gameId");
        try {
            User user = ThreadContext.context("user");
            if (user.isRoot()) return null;

            if (gameId > 0) {
                if (!user.checkAuthorGame(gameId)) {
                    return RunResult.error("权限不足");
                }
            }
            HashMap<String, HttpMapping> httpMappingMap = httpListenerFactory.getHttpListenerContent().getHttpMappingMap();
            HttpMapping httpMapping = httpMappingMap.get(url);
            if (httpMapping == null)
                return RunResult.error("权限不足");

            if (httpMapping.path().startsWith("/log/") || httpMapping.path().startsWith("log/")) {
                return null;
            }

            if (httpMapping.httpRequest().authority().length == 0) {
                return null;
            }

            if (!user.checkAuthorRouting(url)) {
                return RunResult.error("权限不足");
            }
        } finally {
            if (gameId > 0) {
                GameContext gameContext = gameService.gameContext(gameId);
                ThreadContext.putContent(gameContext);
            }
        }
        return null;
    }

}
