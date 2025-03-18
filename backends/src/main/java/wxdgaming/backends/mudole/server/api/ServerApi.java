package wxdgaming.backends.mudole.server.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.games.logs.ServerRecord;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.io.FileReadUtil;
import wxdgaming.boot2.core.io.FileWriteUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.js.JsService;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    final JsService jsService;

    @Inject
    public ServerApi(GameService gameService, JsService jsService) {
        this.gameService = gameService;
        this.jsService = jsService;
    }

    @HttpRequest(authority = 2)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") ServerRecord record) {
        gameContext.submit(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                if (record.getUid() == 0) {
                    gameContext.recordError("sid为空", record.toJSONString());
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

    private String getJs(int gameId) {
        String string = null;
        File file = new File("./script/" + gameId + "/action-server-2-json.js");
        if (file.exists()) {
            string = FileReadUtil.readString(file, StandardCharsets.UTF_8);
        }
        if (StringUtils.isBlank(string)) {
            string = FileReadUtil.readString("script/action-server-2-json.js", StandardCharsets.UTF_8);
        }
        return string;
    }

    @HttpRequest(authority = 9)
    public RunResult jsPlugin(HttpContext httpContext,
                              @Param(path = "gameId") int gameId) {

        String string = getJs(gameId);

        return RunResult.ok().data(string);
    }

    @HttpRequest(authority = 9)
    public RunResult saveJsPlugin(HttpContext httpContext,
                                  @Param(path = "gameId") int gameId,
                                  @Param(path = "js") String js) {

        File file = new File("./script/" + gameId + "/action-server-2-json.js");
        FileWriteUtil.writeString(file, js);
        return RunResult.ok();
    }

    @HttpRequest()
    public RunResult json(HttpContext httpContext, @Param(path = "gameId") int gameId) {
        GameContext gameContext = gameService.gameContext(gameId);
        if (gameContext == null) {
            return RunResult.error("gameId is not exist");
        }
        RunResult ok = RunResult.ok();
        String js = getJs(gameId);
        /*通用的*/
        jsService.threadContext().eval(js);

        List<Map<?, ?>> vs = new ArrayList<>();
        gameContext.getServerRecordMap().values().forEach(v -> {
            Value execute = jsService.threadContext().execute("actionServer2Json", v);
            Map<?, ?> string = execute.as(Map.class);
            vs.add(string);
        });
        ok.data(vs);
        return ok;
    }

    @HttpRequest(authority = 9)
    public RunResult list(HttpContext httpContext,
                          @ThreadParam GameContext gameContext,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "sid", required = false) String sid,
                          @Param(path = "mainSid", required = false) String mainSid,
                          @Param(path = "name", required = false) String name,
                          @Param(path = "showName", required = false) String showName,
                          @Param(path = "wlan", required = false) String wlan,
                          @Param(path = "lan", required = false) String lan) {

        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();

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
        Collection<RoleRecord> roleRecords = gameContext.getRoleRecordJdbcCache().values();
        List<JSONObject> list = records.stream()
                .map(serverRecord -> {
                    JSONObject jsonObject = serverRecord.toJSONObject();
                    long onlineRoleCount = roleRecords.stream()
                            .filter(v -> v.online())
                            .filter(v -> v.getCurSid() == serverRecord.getUid())
                            .count();

                    long activeRoleCount = roleRecords.stream()
                            .filter(v -> MyClock.isSameDay(v.getOnlineUpdateTime()))
                            .filter(v -> v.getCurSid() == serverRecord.getUid())
                            .count();

                    jsonObject.put("onlineRoleCount", onlineRoleCount);
                    jsonObject.put("activeRoleCount", activeRoleCount);
                    jsonObject.put("updateTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", serverRecord.getUpdateTime()));
                    jsonObject.put("other", serverRecord.getOther().toJSONString());
                    return jsonObject;
                })
                .toList();

        return RunResult.ok().data(list).fluentPut("rowCount", rowCount);
    }

}
