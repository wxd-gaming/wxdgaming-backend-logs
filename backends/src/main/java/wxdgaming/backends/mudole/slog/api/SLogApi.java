package wxdgaming.backends.mudole.slog.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.SLog;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ExecutorWith;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.List;

/**
 * 日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 16:54
 **/
@Slf4j
@Singleton
@RequestMapping(path = "log")
public class SLogApi {

    final GameService gameService;
    final SLogService SLogService;

    @Inject
    public SLogApi(GameService gameService, SLogService SLogService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
    }

    @HttpRequest(authority = 2)
    @ExecutorWith(useVirtualThread = true)
    public RunResult push(HttpContext httpContext, @Body SLog sLog) {
        log.info("sLog - {}", sLog.toJsonString());

        PgsqlDataHelper pgsqlDataHelper = this.gameService.pgsqlDataHelper(sLog.getGameId());

        if (sLog.getUid() == 0)
            sLog.setUid(gameService.newId(sLog.getGameId()));

        sLog.checkDataKey();

        pgsqlDataHelper.getSqlDataBatch().insert(sLog);
        // pgsqlDataHelper.getSqlDataBatch().insert(sLog);
        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    @ExecutorWith(useVirtualThread = true)
    public RunResult list(HttpContext httpSession,
                          @Param(path = "gameId") Integer gameId,
                          @Param(path = "logType") String logType,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName,
                          @Param(path = "dataJson", required = false) String dataJson) {

        Game game = gameService.getGameId2GameRecordMap().get(gameId);

        if (game == null) {
            return RunResult.error("gameId error");
        }

        if (!game.getTableMapping().containsKey(logType)) {
            return RunResult.error("log type error");
        }
        PgsqlDataHelper pgsqlDataHelper = this.gameService.pgsqlDataHelper(gameId);
        SqlQueryBuilder sqlQueryBuilder = pgsqlDataHelper.queryBuilder();

        sqlQueryBuilder
                .setTableName(logType)
                .pushWhereByValueNotNull("account=?", account)
                .pushWhereByValueNotNull("roleid=?", roleId)
                .pushWhereByValueNotNull("rolename=?", roleName);

        if (StringUtils.isNotBlank(dataJson)) {
            String[] split = dataJson.split(",");
            for (String s : split) {
                String[] strings = s.split("=");
                sqlQueryBuilder.pushWhere("json_extract_path_text(data,'" + strings[0] + "') = ?", strings[1]);
            }
        }

        sqlQueryBuilder.setOrderBy("createtime desc");

        if (pageIndex > 0) {
            sqlQueryBuilder.setSkip((pageIndex - 1) * pageSize);
        }

        if (pageSize <= 10) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;

        sqlQueryBuilder.setLimit(pageSize);

        long rowCount = pgsqlDataHelper.tableCount(logType, sqlQueryBuilder.getWhere(), sqlQueryBuilder.getParameters());

        List<SLog> slogs = pgsqlDataHelper.findListBySql(SLog.class, sqlQueryBuilder.buildSelectSql(), sqlQueryBuilder.getParameters());

        List<JSONObject> list = slogs.stream()
                .map(SLog::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("data", jsonObject.getString("data"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }

}
