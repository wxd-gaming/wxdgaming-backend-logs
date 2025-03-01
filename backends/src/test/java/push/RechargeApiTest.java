package push;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import wxdgaming.backends.entity.games.logs.RechargeRecord;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.DiffTime;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 充值日志测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 11:15
 **/
public class RechargeApiTest extends AccountApiTest {


    @Test
    public void pushRechargeList() throws Exception {
        String logToken = findLogToken();
        HashMap<Integer, List<JSONObject>> accountRecordMap = readAccount();
        CountDownLatch countDownLatch = new CountDownLatch(accountRecordMap.size());
        for (List<JSONObject> recordList : accountRecordMap.values()) {
            executorServices.execute(() -> {
                for (JSONObject accountRecord : recordList) {
                    test(logToken, accountRecord);
                }
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
    }

    public void test(String logToken, JSONObject accountRecord) {

        long createTime = accountRecord.getLong("createTime");
        LocalDateTime localDateTime = MyClock.localDateTime(createTime);

        DiffTime diffTime = new DiffTime();
        List<RechargeRecord> sLogs = new ArrayList<>();
        for (int i = 1; i <= days; i++) {

            LocalDateTime plusDays = localDateTime.plusDays(i);
            long milli = MyClock.time2Milli(plusDays);
            if (milli > MyClock.millis()) continue;/*当前当前时间不在执行*/
            boolean randomBoolean = RandomUtils.randomBoolean(30);
            if (!randomBoolean) continue;

            RechargeRecord record = new RechargeRecord();
            record.setUid(hexId.newId());
            record.setAccount(accountRecord.getString("account"));
            record.setSid(RandomUtils.random(1, 100));
            record.setCreateTime(milli);
            record.setRoleId(accountRecord.getLong("uid"));
            record.setRoleName(StringUtils.randomString(8));
            record.setLv(RandomUtils.random(1, 100));
            record.setChannel("huawei");
            record.setAmount(RandomUtils.random(6, 128) * 100);
            record.setSpOrder(StringUtils.randomString(32));
            record.setCpOrder(StringUtils.randomString(32));
            record.getOther().fluentPut("充值ID", "1");
            record.getOther().fluentPut("充值商品", "首充武器");
            sLogs.add(record);

        }
        if (!sLogs.isEmpty()) {
            JSONObject push = new JSONObject()
                    .fluentPut("gameId", gameId)
                    .fluentPut("token", logToken)
                    .fluentPut("data", sLogs);
            Response<PostText> join = post("recharge/pushList", push.toJSONString()).join();
            RunResult runResult = join.bodyRunResult();
            if (join.responseCode() != 200 || runResult.code() != 1) {
                System.out.println(join.bodyString());
            }

            System.out.println(sLogs.size() + ", 耗时：" + diffTime.diff() + " ms");
        }
    }


}
