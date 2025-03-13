package wxdgaming.backends.mudole.server.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.ServerRecord;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
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
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") ServerRecord record) {
        gameContext.submit(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                if (record.getUid() == 0) {
                    gameContext.recordError("sid为空", record.toJsonString());
                } else {
                    ServerRecord serverRecord = gameContext.serverGetOrCreate(record.getUid());
                    serverRecord.setMainSid(record.getMainSid());
                    serverRecord.setName(record.getName());
                    serverRecord.setShowName(record.getShowName());
                    serverRecord.setOpenTime(record.getOpenTime());
                    serverRecord.setMaintainTime(record.getMaintainTime());
                    serverRecord.setWlan(record.getWlan());
                    serverRecord.setLan(record.getLan());
                    serverRecord.setPort(record.getPort());
                    serverRecord.setWebPort(record.getWebPort());
                    serverRecord.setStatus(record.getStatus());
                    serverRecord.setOrdinal(record.getOrdinal());
                    serverRecord.setRegisterRoleCount(record.getRegisterRoleCount());
                    serverRecord.setActiveRoleCount(record.getActiveRoleCount());
                    serverRecord.setOnlineRoleCount(record.getOnlineRoleCount());
                    serverRecord.getOther().putAll(record.getOther());
                    serverRecord.setUpdateTime(System.currentTimeMillis());
                    gameContext.getDataHelper().getDataBatch().update(record);
                }
            }
        });
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<ServerRecord> recordList) {

        for (ServerRecord record : recordList) {
            push(gameContext, record);
        }
        return RunResult.OK;
    }

    @HttpRequest(authority = 9)
    public RunResult list(HttpContext httpContext,
                          @Param(path = "gameId") int gameId,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "sid", required = false) String sid,
                          @Param(path = "mainSid", required = false) String mainSid,
                          @Param(path = "name", required = false) String name,
                          @Param(path = "showName", required = false) String showName,
                          @Param(path = "wlan", required = false) String wlan,
                          @Param(path = "lan", required = false) String lan) {

        PgsqlDataHelper pgsqlDataHelper = gameService.gameContext(gameId).getDataHelper();

        SqlQueryBuilder sqlQueryBuilder = pgsqlDataHelper.queryBuilder();
        sqlQueryBuilder.sqlByEntity(ServerRecord.class);

        if (StringUtils.isNotBlank(sid)) {
            sqlQueryBuilder.pushWhere("uid = ?", Integer.parseInt(sid));
        }

        if (StringUtils.isNotBlank(mainSid)) {
            sqlQueryBuilder.pushWhere("mainsid = ?", Integer.parseInt(mainSid));
        }

        sqlQueryBuilder.pushWhereByValueNotNull("name = ?", name);
        sqlQueryBuilder.pushWhereByValueNotNull("showname = ?", showName);
        sqlQueryBuilder.pushWhereByValueNotNull("wlan=?", wlan);
        sqlQueryBuilder.pushWhereByValueNotNull("lan=?", lan);

        sqlQueryBuilder.setOrderBy("uid desc");
        if (pageIndex > 0) {
            sqlQueryBuilder.setSkip((pageIndex - 1) * pageSize);
        }

        if (pageSize <= 10) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;

        sqlQueryBuilder.setLimit(pageSize);

        long rowCount = sqlQueryBuilder.findCount();
        List<ServerRecord> records = sqlQueryBuilder.findList2Entity(ServerRecord.class);

        List<JSONObject> list = records.stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("updateTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("updateTime")));
                })
                .toList();

        return RunResult.ok().data(list).fluentPut("rowCount", rowCount);
    }

}
