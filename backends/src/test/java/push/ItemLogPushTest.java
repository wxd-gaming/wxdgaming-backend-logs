package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.lang.DiffTime;
import wxdgaming.boot2.core.threading.RunnableEvent;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志push
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-23 09:26
 **/
@Slf4j
public class ItemLogPushTest extends RoleApiTest {


    @Test
    public void pushItemLogList() throws InterruptedException {
        String logToken = findLogToken();
        AtomicInteger submittedCount = new AtomicInteger();
        HashMap<Integer, List<JSONObject>> accountRecordMap = readAccount();
        for (List<JSONObject> recordList : accountRecordMap.values()) {
            CountDownLatch countDown = new CountDownLatch(1);
            executorServices.execute(new RunnableEvent(150, 10000, () -> {
                try {
                    submittedCount.addAndGet(pushItemLog(logToken, recordList));
                } finally {
                    countDown.countDown();
                }
            }));
            countDown.await();
        }
        executorServices.shutdown();
        System.out.println("总提交：" + submittedCount.get());
    }


    public int pushItemLog(String logToken, List<JSONObject> recordList) {
        DiffTime diffTime = new DiffTime();
        List<JSONObject> sLogs = new ArrayList<>();
        for (JSONObject accountRecord : recordList) {
            long createTime = accountRecord.getLongValue("createTime");
            LocalDateTime localDateTime = MyClock.localDateTime(createTime);

            for (int i = 0; i < days; i++) {
                LocalDateTime plusDays = localDateTime.plusDays(i);
                long milli = MyClock.time2Milli(plusDays);
                if (milli > MyClock.millis()) continue;/*当前当前时间不在执行*/
                JSONObject sLog = new JSONObject();
                sLog.fluentPut("uid", hexId.newId());/*创建唯一id，避免传递重复脏数据*/
                sLog.fluentPut("createTime", milli);
                sLog.fluentPut("account", accountRecord.getString("account"));
                sLog.fluentPut("roleId", accountRecord.getLong("uid"));
                sLog.fluentPut("roleName", StringUtils.randomString(8));
                sLog.fluentPut("sid", 1);
                sLog.fluentPut("lv", RandomUtils.random(1, 100));
                sLog.fluentPut("changeType", RandomUtils.randomBoolean() ? "GET" : "COST");
                sLog.fluentPut("itemId", RandomUtils.random(1, 100));
                sLog.fluentPut("itemBind", RandomUtils.randomBoolean());
                sLog.fluentPut("itemCount", RandomUtils.random(1, 100));
                sLog.fluentPut("itemType", "货币");
                sLog.fluentPut("itemSubType", "货币");
                sLog.fluentPut("itemName", "货币");
                sLog.fluentPut("source", "首充");
                sLog.fluentPut("comment", "首充id=1");
                sLog.fluentPut(
                        "other", MapOf.newJSONObject().fluentPut("a", "b")
                );
                sLogs.add(sLog);
            }
        }
        if (!sLogs.isEmpty()) {
            JSONObject push = new JSONObject()
                    .fluentPut("gameId", gameId)
                    .fluentPut("token", logToken)
                    .fluentPut("data", sLogs);

            CompletableFuture<Response<PostText>> future = post("log/item/pushList", push.toJSONString());
            Response<PostText> join = future.join();
            if (join.responseCode() != 200 || join.bodyRunResult().code() != 1) {
                log.error("{}", join.bodyString());
            }
            System.out.println(sLogs.size() + ", 耗时：" + diffTime.diff() + " ms");
        }
        return sLogs.size();
    }

}
