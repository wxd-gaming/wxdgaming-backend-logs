package wxdgaming.backends.mudole.account.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
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
@RequestMapping(path = "account")
public class AccountApi {

    final GameService gameService;
    final SLogService SLogService;

    @Inject
    public AccountApi(GameService gameService, SLogService SLogService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
    }

    @HttpRequest(authority = 2)
    public RunResult push(HttpContext httpContext, @Body AccountRecord accountRecord) {
        if (accountRecord.getUid() == 0) {
            accountRecord.setUid(gameService.newId(accountRecord.getGameId()));
        }
        if (accountRecord.getCreateTime() == 0) {
            accountRecord.setCreateTime(System.currentTimeMillis());
        }
        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(accountRecord.getGameId());
        AccountRecord entity = pgsqlDataHelper.findByWhere(AccountRecord.class, "account = ?", accountRecord.getAccount());
        if (entity == null) {
            accountRecord.checkDataKey();
            pgsqlDataHelper.dataBatch().insert(accountRecord);
        } else {
            accountRecord.setUid(entity.getUid());
            accountRecord.setCreateTime(entity.getCreateTime());
            accountRecord.setDayKey(entity.getDayKey());
            pgsqlDataHelper.dataBatch().update(accountRecord);
        }
        accountRecord.setToken("");
        accountRecord.setGameId(0);
        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    public RunResult list(HttpContext httpContext,
                          @Param(path = "gameId") Integer gameId,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "account", required = false) String account) {
        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);

        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();
        queryBuilder
                .sqlByEntity(AccountRecord.class)
                .pushWhereByValueNotNull("account=?", account)
        ;

        queryBuilder.setOrderBy("createtime desc");

        if (pageIndex > 0) {
            queryBuilder.setSkip((pageIndex - 1) * pageSize);
        }

        if (pageSize <= 10) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;
        queryBuilder.setLimit(pageSize);

        long rowCount = queryBuilder.findCount();
        List<AccountRecord> records = queryBuilder.findList2Entity(AccountRecord.class);

        List<JSONObject> list = records.stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("lastJoinTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("lastJoinTime")));
                    jsonObject.put("data", jsonObject.getString("data"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }
}
