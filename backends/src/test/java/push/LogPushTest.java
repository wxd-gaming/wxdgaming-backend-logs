package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.SLog;
import wxdgaming.boot2.core.chatset.StringUtils;
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

        List<SLog> sLogs = new ArrayList<>();
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
