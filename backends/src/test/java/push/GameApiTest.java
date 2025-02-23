package push;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.io.FileReadUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ExecutorConfig;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.batis.Entity;
import wxdgaming.boot2.starter.net.httpclient.HttpBuilder;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.nio.charset.StandardCharsets;
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

    protected int days = 120;
    protected int gameId = 2;
    protected HexId hexId = new HexId(gameId);

    protected AtomicBoolean logined = new AtomicBoolean();

    static {
        ExecutorUtil.getInstance().init(ExecutorConfig.INSTANCE);
    }

    public void post(String path, Entity entity) throws Exception {
        String json = entity.toJsonString();
        CompletableFuture<Response<PostText>> post = post(path, json);
        Response<PostText> postTextResponse = post.join();
        postTextResponse.systemOut();
    }

    public CompletableFuture<Response<PostText>> post(String path, String json) {
        login();
        if (path.startsWith("/")) path = path.substring(1);
        return HttpBuilder.postJson("http://127.0.0.1:19000/" + path, json)
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
        Response<PostText> postTextResponse = HttpBuilder
                .postJson(
                        "http://127.0.0.1:19000/game/find",
                        jsonObject.toJSONString()
                )
                .readTimeout(130000)
                .request();
        int i = postTextResponse.responseCode();
        RunResult runResult = postTextResponse.bodyRunResult();
        if (runResult.code() != 1)
            throw new RuntimeException(runResult.msg());
        String appToken = runResult.getNestedValue("data.appToken", String.class);
        System.out.println("token: " + appToken);
        return appToken;
    }

    public String findLogToken() {
        login();
        JSONObject jsonObject = MapOf.newJSONObject().fluentPut("gameId", gameId);
        Response<PostText> postTextResponse = HttpBuilder.postJson("http://127.0.0.1:19000/game/find", jsonObject.toJSONString())
                .readTimeout(130000)
                .request();
        int i = postTextResponse.responseCode();
        RunResult runResult = RunResult.parse(postTextResponse.bodyString());
        if (runResult.code() != 1)
            throw new RuntimeException(runResult.msg());
        String logToken = runResult.getNestedValue("data.logToken", String.class);
        System.out.println("token: " + logToken);
        return logToken;
    }

    public HashMap<Integer, List<AccountRecord>> readAccount() {
        String readString = FileReadUtil.readString("account.json", StandardCharsets.UTF_8);
        return FastJsonUtil.parse(readString, new TypeReference<HashMap<Integer, List<AccountRecord>>>() {});
    }

    public void login() {
        if (logined.get()) {
            return;
        }
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject
                .fluentPut("account", "wxdgaming")
                .fluentPut("pwd", "123456");
        Response<PostText> postTextResponse = HttpBuilder.postJson("http://127.0.0.1:19000/login", jsonObject.toJSONString())
                .request();
        int i = postTextResponse.responseCode();
        String token = postTextResponse.cookie(HttpHeaderNames.AUTHORIZATION.toString());
        RunResult runResult = RunResult.parse(postTextResponse.bodyString());
        if (runResult.code() != 1)
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
    public void pushGame() throws Exception {
        Game game = new Game();
        game.setUid(gameId);
        game.setName("神剑诀");
        game.setIcon("icon");
        game.setDesc("desc");
        game.setUrl("url");
        game.setCreateTime(randomCreateTime());

        LinkedHashMap<String, String> tableMapping = game.getTableMapping();
        tableMapping.put("log_login", "登录日志");
        tableMapping.put("log_item", "道具日志");
        tableMapping.put("log_pay", "支付日志");

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
