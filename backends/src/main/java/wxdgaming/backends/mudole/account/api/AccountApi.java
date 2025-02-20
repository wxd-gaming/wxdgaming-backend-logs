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
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
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
        if (accountRecord.getGameId() == 0) return RunResult.error("gameId is null");
        if (StringUtils.isBlank(accountRecord.getToken())) return RunResult.error("token is null");
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
            pgsqlDataHelper.insert(accountRecord);
        } else {
            accountRecord.setUid(entity.getUid());
            accountRecord.setCreateTime(entity.getCreateTime());
            accountRecord.setDayKey(entity.getDayKey());
            pgsqlDataHelper.update(accountRecord);
        }
        accountRecord.setToken("");
        accountRecord.setGameId(0);
        return RunResult.ok().data(accountRecord);
    }

    @HttpRequest(authority = 9)
    public RunResult list(HttpContext httpContext,
                          @Param(path = "gameId") Integer gameId,
                          @Param(path = "account", required = false) String account) {
        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);

        List<AccountRecord> accountRecords;
        if (StringUtils.isBlank(account)) {
            accountRecords = pgsqlDataHelper.findList(AccountRecord.class);
        } else {
            accountRecords = pgsqlDataHelper.findListByWhere(AccountRecord.class, "account = ?", account);
        }
        List<JSONObject> list = accountRecords.stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("lastJoinTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("lastJoinTime")));
                    jsonObject.put("data", jsonObject.getString("data"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", list.size());
    }
}
