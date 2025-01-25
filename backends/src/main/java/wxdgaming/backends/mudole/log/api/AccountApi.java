package wxdgaming.backends.mudole.log.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.AccountRecord;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.backends.mudole.log.LogsService;
import wxdgaming.boot.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot.core.lang.RunResult;
import wxdgaming.boot.core.str.StringUtil;
import wxdgaming.boot.core.str.json.FastJsonUtil;
import wxdgaming.boot.core.timer.MyClock;
import wxdgaming.boot.net.controller.ann.Body;
import wxdgaming.boot.net.controller.ann.Param;
import wxdgaming.boot.net.controller.ann.TextController;
import wxdgaming.boot.net.controller.ann.TextMapping;
import wxdgaming.boot.net.web.hs.HttpSession;

import java.util.List;

/**
 * 账号api
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 15:52
 **/
@Slf4j
@Singleton
@TextController(path = "account")
public class AccountApi {

    final GameService gameService;
    final LogsService logsService;

    @Inject
    public AccountApi(GameService gameService, LogsService logsService) {
        this.gameService = gameService;
        this.logsService = logsService;
    }

    @TextMapping
    public RunResult push(HttpSession httpSession, @Body AccountRecord accountRecord) {
        if (accountRecord.getGameId() == 0) return RunResult.error("gameId is null");
        if (StringUtil.emptyOrNull(accountRecord.getToken())) return RunResult.error("token is null");
        if (accountRecord.getUid() == 0) {
            accountRecord.setUid(gameService.newId(accountRecord.getGameId()));
        }
        if (accountRecord.getCreateTime() == 0) {
            accountRecord.setCreateTime(System.currentTimeMillis());
        }

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(accountRecord.getGameId());
        AccountRecord entity = pgsqlDataHelper.queryEntityByWhere(AccountRecord.class, "account = ?", accountRecord.getAccount());
        if (entity == null) {
            pgsqlDataHelper.insert(accountRecord);
        } else {
            accountRecord.setUid(entity.getUid());
            accountRecord.setCreateTime(Math.min(accountRecord.getCreateTime(), entity.getCreateTime()));
            pgsqlDataHelper.update(accountRecord);
        }
        return RunResult.ok();
    }

    @TextMapping
    public RunResult list(HttpSession httpSession,
                          @Param("gameId") Integer gameId,
                          @Param(value = "search", required = false) String search) {
        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
        List<AccountRecord> accountRecords;
        if (StringUtil.emptyOrNull(search)) {
            accountRecords = pgsqlDataHelper.queryEntities(AccountRecord.class);
        } else {
            accountRecords = pgsqlDataHelper.queryEntitiesWhere(AccountRecord.class, "account = ?", search);
        }
        List<JSONObject> list = accountRecords.stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("createTime")));
                    jsonObject.put("lastJoinTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("lastJoinTime")));
                    jsonObject.put("data", jsonObject.getString("data"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("length", list.size());
    }
}
