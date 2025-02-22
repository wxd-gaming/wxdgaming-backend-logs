package push;

import org.junit.Test;
import wxdgaming.backends.entity.games.logs.AccountRecord;
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
import java.util.concurrent.CompletableFuture;

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
        HashMap<Integer, List<AccountRecord>> hashMap = readAccount();
        for (List<AccountRecord> recordList : hashMap.values()) {
            for (AccountRecord accountRecord : recordList) {
                test(logToken, accountRecord);
            }
        }
    }

    public void test(String logToken, AccountRecord accountRecord) throws Exception {
        List<CompletableFuture<Response<PostText>>> futures = new ArrayList<>();

        long createTime = accountRecord.getCreateTime();
        LocalDateTime localDateTime = MyClock.localDateTime(createTime);

        DiffTime diffTime = new DiffTime();

        for (int i = 1; i <= days; i++) {

            LocalDateTime plusDays = localDateTime.plusDays(i);
            long milli = MyClock.time2Milli(plusDays);
            if (milli > MyClock.millis()) continue;/*当前当前时间不在执行*/
            boolean randomBoolean = RandomUtils.randomBoolean();
            if (!randomBoolean) continue;

            RechargeRecord record = new RechargeRecord();
            record.setGameId(gameId);
            record.setToken(logToken);
            record.setUid(hexId.newId());
            record.setAccount(accountRecord.getAccount());
            record.setSid(RandomUtils.random(1, 100));
            record.setCreateTime(milli);
            record.setRoleId(String.valueOf(i));
            record.setRoleName(StringUtils.randomString(8));
            record.setLv(RandomUtils.random(1, 100));
            record.setChannel("huawei");
            record.setAmount(RandomUtils.random(6, 1000) * 100);
            record.setSpOrder(StringUtils.randomString(32));
            record.setCpOrder(StringUtils.randomString(32));
            record.getData().fluentPut("充值ID", "1");
            record.getData().fluentPut("充值商品", "首充武器");
            CompletableFuture<Response<PostText>> completableFuture = post("recharge/push", record.toJsonString());
            futures.add(completableFuture);
        }
        for (CompletableFuture<Response<PostText>> future : futures) {
            Response<PostText> join = future.join();
            RunResult runResult = join.bodyRunResult();
            if (join.responseCode() != 200 || runResult.code() != 1) {
                System.out.println(join.bodyString());
            }
        }

        System.out.println(futures.size() + ", 耗时：" + diffTime.diff() + " ms");
    }


}
