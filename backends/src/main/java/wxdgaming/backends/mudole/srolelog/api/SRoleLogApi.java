package wxdgaming.backends.mudole.srolelog.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.SRoleLog;
import wxdgaming.backends.mudole.srolelog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.threading.ExecutorWith;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.NumberUtil;
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
@RequestMapping(path = "log/role/slog")
public class SRoleLogApi {

    final GameService gameService;
    final SLogService SLogService;

    @Inject
    public SRoleLogApi(GameService gameService, SLogService SLogService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
    }

    @HttpRequest(authority = 2)
    @ExecutorWith(useVirtualThread = true)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") SRoleLog sRoleLog) {
        log.info("sLog - {}", sRoleLog.toJSONString());
        gameContext.submit(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                boolean haveLogType = gameContext.getGame().getRoleTableMapping().containsKey(sRoleLog.getLogType());
                if (!haveLogType) {
                    gameContext.recordError("表结构不存在 " + sRoleLog.getLogType(), sRoleLog.toJSONString());
                } else {
                    if (sRoleLog.getUid() == 0)
                        sRoleLog.setUid(gameContext.newId(sRoleLog.getLogType()));

                    String logKey = sRoleLog.tableName() + sRoleLog.getUid();
                    boolean haveLogKey = gameContext.getLogKeyCache().has(logKey);
                    if (haveLogKey) {
                        gameContext.recordError("表结构 " + sRoleLog.getLogType() + " 重复日志记录 " + sRoleLog.getUid(), sRoleLog.toJSONString());
                    } else {
                        gameContext.getLogKeyCache().put(logKey, true);
                        sRoleLog.checkDataKey();
                        gameContext.getDataHelper().getDataBatch().insert(sRoleLog);
                    }
                }
            }
        });
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<SRoleLog> recordList) {

        for (SRoleLog record : recordList) {
            push(gameContext, record);
        }
        return RunResult.OK;
    }


    @HttpRequest(authority = 9)
    @ExecutorWith(useVirtualThread = true)
    public RunResult list(HttpContext httpSession,
                          @Param(path = "gameId") int gameId,
                          @Param(path = "logType") String logType,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "minDay", required = false) String minDay,
                          @Param(path = "maxDay", required = false) String maxDay,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName,
                          @Param(path = "other", required = false) String other) {

        GameContext gameContext = gameService.gameContext(gameId);

        if (gameContext == null) {
            return RunResult.error("gameId error");
        }

        if (!gameContext.getGame().getRoleTableMapping().containsKey(logType)) {
            return RunResult.error("log type error");
        }
        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();

        queryBuilder
                .setTableName(logType)
                .pushWhereByValueNotNull("account=?", account)
                .pushWhereByValueNotNull("roleid=?", roleId)
                .pushWhereByValueNotNull("rolename=?", roleName);

        if (StringUtils.isNotBlank(minDay)) {
            queryBuilder.pushWhereByValueNotNull("daykey>=?", NumberUtil.retainNumber(minDay));
        }

        if (StringUtils.isNotBlank(maxDay)) {
            queryBuilder.pushWhereByValueNotNull("daykey<=?", NumberUtil.retainNumber(maxDay));
        }

        if (StringUtils.isNotBlank(other)) {
            String[] split = other.split(",");
            for (String s : split) {
                String[] strings = s.split("=");
                queryBuilder.pushWhere("json_extract_path_text(other,'" + strings[0] + "') = ?", strings[1]);
            }
        }

        queryBuilder.setOrderBy("createtime desc");

        queryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);

        long rowCount = queryBuilder.findCount();

        List<SRoleLog> slogs = queryBuilder.findList2Entity(SRoleLog.class);

        List<JSONObject> list = slogs.stream()
                .map(SRoleLog::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("other", jsonObject.getString("other"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }

}
