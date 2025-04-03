package push;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.io.FileReadUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ExecutorServices;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.threading.ExecutorUtilImpl;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.batis.Entity;
import wxdgaming.boot2.starter.net.httpclient.HttpBuilder;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * test
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 14:02
 **/
@Slf4j
public class GameApiTest {

    // protected String postHost = "http://211.149.228.9:19000";
    protected String postHost = "http://127.0.0.1:19000";

    protected static int days = 120;
    protected static int gameId = 2;
    protected static HexId hexId = new HexId(gameId);

    protected AtomicBoolean logined = new AtomicBoolean();
    protected static ExecutorServices executorServices;


    static {
        executorServices = ExecutorUtilImpl.getInstance().newExecutorServices("push", 10, 10, 10000);
        ExecutorUtilImpl.getInstance().init();
    }

    public void post(String path, Entity entity) throws Exception {
        String json = entity.toJSONString();
        CompletableFuture<Response<PostText>> post = post(path, json);
        Response<PostText> postTextResponse = post.join();
        postTextResponse.systemOut();
    }

    public CompletableFuture<Response<PostText>> post(String path, String json) {
        login();
        if (path.startsWith("/")) path = path.substring(1);
        return HttpBuilder.postJson(postHost + "/" + path, json)
                .readTimeout(130000)
                // .header(HttpHeaderNames.AUTHORIZATION.toString(), token)
                .async();
    }


    public JSONObject listLogType() {
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject
                .fluentPut("gameId", gameId)
                .fluentPut("token", findAppToken());

        String push = HttpBuilder.postJson("game/listLogType", jsonObject.toJSONString())
                .readTimeout(130000)
                .request()
                .bodyString();
        RunResult runResult = RunResult.parse(push);
        if (runResult.code() != 1)
            throw new RuntimeException(runResult.msg());
        return runResult.data(JSONObject.class);
    }

    public String findAppToken() {
        login();
        JSONObject jsonObject = MapOf.newJSONObject().fluentPut("gameId", gameId);
        Response<PostText> postTextResponse = post("/game/find", jsonObject.toJSONString()).join();
        int i = postTextResponse.responseCode();
        RunResult runResult = postTextResponse.bodyRunResult();
        if (i != 200 || runResult.code() != 1)
            throw new RuntimeException(runResult.msg());
        String appToken = runResult.getNestedValue("data.appToken", String.class);
        System.out.println("token: " + appToken);
        return appToken;
    }

    public String findLogToken() {
        login();
        JSONObject jsonObject = MapOf.newJSONObject().fluentPut("gameId", gameId);
        Response<PostText> postTextResponse = post("/game/find", jsonObject.toJSONString()).join();
        int i = postTextResponse.responseCode();
        RunResult runResult = RunResult.parse(postTextResponse.bodyString());
        if (i != 200 || runResult.code() != 1)
            throw new RuntimeException(runResult.msg());
        String logToken = runResult.getNestedValue("data.logToken", String.class);
        System.out.println("token: " + logToken);
        return logToken;
    }

    public HashMap<Integer, List<JSONObject>> readAccount() {
        String readString = FileReadUtil.readString("account.json", StandardCharsets.UTF_8);
        return FastJsonUtil.parse(readString, new TypeReference<HashMap<Integer, List<JSONObject>>>() {});
    }

    public void login() {
        if (logined.get()) {
            return;
        }
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject
                .fluentPut("account", "wxdgaming")
                .fluentPut("pwd", "123456");
        Response<PostText> postTextResponse = HttpBuilder.postJson(postHost + "/login", jsonObject.toJSONString())
                .request();
        int i = postTextResponse.responseCode();
        String token = postTextResponse.cookie(HttpHeaderNames.AUTHORIZATION.toString());
        RunResult runResult = RunResult.parse(postTextResponse.bodyString());
        if (i != 200 || runResult.code() != 1)
            throw new RuntimeException(runResult.msg());
        System.out.println("token: " + token);
        logined.set(true);
    }

    private final char[] chars = new char[]{'a', 'b', 'c', 'd'};

    public String randomAccount() {
        return StringUtils.randomString(chars, 8);
    }

    public long randomCreateTime() {
        int random = RandomUtils.random(0, 120);
        random = random - 120;
        return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(random);
    }

    @Test
    public void outTime() {
        LocalDateTime localDateTime = LocalDateTime.now().plusDays(-125);
        System.out.println(localDateTime);
        System.out.println(MyClock.time2Milli(localDateTime));
    }

    @Test
    public void addGame() throws Exception {
        JSONObject jsonObject = MapOf.newJSONObject("gameName", "超变传奇");
        post("game/add", jsonObject.toString()).get().bodyString();
    }

    @Test
    public void pushGame() throws Exception {
        Game game = new Game();
        game.setName("超变传世");
        game.setIcon("icon");
        game.setDesc("desc");
        game.setUrl("url");
        game.setCreateTime(MyClock.time2Milli(LocalDateTime.now().plusDays(-125)));

        LinkedHashMap<String, String> tableMapping = game.getRoleTableMapping();
        tableMapping.put("log_lv", "升级日志");

        post("game/push", game);
    }

    @Test
    public void pushLogType() {
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject
                .fluentPut("gameId", gameId)
                .fluentPut("token", findAppToken())
                .fluentPut("logType", "log_lv")
                .fluentPut("logComment", "等级日志");

        post("game/addLogType", jsonObject.toJSONString());
    }


}
