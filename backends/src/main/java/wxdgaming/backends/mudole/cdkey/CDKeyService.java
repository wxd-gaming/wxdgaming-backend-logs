package wxdgaming.backends.mudole.cdkey;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.CDKeyEntity;
import wxdgaming.backends.entity.games.CDKeyRecord;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.CDKeyUtil;
import wxdgaming.boot2.core.util.ObjectLockUtil;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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
