package wxdgaming.backends.mudole.server.api;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.ServerRecord;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.server.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

/**
 * 区服接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:22
 **/
@Slf4j
@RequestMapping(path = "/server")
public class ServerApi {

    final GameService gameService;

    @Inject
    public ServerApi(GameService gameService) {
        this.gameService = gameService;
    }

    @HttpRequest
    public RunResult push(HttpContext httpContext,
                          @Param("gameId") Integer gameId,
                          @Param("serverRecord") ServerRecord serverRecord) {

        return null;
    }

    @HttpRequest
    public RunResult list(HttpContext httpContext, @Param("gameId") Integer gameId) {

        return null;
    }

}
