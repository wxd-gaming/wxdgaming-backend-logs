package push;

import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import wxdgaming.backends.entity.RecordBase;
import wxdgaming.backends.entity.system.GameRecord;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ExecutorConfig;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.starter.net.httpclient.HttpBuilder;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * test
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 14:02
 **/
@Slf4j
public class GameApiTest {

    protected int gameId = 2;

    protected AtomicBoolean logined = new AtomicBoolean();

    static {
        ExecutorUtil.init(ExecutorConfig.INSTANCE);
    }

    public void post(String path, RecordBase base) throws Exception {
        String json = base.toJsonString();
        CompletableFuture<Response<PostText>> post = post(path, json);
        Response<PostText> postTextResponse = post.join();
        postTextResponse.systemOut();
    }

    public CompletableFuture<Response<PostText>> post(String path, String json) {
        login();
        if (path.startsWith("/")) path = path.substring(1);
        return  HttpBuilder.postJson("http://127.0.0.1:19000/" + path, json)
                .readTimeout(10000)
                // .header(HttpHeaderNames.AUTHORIZATION.toString(), token)
                .async();
    }


    public JSONObject listLogType() {
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject
                .fluentPut("gameId", gameId)
                .fluentPut("token", findAppToken());

        String push = HttpBuilder.postJson("game/listLogType", jsonObject.toJSONString()).request().bodyString();
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
                .request();
        int i = postTextResponse.responseCode();
        RunResult runResult = RunResult.parse(postTextResponse.bodyString());
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
                .request();
        int i = postTextResponse.responseCode();
        RunResult runResult = RunResult.parse(postTextResponse.bodyString());
        if (runResult.code() != 1)
            throw new RuntimeException(runResult.msg());
        String logToken = runResult.getNestedValue("data.logToken", String.class);
        System.out.println("token: " + logToken);
        return logToken;
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

    @Test
    public void pushGame() throws Exception {
        GameRecord gameRecord = new GameRecord();
        gameRecord.setUid(9L);
        gameRecord.setName("野火燎原");
        gameRecord.setIcon("icon");
        gameRecord.setDesc("desc");
        gameRecord.setUrl("url");

        LinkedHashMap<String, String> tableMapping = gameRecord.getTableMapping();
        tableMapping.put("log_item", "道具日志");
        tableMapping.put("log_pay", "支付日志");
        tableMapping.put("log_login", "登录日志");

        post("game/push", gameRecord);
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
