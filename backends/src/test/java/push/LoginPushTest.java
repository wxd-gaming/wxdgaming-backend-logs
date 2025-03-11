package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.lang.DiffTime;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * 日志push
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-23 09:26
 **/
@Slf4j
public class LoginPushTest extends RoleApiTest {


    @Test
    public void pushLoginLogList() throws InterruptedException {
        String logToken = findLogToken();
        HashMap<Integer, List<JSONObject>> accountRecordMap = readAccount();
        CountDownLatch countDownLatch = new CountDownLatch(accountRecordMap.size());
        for (List<JSONObject> recordList : accountRecordMap.values()) {
            executorServices.execute(() -> {
                try {
                    for (JSONObject accountRecord : recordList) {
                        pushLoginLog(logToken, accountRecord);
                    }
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
    }

    public void pushLoginLog(String logToken, JSONObject accountRecord) {

        long createTime = accountRecord.getLong("createTime");
        LocalDateTime localDateTime = MyClock.localDateTime(createTime);

        DiffTime diffTime = new DiffTime();
        List<JSONObject> sLogs = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDateTime plusDays = localDateTime.plusDays(i);
            long milli = MyClock.time2Milli(plusDays);
            if (milli > MyClock.millis()) continue;/*当前当前时间不在执行*/
            int random = 130 - i;
            boolean randomBoolean = RandomUtils.randomBoolean(random, 500);
            if (!randomBoolean) continue;

            JSONObject jsonObject = buildLoginLog(logToken, accountRecord.getLong("uid"), accountRecord.getString("account"), "LOGIN", milli);
            sLogs.add(jsonObject);
        }
        if (!sLogs.isEmpty()) {
            JSONObject push = new JSONObject()
                    .fluentPut("gameId", gameId)
                    .fluentPut("token", logToken)
                    .fluentPut("data", sLogs);
            Response<PostText> join = post("log/login/pushList", push.toJSONString()).join();
            RunResult runResult = join.bodyRunResult();
            if (join.responseCode() != 200 || runResult.code() != 1) {
                System.out.println(join.bodyString());
            }
            System.out.println(sLogs.size() + ", 耗时：" + diffTime.diff() + " ms");
        }
    }

    @Test
    @RepeatedTest(100)
    public void randomPushLogin() {
        String logToken = findLogToken();
        HashMap<Integer, List<JSONObject>> accountRecordMap = readAccount();
        List<JSONObject> list = accountRecordMap.values().stream().flatMap(Collection::stream).toList();
        JSONObject accountRecord = RandomUtils.randomItem(list);
        /*模拟数据 账号的uid就是角色的uid*/
        pushLogout(logToken, accountRecord.getString("account"), accountRecord.getLong("uid"), RandomUtils.randomBoolean() ? "LOGIN" : "LOGOUT");
    }

    @Test
    public void pushLogin() {
        String logToken = findLogToken();
        pushLogout(logToken, "120-96-bdbbbbcb", 1146861275593700L, "LOGIN");
    }

    @Test
    public void pushLogout() {
        String logToken = findLogToken();
        pushLogout(logToken, "120-96-bdbbbbcb", 1146861275593700L, "LOGOUT");
    }

    public void pushLogout(String logToken, String account, long roleId, String logType) {
        JSONObject data = buildLoginLog(logToken, roleId, account, logType, randomCreateTime());
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject.put("gameId", gameId);
        jsonObject.put("token", logToken);
        jsonObject.put("data", data);
        CompletableFuture<Response<PostText>> post = post("log/login/push", jsonObject.toJSONString());
        Response<PostText> join = post.join();
        RunResult runResult = join.bodyRunResult();
        if (join.responseCode() != 200 || runResult.code() != 1) {
            System.out.println(join.bodyString());
        }
    }

    public JSONObject buildLoginLog(String logToken, long roleId, String account, String logEnum, long createTime) {
        return MapOf.newJSONObject()
                .fluentPut("logEnum", logEnum)
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("sid", 10)
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", account)
                .fluentPut("createTime", createTime)
                .fluentPut("lv", RandomUtils.random(1, 300))
                .fluentPut("other", MapOf.newJSONObject().fluentPut("os", "ios"));
    }

}
