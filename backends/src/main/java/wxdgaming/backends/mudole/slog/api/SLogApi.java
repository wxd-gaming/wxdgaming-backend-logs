package wxdgaming.backends.mudole.slog.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.SLog;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.threading.ExecutorUtil;
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
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") SLog sLog) {
        log.info("sLog - {}", sLog.toJsonString());
        boolean haveLogType = gameContext.getGame().getTableMapping().containsKey(sLog.getLogType());
        if (!haveLogType) {
            gameContext.recordError(sLog.toJsonString(), "表结构不存在 " + sLog.getLogType());
        } else {
            if (sLog.getUid() == 0)
                sLog.setUid(gameContext.newId(sLog.getLogType()));
            String logKey = sLog.getLogType() + sLog.getUid();
            boolean haveLogKey = gameContext.getLogKeyCache().containsKey(logKey);
            if (haveLogKey) {
                gameContext.recordError(sLog.toJsonString(), "表结构 " + sLog.getLogType() + " 重复日志记录 " + sLog.getUid());
            } else {
                gameContext.getLogKeyCache().put(logKey, true);
                sLog.checkDataKey();
                gameContext.getDataHelper().getDataBatch().insert(sLog);
            }
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<SLog> recordList) {
        ExecutorUtil.getInstance().getVirtualExecutor().execute(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                for (SLog record : recordList) {
                    push(gameContext, record);
                }
            }
        });
        return RunResult.ok();
    }


    @HttpRequest(authority = 9)
    @ExecutorWith(useVirtualThread = true)
    public RunResult list(HttpContext httpSession,
                          @Param(path = "gameId") int gameId,
                          @Param(path = "logType") String logType,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName,
                          @Param(path = "dataJson", required = false) String dataJson) {

        GameContext gameContext = gameService.gameContext(gameId);

        if (gameContext == null) {
            return RunResult.error("gameId error");
        }

        if (!gameContext.getGame().getTableMapping().containsKey(logType)) {
            return RunResult.error("log type error");
        }
        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
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

        sqlQueryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);

        long rowCount = sqlQueryBuilder.findCount();

        List<SLog> slogs = sqlQueryBuilder.findList2Entity(SLog.class);

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
