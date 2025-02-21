package wxdgaming.backends.admin.game.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.ann.Param;
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

    @HttpRequest(authority = 9)
    public RunResult push(HttpContext session, @Body Game game) {
        Game queryEntity = gameService.gameRecord(game.getUid());
        if (queryEntity == null) {
            game.setCreateTime(System.currentTimeMillis());
            game.setAppToken(StringUtils.randomString(12));
            game.setRechargeToken(StringUtils.randomString(18));
            game.setLogToken(StringUtils.randomString(32));
            pgsqlService.insert(game);
            gameService.addGameCache(game);
        } else {
            game.setUid(queryEntity.getUid());
            game.setCreateTime(queryEntity.getCreateTime());
            game.setTableMapping(queryEntity.getTableMapping());
            game.setAppToken(queryEntity.getAppToken());
            game.setRechargeToken(queryEntity.getRechargeToken());
            game.setLogToken(queryEntity.getLogToken());
            pgsqlService.update(game);
            gameService.addGameCache(game);
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    public RunResult find(HttpContext session, @Param(path = "gameId") int gameId) {
        Game entity = gameService.gameRecord(gameId);
        return RunResult.ok().data(entity);
    }

    @HttpRequest(authority = 9)
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

    @HttpRequest(authority = 9)
    public RunResult list(HttpContext session) {
        List<JSONObject> list = gameService.getGameId2GameRecordMap().values().stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("createTime")));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", list.size());
    }

    @HttpRequest(authority = 9)
    public RunResult addLogType(HttpContext session, JSONObject data) {
        Integer gameId = data.getInteger("gameId");
        String token = data.getString("token");
        RunResult runResult = gameService.checkAppToken(gameId, token);
        if (runResult != null) return runResult;

        Game game = gameService.getGameId2GameRecordMap().get(gameId);

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
        if (pgsqlDataHelper == null) {
            return RunResult.error("gameId is not exist");
        }
        if (!game.getTableMapping().containsKey(data.getString("logType"))) {
            gameService.checkSLogTable(pgsqlDataHelper, data.getString("logType"), data.getString("logComment"));
            game.getTableMapping().put(data.getString("logType"), data.getString("logComment"));
            this.pgsqlService.update(game);
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    public RunResult listLogType(HttpContext session, JSONObject data) {
        Integer gameId = data.getInteger("gameId");
        String token = data.getString("token");
        RunResult runResult = gameService.checkAppToken(gameId, token);
        if (runResult != null) return runResult;

        Game game = gameService.getGameId2GameRecordMap().get(gameId);

        return RunResult.ok().data(game.getTableMapping());
    }


}
