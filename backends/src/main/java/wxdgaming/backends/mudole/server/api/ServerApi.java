package wxdgaming.backends.mudole.server.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.ServerRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.io.FileReadUtil;
import wxdgaming.boot2.core.io.FileWriteUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.js.JsService;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

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
                    serverRecord.setRegisterRoleCount(record.getRegisterRoleCount());
                    serverRecord.getOther().putAll(record.getOther());
                    serverRecord.getUpdateTime().refresh(System.currentTimeMillis());
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

    @HttpRequest(authority = 2)
    public RunResult addList(@ThreadParam GameContext gameContext, @Param(path = "data") List<ServerRecord> recordList) {
        for (ServerRecord record : recordList) {
            add(gameContext, record);
        }
        return RunResult.OK;
    }

    @HttpRequest(authority = 9)
    public RunResult add(@ThreadParam GameContext gameContext, @Param(path = "data") ServerRecord record) {
        if (record.getUid() == 0) {
            return RunResult.error("sid为空 " + record.toJSONString());
        } else {
            ServerRecord serverRecord = gameContext.serverGetOrCreate(record.getUid());
            serverRecord.setGroup(record.getGroup());
            serverRecord.setOrdinal(record.getOrdinal());
            serverRecord.setLabel(record.getLabel());
            serverRecord.setName(record.getName());
            serverRecord.setOpenTime(record.getOpenTime());
            serverRecord.setWlan(record.getWlan());
            serverRecord.setLan(record.getLan());
            serverRecord.setPort(record.getPort());
            serverRecord.setWebPort(record.getWebPort());
            serverRecord.getUpdateTime().refresh(System.currentTimeMillis());
            gameContext.getDataHelper().getDataBatch().update(record);
        }
        return RunResult.OK;
    }

    @HttpRequest(authority = 9)
    public RunResult editShowName(@ThreadParam GameContext gameContext,
                                  @Param(path = "sid") int sid,
                                  @Param(path = "name") String name,
                                  @Param(path = "showName") String showName) {
        ServerRecord serverRecord = gameContext.getServerRecordMap().get(sid);
        serverRecord.setName(name);
        serverRecord.setShowName(showName);
        gameContext.getDataHelper().getDataBatch().save(serverRecord);
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
            if (!v.isEnabled()) return;/*如果禁用了*/
            Value execute = jsService.threadContext().execute("actionServer2Json", v);
            Map<?, ?> string = execute.as(Map.class);
            vs.add(string);
        });
        ok.data(vs);
        return ok;
    }

    @HttpRequest()
    public RunResult get(HttpContext httpContext, @Param(path = "gameId") int gameId, @Param(path = "sid") int sid) {
        GameContext gameContext = gameService.gameContext(gameId);
        if (gameContext == null) {
            return RunResult.error("gameId is not exist");
        }
        RunResult ok = RunResult.ok();
        String js = getJs(gameId);
        /*通用的*/
        jsService.threadContext().eval(js);

        ServerRecord serverRecord = gameContext.getServerRecordMap().get(sid);
        if (serverRecord == null) {
            return RunResult.error("sid is not exist");
        }
        if (!serverRecord.isEnabled()) {
            return RunResult.error("sid is not exist");
        }
        Value execute = jsService.threadContext().execute("actionServer2Json", serverRecord);
        Map<?, ?> string = execute.as(Map.class);
        ok.data(string);
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

        Map<Integer, ServerRecord> serverRecordMap = gameContext.getServerRecordMap();

        Stream<ServerRecord> stream = new ArrayList<>(serverRecordMap.values()).stream();

        if (StringUtils.isNotBlank(sid)) {
            stream = stream.filter(v -> v.getUid() == Integer.parseInt(sid));
        }

        if (StringUtils.isNotBlank(mainSid)) {
            stream = stream.filter(v -> v.getMainSid() == Integer.parseInt(mainSid));
        }
        if (StringUtils.isNotBlank(name)) {
            stream = stream.filter(v -> Objects.equals(v.getName(), name));
        }

        if (StringUtils.isNotBlank(showName)) {
            stream = stream.filter(v -> Objects.equals(v.getShowName(), showName));
        }
        if (StringUtils.isNotBlank(wlan)) {
            stream = stream.filter(v -> Objects.equals(v.getWlan(), wlan));
        }
        if (StringUtils.isNotBlank(lan)) {
            stream = stream.filter(v -> Objects.equals(v.getLan(), lan));
        }

        stream = stream.sorted((o1, o2) -> Integer.compare(o2.getUid(), o1.getUid()));

        List<ServerRecord> records = stream.toList();

        stream = records.stream();

        if (pageIndex > 0) {
            stream = stream.skip(((long) pageIndex - 1) * pageSize);
        }

        if (pageSize <= 10) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;

        stream = stream.limit(pageSize);

        long rowCount = records.size();

        Collection<RoleRecord> roleRecords = gameContext.getRoleRecordJdbcCache().values();
        List<JSONObject> list = stream.map(serverRecord -> {
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
                    jsonObject.put("updateTime", serverRecord.getUpdateTime().dateFormat());
                    jsonObject.put("openTime", serverRecord.getOpenTime().dateFormat());
                    jsonObject.put("maintainTime", serverRecord.getMaintainTime().dateFormat());
                    jsonObject.put("other", serverRecord.getOther().toJSONString());
                    return jsonObject;
                })
                .toList();

        return RunResult.ok().data(list).fluentPut("rowCount", rowCount);
    }

}
