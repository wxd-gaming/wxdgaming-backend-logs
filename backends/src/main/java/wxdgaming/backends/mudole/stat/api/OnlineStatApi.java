package wxdgaming.backends.mudole.stat.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.GameStat;
import wxdgaming.backends.entity.games.OnlineStat;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.List;

/**
 * 账号api
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 15:52
 **/
@Slf4j
@Singleton
@RequestMapping(path = "stat/online")
public class OnlineStatApi {

    final GameService gameService;

    @Inject
    public OnlineStatApi(GameService gameService) {
        this.gameService = gameService;
    }


    @HttpRequest(authority = 9)
    public RunResult list(HttpContext httpContext, @Param(path = "gameId") int gameId,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "day", required = false) String day) {

        PgsqlDataHelper pgsqlDataHelper = gameService.gameContext(gameId).getDataHelper();
        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();
        queryBuilder.sqlByEntity(OnlineStat.class);
        queryBuilder.setOrderBy("uid desc");
        if (StringUtils.isNotBlank(day)) {
            queryBuilder.pushWhereByValueNotNull("uid=?", Integer.parseInt(day));
        }

        long rowCount = queryBuilder.findCount();

        queryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);

        List<JSONObject> list = queryBuilder.findList2Entity(OnlineStat.class)
                .stream()
                .map(OnlineStat::toJSONObject)
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }
}
