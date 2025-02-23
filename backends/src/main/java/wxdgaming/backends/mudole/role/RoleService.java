package wxdgaming.backends.mudole.role;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;

/**
 * 角色服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:38
 **/
@Slf4j
@Singleton
public class RoleService {

    final GameService gameService;

    @Inject
    public RoleService(GameService gameService) {
        this.gameService = gameService;
    }


}
