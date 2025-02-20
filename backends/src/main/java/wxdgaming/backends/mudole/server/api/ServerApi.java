package wxdgaming.backends.mudole.server.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.ServerRecord;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.List;

/**
 * 区服接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:22
 **/
@Slf4j
@Singleton
@RequestMapping(path = "/server")
public class ServerApi {

    final GameService gameService;

    @Inject
    public ServerApi(GameService gameService) {
        this.gameService = gameService;
    }

    @HttpRequest(authority = 2)
    public RunResult push(HttpContext httpContext,
                          @Param(path = "gameId") int gameId,
                          @Body ServerRecord serverRecord) {
        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
        ServerRecord queryEntity = pgsqlDataHelper.findByWhere(ServerRecord.class, "sid=?", serverRecord.getSid());
        serverRecord.setUpdateTime(System.currentTimeMillis());
        if (queryEntity == null) {
            serverRecord.setUid(gameService.newId(gameId));
            pgsqlDataHelper.getSqlDataBatch().insert(serverRecord);
        } else {
            serverRecord.setUid(queryEntity.getUid());
            pgsqlDataHelper.getSqlDataBatch().update(serverRecord);
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    public RunResult list(HttpContext httpContext, @Param(path = "gameId") int gameId) {
        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
        List<JSONObject> list = pgsqlDataHelper
                .findList(ServerRecord.class)
                .stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("logTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("logTime")));
                    jsonObject.put("updateTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("updateTime")));
                })
                .toList();

        return RunResult.ok().data(list).fluentPut("rowCount", list.size());
    }

}
