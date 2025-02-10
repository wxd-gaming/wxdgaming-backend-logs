package wxdgaming.backends.mudole.log.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.SLog;
import wxdgaming.backends.entity.system.GameRecord;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.backends.mudole.log.LogsService;
import wxdgaming.boot.agent.io.Objects;
import wxdgaming.boot.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot.core.lang.RunResult;
import wxdgaming.boot.core.str.StringUtil;
import wxdgaming.boot.core.str.json.FastJsonUtil;
import wxdgaming.boot.core.threading.ThreadInfo;
import wxdgaming.boot.core.timer.MyClock;
import wxdgaming.boot.net.controller.ann.Body;
import wxdgaming.boot.net.controller.ann.Param;
import wxdgaming.boot.net.controller.ann.TextController;
import wxdgaming.boot.net.controller.ann.TextMapping;
import wxdgaming.boot.net.web.hs.HttpSession;

import java.time.LocalDate;
import java.util.List;

/**
 * 日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 16:54
 **/
@Slf4j
@TextController(path = "log")
public class LogApi {

    final GameService gameService;
    final LogsService logsService;

    @Inject
    public LogApi(GameService gameService, LogsService logsService) {
        this.gameService = gameService;
        this.logsService = logsService;
    }

    @TextMapping()
    @ThreadInfo(vt = true)
    public RunResult push(HttpSession httpSession, @Body SLog sLog) {

        log.info("sLog - {}", sLog.toJson());

        if (sLog.getGameId() == 0) return RunResult.error("gameId is null");
        if (StringUtil.emptyOrNull(sLog.getToken())) return RunResult.error("token is null");

        PgsqlDataHelper pgsqlDataHelper = this.gameService.pgsqlDataHelper(sLog.getGameId());

        if (sLog.getUid() == 0)
            sLog.setUid(gameService.newId(sLog.getGameId()));

        if (sLog.getLogTime() == 0) {
            sLog.setLogTime(System.currentTimeMillis());
        }

        LocalDate localDate = MyClock.localDate(sLog.getLogTime());
        sLog.setDayKey(localDate.getYear() * 10000 + localDate.getMonthValue() * 100 + localDate.getDayOfMonth());
        pgsqlDataHelper.getBatchPool().insert(sLog);
        return RunResult.ok();
    }

    @TextMapping()
    @ThreadInfo(vt = true)
    public RunResult list(HttpSession httpSession,
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
        if (StringUtil.notEmptyOrNull(account)) {
            sqlWhere = "account = ?";
            args = Objects.merge(args, account);
        }
        if (StringUtil.notEmptyOrNull(roleId)) {
            if (!sqlWhere.isEmpty()) {
                sqlWhere += " AND ";
            }
            sqlWhere += "roleid = ?";
            args = Objects.merge(args, roleId);
        }
        if (StringUtil.notEmptyOrNull(roleName)) {
            if (!sqlWhere.isEmpty()) {
                sqlWhere += " AND ";
            }
            sqlWhere += "rolename = ?";
            args = Objects.merge(args, roleName);
        }

        if (StringUtil.notEmptyOrNull(dataJson)) {
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
        if (StringUtil.notEmptyOrNull(sqlWhere)) {
            sql += " where " + sqlWhere;
        } else {
            args = Objects.EMPTY_OBJECT_ARRAY;
        }

        sql += " order by logtime desc";

        if (pageIndex != null && pageIndex > 0) {
            sql += " offset " + (pageIndex - 1) * pageSize;
        }

        if (pageSize == null || pageSize <= 10) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;
        sql += " limit " + pageSize;

        PgsqlDataHelper pgsqlDataHelper = this.gameService.pgsqlDataHelper(gameId);

        long rowCount = pgsqlDataHelper.rowCount(logType, sqlWhere, args);

        List<SLog> slogs = pgsqlDataHelper.queryEntities(
                sql,
                SLog.class,
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
