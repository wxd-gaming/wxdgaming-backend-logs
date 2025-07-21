package wxdgaming.backends.mudole.cdkey;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;

/**
 * 服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-04-29 13:51
 **/
@Slf4j
@Singleton
public class CDKeyService {

    final GameService gameService;

    @Inject
    public CDKeyService(GameService gameService) {
        this.gameService = gameService;
    }


}
