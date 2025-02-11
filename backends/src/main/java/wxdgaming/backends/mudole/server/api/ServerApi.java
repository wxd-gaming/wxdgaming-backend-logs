package wxdgaming.backends.mudole.server.api;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.boot.core.lang.RunResult;
import wxdgaming.boot.net.controller.ann.Param;
import wxdgaming.boot.net.controller.ann.TextController;
import wxdgaming.boot.net.web.hs.HttpSession;

/**
 * 区服接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:22
 **/
@Slf4j
@TextController(path = "/server")
public class ServerApi {

    final GameService gameService;

    @Inject
    public ServerApi(GameService gameService) {
        this.gameService = gameService;
    }


    public RunResult list(HttpSession httpSession, @Param("gameId") Integer gameId) {

        return null;
    }

}
