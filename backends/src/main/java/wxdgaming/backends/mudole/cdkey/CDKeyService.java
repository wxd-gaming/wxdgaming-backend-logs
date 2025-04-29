package wxdgaming.backends.mudole.cdkey;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.CDKeyEntity;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.CDKeyUtil;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.List;

/**
 * 服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-04-29 13:51
 **/
@Slf4j
@Singleton
@RequestMapping(path = "cdkey")
public class CDKeyService {

    final GameService gameService;

    @Inject
    public CDKeyService(GameService gameService) {
        this.gameService = gameService;
    }

    /** 生成cdkey */
    @HttpRequest(authority = 9)
    public RunResult addCDKey(@ThreadParam User user,
                              @Param(path = "gameId") int gameId,
                              @Param(path = "useType") int useType,
                              @Param(path = "useType") int useCount,
                              @Param(path = "rewards") String rewards) {

        List<CDKeyEntity.CDKeyReward> parseArray = FastJsonUtil.parseArray(rewards, CDKeyEntity.CDKeyReward.class);
        CDKeyEntity cdKeyEntity = new CDKeyEntity().setUseType(useType).setUseCount(useCount);
        cdKeyEntity.getRewards().addAll(parseArray);
        GameContext gameContext = gameService.gameContext(gameId);
        long l = gameContext.getDataHelper().tableCount(CDKeyEntity.class);
        cdKeyEntity.setUid((int) (l + 1));
        gameContext.getDataHelper().insert(cdKeyEntity);
        return RunResult.ok();
    }

    /** 生成cdkey */
    @HttpRequest(authority = 9)
    public RunResult gainCDKey(@Param(path = "id") int id, @Param(path = "num") int num) {
        List<String> strings = CDKeyUtil.cdKey(id, num);
        return RunResult.ok().data(strings);
    }

    /** 获取所有的cdkey */
    @HttpRequest(authority = 1)
    public RunResult list(HttpContext httpContext,
                          @ThreadParam GameContext gameContext,
                          @Param(path = "pageIndex") Integer pageIndex,
                          @Param(path = "pageSize") Integer pageSize) {
        PgsqlDataHelper dataHelper = gameContext.getDataHelper();
        SqlQueryBuilder sqlQueryBuilder = dataHelper.queryBuilder();
        sqlQueryBuilder.sqlByEntity(CDKeyEntity.class);
        sqlQueryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);
        long rowCount = sqlQueryBuilder.findCount();
        List<CDKeyEntity> cdKeyEntities = sqlQueryBuilder.findList2Entity(CDKeyEntity.class);
        return RunResult.ok().fluentPut("rowCount", rowCount).data(cdKeyEntities);
    }

}
