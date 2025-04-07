package wxdgaming.backends.mudole.serverlog.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.SServerLog;
import wxdgaming.backends.mudole.serverlog.SServerLogService;
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
 * 区服日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-12 20:05
 */
@Slf4j
@Singleton
@RequestMapping(path = "log/server/slog")
public class SServerLogApi {

    final GameService gameService;
    final SServerLogService sServerLogService;

    @Inject
    public SServerLogApi(GameService gameService, SServerLogService sServerLogService) {
        this.gameService = gameService;
        this.sServerLogService = sServerLogService;
    }

    @HttpRequest(authority = 2)
    @ExecutorWith(useVirtualThread = true)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") SServerLog sServerLog) {
        log.info("sLog - {}", sServerLog.toJSONString());
        gameContext.submit(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                boolean haveLogType = gameContext.getGame().getServerTableMapping().containsKey(sServerLog.getLogType());
                if (!haveLogType) {
                    gameContext.recordError("表结构不存在 " + sServerLog.getLogType(), sServerLog.toJSONString());
                } else {
                    if (sServerLog.getUid() == 0) {
                        sServerLog.setUid(gameContext.newId(sServerLog.getLogType()));
                    }
                    SServerLog byWhere = gameContext.getDataHelper().findByWhere(sServerLog.getLogType(), SServerLog.class, "uid=?", sServerLog.getUid());
                    if (byWhere == null) {
                        sServerLog.checkDataKey();
                        gameContext.getDataHelper().getDataBatch().insert(sServerLog);
                    } else {
                        byWhere.setCreateTime(sServerLog.getCreateTime());
                        byWhere.setSid(sServerLog.getSid());
                        byWhere.setOther(sServerLog.getOther());
                        byWhere.checkDataKey();
                        gameContext.getDataHelper().getDataBatch().update(byWhere);
                    }
                }
            }
        });
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<SServerLog> recordList) {

        for (SServerLog record : recordList) {
            push(gameContext, record);
        }
        return RunResult.OK;
    }


    @HttpRequest(authority = 9)
    @ExecutorWith(useVirtualThread = true)
    public RunResult list(HttpContext httpSession,
                          @ThreadParam GameContext gameContext,
                          @Param(path = "logType") String logType,
                          @Param(path = "pageIndex") Integer pageIndex,
                          @Param(path = "pageSize") Integer pageSize,
                          @Param(path = "minDay", required = false) String minDay,
                          @Param(path = "maxDay", required = false) String maxDay,
                          @Param(path = "sid", required = false) Integer sid,
                          @Param(path = "other", required = false) String other) {

        if (gameContext == null) {
            return RunResult.error("gameId error");
        }

        if (!gameContext.getGame().getServerTableMapping().containsKey(logType)) {
            return RunResult.error("log type error");
        }
        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();

        queryBuilder.setTableName(logType);

        if (StringUtils.isNotBlank(minDay)) {
            queryBuilder.pushWhereByValueNotNull("daykey>=?", NumberUtil.retainNumber(minDay));
        }

        if (StringUtils.isNotBlank(maxDay)) {
            queryBuilder.pushWhereByValueNotNull("daykey<=?", NumberUtil.retainNumber(maxDay));
        }

        if (sid != null) {
            queryBuilder.pushWhereByValueNotNull("sid=?", NumberUtil.parseInt(sid, 0));
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

        List<SServerLog> slogs = queryBuilder.findList2Entity(SServerLog.class);

        List<JSONObject> list = slogs.stream()
                .map(SServerLog::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("other", jsonObject.getString("other"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }

}
