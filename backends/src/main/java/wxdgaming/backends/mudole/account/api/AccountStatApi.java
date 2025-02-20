package wxdgaming.backends.mudole.account.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.AccountStat;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.lang.RunResult;
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
@RequestMapping(path = "account/stat")
public class AccountStatApi {

    final GameService gameService;

    @Inject
    public AccountStatApi(GameService gameService) {
        this.gameService = gameService;
    }


    @HttpRequest(authority = 9)
    public RunResult list(HttpContext httpContext, @Param(path = "gameId") int gameId,
                          @Param(path = "pageIndex") Integer pageIndex,
                          @Param(path = "pageSize") Integer pageSize) {
        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
        String tableName = pgsqlDataHelper.tableMapping(AccountStat.class).getTableName();
        String sql = "select * from " + tableName + " order by uid desc ";
        long rowCount = pgsqlDataHelper.tableCount(AccountStat.class);

        if (pageIndex != null && pageIndex > 0) {
            sql += " offset " + (pageIndex - 1) * pageSize;
        }

        if (pageSize == null || pageSize <= 10) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;
        sql += " limit " + pageSize;


        List<JSONObject> list = pgsqlDataHelper.findListBySql(AccountStat.class, sql)
                .stream()
                .map(AccountStat::toJSONObject)
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }
}
