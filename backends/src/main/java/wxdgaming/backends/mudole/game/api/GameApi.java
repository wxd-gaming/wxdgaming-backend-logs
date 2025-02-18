package wxdgaming.backends.mudole.game.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.system.GameRecord;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.List;

/**
 * 游戏操作
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 13:43
 **/
@Slf4j
@Singleton
@RequestMapping(path = "/game")
public class GameApi {

    final PgsqlService pgsqlService;
    final GameService gameService;

    @Inject
    public GameApi(GameService gameService, PgsqlService pgsqlService) {
        this.gameService = gameService;
        this.pgsqlService = pgsqlService;
    }

    @HttpRequest
    public RunResult push(HttpContext session, @Body GameRecord gameRecord) {
        GameRecord queryEntity = pgsqlService.findById(GameRecord.class, gameRecord.getUid());
        if (queryEntity == null) {
            gameRecord.setCreateTime(System.currentTimeMillis());
            gameRecord.setAppToken(StringUtils.randomString(12));
            gameRecord.setRechargeToken(StringUtils.randomString(18));
            gameRecord.setLogToken(StringUtils.randomString(32));
            pgsqlService.insert(gameRecord);
            gameService.addGameCache(gameRecord);
        } else {
            gameRecord.setUid(queryEntity.getUid());
            gameRecord.setCreateTime(Math.min(gameRecord.getCreateTime(), queryEntity.getCreateTime()));
            gameRecord.setTableMapping(queryEntity.getTableMapping());
            gameRecord.setAppToken(queryEntity.getAppToken());
            gameRecord.setRechargeToken(queryEntity.getRechargeToken());
            gameRecord.setLogToken(queryEntity.getLogToken());
            pgsqlService.update(gameRecord);
            gameService.addGameCache(gameRecord);
        }
        return RunResult.ok();
    }

    @HttpRequest
    public RunResult menu(HttpContext session) {
        List<JSONObject> list = gameService.getGameId2GameRecordMap().values().stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("createTime")));
                    jsonObject.entrySet().removeIf(v -> v.getKey().toLowerCase().endsWith("token"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", list.size());
    }

    @HttpRequest
    public RunResult list(HttpContext session) {
        List<JSONObject> list = gameService.getGameId2GameRecordMap().values().stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("createTime")));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", list.size());
    }

    @HttpRequest
    public RunResult addLogType(HttpContext session, JSONObject data) {
        Integer gameId = data.getInteger("gameId");
        String token = data.getString("token");
        RunResult runResult = gameService.checkAppToken(gameId, token);
        if (runResult != null) return runResult;

        GameRecord gameRecord = gameService.getGameId2GameRecordMap().get(gameId);

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
        if (pgsqlDataHelper == null) {
            return RunResult.error("gameId is not exist");
        }
        if (!gameRecord.getTableMapping().containsKey(data.getString("logType"))) {
            gameService.checkSLogTable(pgsqlDataHelper, data.getString("logType"));
            gameRecord.getTableMapping().put(data.getString("logType"), data.getString("logComment"));
            this.pgsqlService.update(gameRecord);
        }
        return RunResult.ok();
    }

    @HttpRequest
    public RunResult listLogType(HttpContext session, JSONObject data) {
        Integer gameId = data.getInteger("gameId");
        String token = data.getString("token");
        RunResult runResult = gameService.checkAppToken(gameId, token);
        if (runResult != null) return runResult;

        GameRecord gameRecord = gameService.getGameId2GameRecordMap().get(gameId);

        return RunResult.ok().data(gameRecord.getTableMapping());
    }


}
