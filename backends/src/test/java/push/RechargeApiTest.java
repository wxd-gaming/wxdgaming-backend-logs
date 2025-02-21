package push;

import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.games.logs.RechargeRecord;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.util.ArrayList;
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
    public void test() throws Exception {
        String logToken = findLogToken();
        test(logToken, 1);
    }

    @Test
    @RepeatedTest(9999)
    public void testList() throws Exception {
        String logToken = findLogToken();
        test(logToken, 1000);
    }

    public void test(String logToken, int count) throws Exception {
        List<CompletableFuture<Response<PostText>>> futures = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            RechargeRecord record = new RechargeRecord();
            record.setGameId(gameId);
            record.setToken(logToken);
            record.setAccount(randomAccount());
            record.setSid(RandomUtils.random(1, 100));
            record.setCreateTime(randomCreateTime());
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
    }


}
