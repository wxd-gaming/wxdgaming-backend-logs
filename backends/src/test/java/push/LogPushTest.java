package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.games.logs.SLog;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 日志push
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-23 09:26
 **/
@Slf4j
public class LogPushTest extends RoleApiTest {

    static HexId hexId = new HexId(1);

    @Test
    public void pushItemLog() {
        String logToken = findLogToken();
        pushItemLog(logToken, 1);
    }

    @Test
    public void pushItemLogList() {
        String logToken = findLogToken();
        for (int i = 0; i < 50000; i++) {
            pushItemLog(logToken, 100);
        }
    }


    public void pushItemLog(String logToken, int count) {
        List<CompletableFuture<Response<PostText>>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SLog sLog = new SLog();
            sLog.setGameId(gameId);
            sLog.setToken(logToken);
            sLog.setLogType("log_item");
            sLog.setUid(hexId.newId());
            sLog.setCreateTime(randomCreateTime());
            sLog.setAccount(randomAccount());
            sLog.setRoleId(String.valueOf(RandomUtils.random(1, 1000)));
            sLog.setRoleName(StringUtils.randomString(8));
            sLog.setMainId(1);
            sLog.setSId(1);
            sLog.setLv(RandomUtils.random(1, 100));
            sLog.getData().fluentPut("a", "b")
                    .fluentPut("itemId", RandomUtils.random(1, 100))
                    .fluentPut("itemNum", RandomUtils.random(1, 100))
                    .fluentPut("item_name", "货币")
                    .fluentPut("bind", "true");

            String json = sLog.toJsonString();
            CompletableFuture<Response<PostText>> async = post("log/push", json);
            futures.add(async);
        }
        for (CompletableFuture<Response<PostText>> future : futures) {
            Response<PostText> join = future.join();
            if (join.responseCode() != 200 || join.bodyRunResult().code() != 1) {
                log.error("{}", join.bodyString());
            }
        }
    }

    @Test
    public void pushLoginLog() {
        String logToken = findLogToken();
        pushLoginLog(logToken, 1);
    }

    @Test
    @RepeatedTest(5000)
    public void pushLoginLogList() {
        String logToken = findLogToken();
        pushLoginLog(logToken, 1000);
    }

    public void pushLoginLog(String logToken, int count) {
        List<CompletableFuture<Response<PostText>>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {

            // List<String> strings = List.of("item_log");
            SLog sLog = new SLog();
            sLog.setGameId(gameId);
            sLog.setToken(logToken);
            sLog.setLogType("log_login");
            sLog.setUid(hexId.newId());
            sLog.setCreateTime(randomCreateTime());
            sLog.setAccount(randomAccount());
            sLog.setRoleId(String.valueOf(RandomUtils.random(1, 1000)));
            sLog.setRoleName(StringUtils.randomString(8));
            sLog.setMainId(1);
            sLog.setSId(1);
            sLog.setLv(RandomUtils.random(1, 100));
            sLog.getData()
                    .fluentPut("a", "b")
                    .fluentPut("login_type", RandomUtils.random(1, 100))
                    .fluentPut("login_ip", "127.0.0.1")
                    .fluentPut("login_time", System.currentTimeMillis())
                    .fluentPut("login_platform", "android")
                    .fluentPut("login_channel", "google")
                    .fluentPut("login_version", "1.0.0")
            ;
            String json = sLog.toJsonString();
            CompletableFuture<Response<PostText>> post = post("log/push", json);
            futures.add(post);
        }
        for (CompletableFuture<Response<PostText>> future : futures) {
            Response<PostText> join = future.join();
            RunResult runResult = join.bodyRunResult();
            if (join.responseCode() != 200 || runResult.code() != 1) {
                System.out.println(join.bodyString());
            }
        }
    }

    @Test
    public void convert() {
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject.put("a", "b");
        String javaObject = jsonObject.toJavaObject(String.class);
        log.info("{}", javaObject);
    }

}
