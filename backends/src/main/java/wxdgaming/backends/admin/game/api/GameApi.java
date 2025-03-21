package wxdgaming.backends.admin.game.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.AdminService;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.SRoleLog;
import wxdgaming.backends.entity.games.logs.SServerLog;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.io.FileUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.lang.Tuple2;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    final AdminService adminService;

    @Inject
    public GameApi(GameService gameService, PgsqlService pgsqlService, AdminService adminService) {
        this.gameService = gameService;
        this.pgsqlService = pgsqlService;
        this.adminService = adminService;
    }

    @HttpRequest(authority = 9, comment = "添加游戏")
    public RunResult add(HttpContext session, @ThreadParam() User user, @Param(path = "gameName") String gameName) {
        if (!user.isAdmin()) return RunResult.error("权限不足");
        int newGameId = gameService.newGameId();
        Game game = new Game();
        game.setUid(newGameId);
        game.setName(gameName);
        if (game.getCreateTime() == 0) {
            game.setCreateTime(System.currentTimeMillis());
        }
        game.setAppToken(StringUtils.randomString(16));
        game.setRechargeToken(StringUtils.randomString(16));
        game.setLogToken(StringUtils.randomString(16));
        pgsqlService.insert(game);
        Thread.ofPlatform().start(() -> gameService.addGameCache(game));
        return RunResult.ok();
    }

    @HttpRequest(authority = 9, comment = "添加游戏")
    public RunResult push(HttpContext session, @ThreadParam() User user, @Body Game game) {
        if (!user.isAdmin()) return RunResult.error("权限不足");
        GameContext gameContext = null;
        if (game.getUid() > 0) {
            gameContext = gameService.gameContext(game.getUid());
        }

        if (gameContext == null) {
            int newGameId = gameService.newGameId();
            game.setUid(newGameId);
            if (game.getCreateTime() == 0) {
                game.setCreateTime(System.currentTimeMillis());
            }
            game.setAppToken(StringUtils.randomString(16));
            game.setRechargeToken(StringUtils.randomString(16));
            game.setLogToken(StringUtils.randomString(16));
            pgsqlService.insert(game);
            Thread.ofPlatform().start(() -> gameService.addGameCache(game));
        } else {
            Game queryEntity = gameContext.getGame();
            queryEntity.getRoleTableMapping().putAll(game.getRoleTableMapping());
            queryEntity.getServerTableMapping().putAll(game.getServerTableMapping());
            queryEntity.setAppToken(game.getAppToken());
            queryEntity.setRechargeToken(game.getRechargeToken());
            queryEntity.setLogToken(game.getLogToken());
            Thread.ofPlatform().start(() -> gameService.addGameCache(game));
        }
        user.getAuthorizationGames().add(game.getUid());
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

    @HttpRequest(authority = 10)
    public RunResult truncates(HttpContext session, @ThreadParam() User user, @ThreadParam GameContext gameContext) {

        log.info(
                "管理员：{} 清档 游戏 {}-{}",
                user.getAccount(),
                gameContext.getGame().getUid(), gameContext.getGame().getName()
        );

        gameContext.submit(() -> {
            gameContext.getLogKeyCache().discardAll();
            gameContext.getRoleRecordJdbcCache().discardAll();
            gameContext.getAccountRecordJdbcCache().discardAll();
            gameContext.getServerRecordMap().clear();
            gameContext.getDataHelper().truncates();
        });

        return RunResult.OK;
    }

    @HttpRequest(authority = 10)
    public RunResult refreshKey(HttpContext httpContext,
                                @ThreadParam() User user,
                                @ThreadParam GameContext gameContext,
                                @Param(path = "type") String type) {

        log.info(
                "管理员：{} 刷新 游戏 {}-{} {} key",
                user.getAccount(),
                gameContext.getGame().getUid(), gameContext.getGame().getName(),
                type
        );

        switch (type) {
            case "app" -> gameContext.getGame().setAppToken(StringUtils.randomString(16));
            case "log" -> gameContext.getGame().setLogToken(StringUtils.randomString(16));
            case "recharge" -> gameContext.getGame().setRechargeToken(StringUtils.randomString(16));
        }


        return RunResult.OK;
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
                .filter(game -> user.isRoot() || user.isAllGame() || user.getAuthorizationGames().contains(game.getUid()))
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

    @HttpRequest(authority = 9, comment = "游戏列表")
    public RunResult menu(HttpContext session, @ThreadParam() User user) {
        List<JSONObject> list = gameService.getGameContextHashMap().values()
                .stream()
                .sorted(Comparator.comparingInt(GameContext::getGameId))
                .map(GameContext::getGame)
                .filter(game -> user.isRoot() || user.isAllGame() || user.getAuthorizationGames().contains(game.getUid()))
                .map(game -> {
                    JSONObject jsonObject = game.toJSONObject();
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("createTime")));
                    return jsonObject;
                })
                .collect(Collectors.toList());

        List<String> routings = new ArrayList<>();

        Stream<Tuple2<Path, byte[]>> htmlStream = FileUtil.resourceStreams("html", ".html");
        htmlStream.forEach(tuple2 -> {
            Path left = tuple2.getLeft();
            String pathString = left.toString();
            int indexOf = pathString.indexOf("html" + File.separator);
            if (indexOf < 0) {
                return;
            }
            pathString = "/" + pathString.substring(indexOf + 5);
            pathString = pathString.replace("\\", "/");
            if ((user.isRoot() || user.getAuthorizationRouting().contains(pathString))) {
                routings.add(pathString);
            }
        });

        return RunResult.ok().fluentPut("data", list).fluentPut("routings", routings).fluentPut("admin", user.isRoot() || user.isAdmin());
    }

    @HttpRequest(authority = 1, comment = "添加日志表")
    public RunResult addRoleLogType(HttpContext session, @ThreadParam GameContext gameContext, @Body JSONObject data) {
        Game game = gameContext.getGame();
        if (!game.getRoleTableMapping().containsKey(data.getString("logType"))) {
            gameService.checkSLogTable(gameContext, gameContext.getDataHelper(), SRoleLog.class, data.getString("logType"), data.getString("logComment"));
            game.getRoleTableMapping().put(data.getString("logType"), data.getString("logComment"));
            this.pgsqlService.update(game);
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 1, comment = "日志列表")
    public RunResult roleLogTypeList(HttpContext session, @ThreadParam GameContext gameContext) {
        Game game = gameContext.getGame();
        return RunResult.ok().data(game.getRoleTableMapping());
    }

    @HttpRequest(authority = 1, comment = "添加日志表")
    public RunResult addServerLogType(HttpContext session, @ThreadParam GameContext gameContext, @Body JSONObject data) {
        Game game = gameContext.getGame();
        if (!game.getServerTableMapping().containsKey(data.getString("logType"))) {
            gameService.checkSLogTable(gameContext, gameContext.getDataHelper(), SServerLog.class, data.getString("logType"), data.getString("logComment"));
            game.getServerTableMapping().put(data.getString("logType"), data.getString("logComment"));
            this.pgsqlService.update(game);
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 1, comment = "日志列表")
    public RunResult serverLogTypeList(HttpContext session, @ThreadParam GameContext gameContext) {
        Game game = gameContext.getGame();
        return RunResult.ok().data(game.getServerTableMapping());
    }


}
