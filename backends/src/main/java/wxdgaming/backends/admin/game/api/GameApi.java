package wxdgaming.backends.admin.game.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.Comparator;
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

    @HttpRequest(authority = 9, comment = "添加游戏")
    public RunResult push(HttpContext session, @Body Game game) {
        GameContext gameContext = gameService.gameContext(game.getUid());
        if (gameContext == null) {
            game.setCreateTime(System.currentTimeMillis());
            game.setAppToken(StringUtils.randomString(12));
            game.setRechargeToken(StringUtils.randomString(18));
            game.setLogToken(StringUtils.randomString(32));
            pgsqlService.insert(game);
            gameService.addGameCache(game);
        } else {
            Game queryEntity = gameContext.getGame();
            queryEntity.getTableMapping().putAll(game.getTableMapping());
            queryEntity.setAppToken(game.getAppToken());
            queryEntity.setRechargeToken(game.getRechargeToken());
            queryEntity.setLogToken(game.getLogToken());
            gameService.addGameCache(game);
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    public RunResult find(HttpContext session, @Param(path = "gameId") int gameId) {
        GameContext gameContext = gameService.gameContext(gameId);
        if (gameContext == null) {
            return RunResult.error("gameId is not exist");
        }
        return RunResult.ok().data(gameContext.getGame());
    }

    @HttpRequest(authority = 9, comment = "游戏列表")
    public RunResult list(HttpContext session,
                          @ThreadParam() User user,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize) {

        int skip = 0;
        if (pageIndex > 0) {
            skip = ((pageIndex - 1) * pageSize);
        }

        if (pageSize <= 10) pageSize = 10;
        if (pageSize > 100000) pageSize = 100000;

        List<JSONObject> list = gameService.getGameContextHashMap().values()
                .stream()
                .sorted(Comparator.comparingInt(GameContext::getGameId))
                .map(GameContext::getGame)
                .filter(game -> user.isRoot() || user.getAuthorizationGames().contains(game.getUid()))
                .skip(skip)
                .limit(pageSize)
                .map(game -> {
                    JSONObject jsonObject = game.toJSONObject();
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("createTime")));
                    return jsonObject;
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", list.size());
    }

    @HttpRequest(authority = 9, comment = "添加日志表")
    public RunResult addLogType(HttpContext session, JSONObject data) {
        Integer gameId = data.getInteger("gameId");
        String token = data.getString("token");
        RunResult runResult = gameService.checkAppToken(gameId, token);
        if (runResult != null) return runResult;

        GameContext gameContext = gameService.gameContext(gameId);

        Game game = gameContext.getGame();

        if (!game.getTableMapping().containsKey(data.getString("logType"))) {
            gameService.checkSLogTable(gameContext.getDataHelper(), data.getString("logType"), data.getString("logComment"));
            game.getTableMapping().put(data.getString("logType"), data.getString("logComment"));
            this.pgsqlService.update(game);
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 9, comment = "日志列表")
    public RunResult listLogType(HttpContext session, JSONObject data) {
        Integer gameId = data.getInteger("gameId");
        String token = data.getString("token");
        RunResult runResult = gameService.checkAppToken(gameId, token);
        if (runResult != null) return runResult;

        Game game = gameService.gameContext(gameId).getGame();

        return RunResult.ok().data(game.getTableMapping());
    }


}
