package wxdgaming.backends.admin.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.http.HttpContext;
import wxdgaming.boot2.starter.net.server.http.HttpFilter;

import java.lang.reflect.Method;

/**
 * 权限过滤器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-19 15:02
 **/
@Slf4j
@Singleton
public class Authority_1_AppToken_Filter extends HttpFilter {

    final GameService gameService;

    @Inject
    public Authority_1_AppToken_Filter(GameService gameService) {
        this.gameService = gameService;
    }


    @Override public Object doFilter(HttpRequest httpRequest, Method method, String url, HttpContext httpContext) {
        if (httpRequest != null && Objects.checkHave(httpRequest.authority(), 1)) {
            Integer gameId = httpContext.getRequest().getReqParams().getInteger("gameId");
            if (gameId == null) {
                return RunResult.error("参数gameId不能为空");
            }
            String token = httpContext.getRequest().getReqParams().getString("token");
            if (token == null) {
                return RunResult.error("参数token不能为空");
            }
            Game game = gameService.gameRecord(gameId);
            if (game == null) {
                return RunResult.error("gameId is not exist");
            }
            if (!Objects.equals(game.getAppToken(), token)) {
                return RunResult.error("game log token error");
            }
        }
        return null;
    }

}
