package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.SLog;
import wxdgaming.backends.entity.games.logs.SLog2Login;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.lang.DiffTime;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.lang.Tick;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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


    @Test
    public void pushItemLogList() {
        String logToken = findLogToken();
        HashMap<Integer, List<AccountRecord>> accountRecordMap = readAccount();
        for (List<AccountRecord> recordList : accountRecordMap.values()) {
            for (AccountRecord accountRecord : recordList) {
                pushItemLog(logToken, accountRecord);
            }
        }
    }


    public void pushItemLog(String logToken, AccountRecord accountRecord) {
        List<CompletableFuture<Response<PostText>>> futures = new ArrayList<>();
        long createTime = accountRecord.getCreateTime();
        LocalDateTime localDateTime = MyClock.localDateTime(createTime);

        DiffTime diffTime = new DiffTime();
        for (int i = 0; i < days; i++) {
            LocalDateTime plusDays = localDateTime.plusDays(i);
            long milli = MyClock.time2Milli(plusDays);
            if (milli > MyClock.millis()) continue;/*当前当前时间不在执行*/
            SLog sLog = new SLog();
            sLog.setLogType("log_item");
            sLog.setUid(hexId.newId());
            sLog.setCreateTime(milli);
            sLog.setAccount(accountRecord.getAccount());
            sLog.setRoleId(accountRecord.getUid());
            sLog.setRoleName(StringUtils.randomString(8));
            sLog.setMainId(1);
            sLog.setSId(1);
            sLog.setLv(RandomUtils.random(1, 100));
            sLog.getData().fluentPut("a", "b")
                    .fluentPut("itemId", RandomUtils.random(1, 100))
                    .fluentPut("itemNum", RandomUtils.random(1, 100))
                    .fluentPut("item_name", "货币")
                    .fluentPut("bind", "true");


            JSONObject push = new JSONObject()
                    .fluentPut("gameId", gameId)
                    .fluentPut("token", logToken)
                    .fluentPut("data", sLog);

            CompletableFuture<Response<PostText>> async = post("log/push", push.toJSONString());
            futures.add(async);
        }
        for (CompletableFuture<Response<PostText>> future : futures) {
            Response<PostText> join = future.join();
            if (join.responseCode() != 200 || join.bodyRunResult().code() != 1) {
                log.error("{}", join.bodyString());
            }
        }
        System.out.println(futures.size() + ", 耗时：" + diffTime.diff() + " ms");
    }


    @Test
    public void pushLoginLogList() {
        String logToken = findLogToken();
        HashMap<Integer, List<AccountRecord>> accountRecordMap = readAccount();
        for (List<AccountRecord> recordList : accountRecordMap.values()) {
            for (AccountRecord accountRecord : recordList) {
                pushLoginLog(logToken, accountRecord);
            }
        }
    }

    public void pushLoginLog(String logToken, AccountRecord accountRecord) {

        long createTime = accountRecord.getCreateTime();
        LocalDateTime localDateTime = MyClock.localDateTime(createTime);

        DiffTime diffTime = new DiffTime();
        List<SLog2Login> sLogs = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDateTime plusDays = localDateTime.plusDays(i);
            long milli = MyClock.time2Milli(plusDays);
            if (milli > MyClock.millis()) continue;/*当前当前时间不在执行*/
            int random = 130 - i;
            boolean randomBoolean = RandomUtils.randomBoolean(random, 500);
            if (!randomBoolean) continue;
            SLog2Login sLog = new SLog2Login();
            sLog.setLogType("log_login");
            sLog.setUid(hexId.newId());
            sLog.setCreateTime(milli);
            sLog.setAccount(accountRecord.getAccount());
            sLog.setRoleId(accountRecord.getUid());
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
            sLogs.add(sLog);

        }
        JSONObject push = new JSONObject()
                .fluentPut("gameId", gameId)
                .fluentPut("token", logToken)
                .fluentPut("data", sLogs);
        Response<PostText> join = post("log/pushList4Login", push.toJSONString()).join();
        RunResult runResult = join.bodyRunResult();
        if (join.responseCode() != 200 || runResult.code() != 1) {
            System.out.println(join.bodyString());
        }
        System.out.println(sLogs.size() + ", 耗时：" + diffTime.diff() + " ms");
    }

    @Test
    public void convert() {
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject.put("a", "b");
        String javaObject = jsonObject.toJavaObject(String.class);
        log.info("{}", javaObject);
    }

}
