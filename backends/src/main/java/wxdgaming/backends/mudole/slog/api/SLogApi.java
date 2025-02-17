package wxdgaming.backends.mudole.slog.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.SLog;
import wxdgaming.backends.entity.system.GameRecord;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ThreadInfo;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.server.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.time.LocalDate;
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

    @HttpRequest()
    @ThreadInfo(vt = true)
    public RunResult push(HttpContext httpContext, @Body SLog sLog) {

        log.info("sLog - {}", sLog.toJsonString());

        if (sLog.getGameId() == 0) return RunResult.error("gameId is null");
        if (StringUtils.isBlank(sLog.getToken())) return RunResult.error("token is null");

        GameRecord gameRecord = this.gameService.getGameId2GameRecordMap().get(sLog.getGameId());
        if (gameRecord == null)
            return RunResult.error("gameId is error");

        if (Objects.equals(gameRecord.getLogToken(), sLog.getToken()))
            return RunResult.error("game log token error");

        PgsqlDataHelper pgsqlDataHelper = this.gameService.pgsqlDataHelper(sLog.getGameId());

        if (sLog.getUid() == 0)
            sLog.setUid(gameService.newId(sLog.getGameId()));

        if (sLog.getLogTime() == 0) {
            sLog.setLogTime(System.currentTimeMillis());
        }

        LocalDate localDate = MyClock.localDate(sLog.getLogTime());
        sLog.setDayKey(localDate.getYear() * 10000 + localDate.getMonthValue() * 100 + localDate.getDayOfMonth());
        pgsqlDataHelper.getSqlDataBatch().insert(sLog);
        // pgsqlDataHelper.getSqlDataBatch().insert(sLog);
        return RunResult.ok();
    }

    @HttpRequest()
    @ThreadInfo(vt = true)
    public RunResult list(HttpContext httpSession,
                          @Param("gameId") Integer gameId,
                          @Param("logType") String logType,
                          @Param("pageIndex") Integer pageIndex,
                          @Param("pageSize") Integer pageSize,
                          @Param(value = "account", required = false) String account,
                          @Param(value = "roleId", required = false) String roleId,
                          @Param(value = "roleName", required = false) String roleName,
                          @Param(value = "dataJson", required = false) String dataJson) {

        if (gameId == null || gameId == 0) return RunResult.error("gameId is null");

        GameRecord gameRecord = gameService.getGameId2GameRecordMap().get(gameId);

        if (gameRecord == null) {
            return RunResult.error("gameId error");
        }

        if (!gameRecord.getTableMapping().containsKey(logType)) {
            return RunResult.error("log type error");
        }

        String sqlWhere = "";
        Object[] args = new Object[0];
        if (StringUtils.isNotBlank(account)) {
            sqlWhere = "account = ?";
            args = Objects.merge(args, account);
        }
        if (StringUtils.isNotBlank(roleId)) {
            if (!sqlWhere.isEmpty()) {
                sqlWhere += " AND ";
            }
            sqlWhere += "roleid = ?";
            args = Objects.merge(args, roleId);
        }
        if (StringUtils.isNotBlank(roleName)) {
            if (!sqlWhere.isEmpty()) {
                sqlWhere += " AND ";
            }
            sqlWhere += "rolename = ?";
            args = Objects.merge(args, roleName);
        }

        if (StringUtils.isNotBlank(dataJson)) {
            String[] split = dataJson.split(",");
            for (String s : split) {
                if (!sqlWhere.isEmpty()) {
                    sqlWhere += " AND ";
                }
                String[] strings = s.split("=");
                sqlWhere += "json_extract_path_text(data,'" + strings[0] + "') = ?";
                args = Objects.merge(args, strings[1]);
            }
        }
        String sql = "select * from " + logType;
        if (StringUtils.isNotBlank(sqlWhere)) {
            sql += " where " + sqlWhere;
        } else {
            args = Objects.ZERO_ARRAY;
        }

        sql += " order by logtime desc";

        if (pageIndex != null && pageIndex > 0) {
            sql += " offset " + (pageIndex - 1) * pageSize;
        }

        if (pageSize == null || pageSize <= 10) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;
        sql += " limit " + pageSize;

        PgsqlDataHelper pgsqlDataHelper = this.gameService.pgsqlDataHelper(gameId);

        long rowCount = pgsqlDataHelper.tableCount(logType, sqlWhere, args);

        List<SLog> slogs = pgsqlDataHelper.findListBySql(
                SLog.class,
                sql,
                args
        );

        List<JSONObject> list = slogs.stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("logTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("logTime")));
                    jsonObject.put("data", jsonObject.getString("data"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }

}
