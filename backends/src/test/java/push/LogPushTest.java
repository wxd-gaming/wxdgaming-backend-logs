package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.lang.DiffTime;
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
        long createTime = accountRecord.getCreateTime();
        LocalDateTime localDateTime = MyClock.localDateTime(createTime);

        List<JSONObject> sLogs = new ArrayList<>();
        DiffTime diffTime = new DiffTime();
        for (int i = 0; i < days; i++) {
            LocalDateTime plusDays = localDateTime.plusDays(i);
            long milli = MyClock.time2Milli(plusDays);
            if (milli > MyClock.millis()) continue;/*当前当前时间不在执行*/
            JSONObject sLog = new JSONObject();
            sLog.fluentPut("logType", "log_item");
            sLog.fluentPut("uid", hexId.newId());/*创建唯一id，避免传递重复脏数据*/
            sLog.fluentPut("createTime", milli);
            sLog.fluentPut("account", accountRecord.getAccount());
            sLog.fluentPut("roleId", accountRecord.getUid());
            sLog.fluentPut("roleName", StringUtils.randomString(8));
            sLog.fluentPut("mainId", 1);
            sLog.fluentPut("sId", 1);
            sLog.fluentPut("lv", RandomUtils.random(1, 100));
            sLog.fluentPut(
                    "data",
                    MapOf.newJSONObject()
                            .fluentPut("a", "b")
                            .fluentPut("itemId", RandomUtils.random(1, 100))
                            .fluentPut("itemNum", RandomUtils.random(1, 100))
                            .fluentPut("item_name", "货币")
                            .fluentPut("bind", RandomUtils.randomBoolean())
            );
            sLogs.add(sLog);
        }
        if (!sLogs.isEmpty()) {
            JSONObject push = new JSONObject()
                    .fluentPut("gameId", gameId)
                    .fluentPut("token", logToken)
                    .fluentPut("data", sLogs);

            CompletableFuture<Response<PostText>> future = post("log/pushList", push.toJSONString());
            Response<PostText> join = future.join();
            if (join.responseCode() != 200 || join.bodyRunResult().code() != 1) {
                log.error("{}", join.bodyString());
            }
        }
        System.out.println(sLogs.size() + ", 耗时：" + diffTime.diff() + " ms");
    }

}
