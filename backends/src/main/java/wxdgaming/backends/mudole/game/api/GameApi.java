package wxdgaming.backends.mudole.game.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.system.GameRecord;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.boot.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot.core.lang.RunResult;
import wxdgaming.boot.core.str.StringUtil;
import wxdgaming.boot.core.str.json.FastJsonUtil;
import wxdgaming.boot.core.timer.MyClock;
import wxdgaming.boot.net.controller.ann.Body;
import wxdgaming.boot.net.controller.ann.TextController;
import wxdgaming.boot.net.controller.ann.TextMapping;
import wxdgaming.boot.net.web.hs.HttpSession;
import wxdgaming.boot.starter.pgsql.PgsqlService;

import java.util.List;

/**
 * 游戏操作
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 13:43
 **/
@Slf4j
@Singleton
@TextController(path = "game")
public class GameApi {

    final PgsqlService pgsqlService;
    final GameService gameService;

    @Inject
    public GameApi(GameService gameService, PgsqlService pgsqlService) {
        this.gameService = gameService;
        this.pgsqlService = pgsqlService;
    }

    @TextMapping
    public RunResult push(HttpSession session, @Body GameRecord gameRecord) {
        GameRecord queryEntity = pgsqlService.queryEntity(GameRecord.class, gameRecord.getUid());
        if (queryEntity == null) {
            gameRecord.setCreateTime(System.currentTimeMillis());
            gameRecord.setAppToken(StringUtil.getRandomString(12));
            gameRecord.setRechargeToken(StringUtil.getRandomString(18));
            gameRecord.setLogToken(StringUtil.getRandomString(32));
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

    @TextMapping
    public RunResult menu(HttpSession session) {
        List<JSONObject> list = gameService.getGameId2GameRecordMap().values().stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("createTime")));
                    jsonObject.entrySet().removeIf(v -> v.getKey().toLowerCase().endsWith("token"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("length", list.size());
    }

    @TextMapping
    public RunResult list(HttpSession session) {
        List<JSONObject> list = gameService.getGameId2GameRecordMap().values().stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("createTime")));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("length", list.size());
    }

    @TextMapping
    public RunResult addLogType(HttpSession session, JSONObject data) {
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

    @TextMapping
    public RunResult listLogType(HttpSession session, JSONObject data) {
        Integer gameId = data.getInteger("gameId");
        String token = data.getString("token");
        RunResult runResult = gameService.checkAppToken(gameId, token);
        if (runResult != null) return runResult;

        GameRecord gameRecord = gameService.getGameId2GameRecordMap().get(gameId);

        return RunResult.ok().data(gameRecord.getTableMapping());
    }


}
