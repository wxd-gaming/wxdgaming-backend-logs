package push;

import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 角色api操作
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 11:15
 **/
public class RoleApiTest extends AccountApiTest {

    protected String roleName = "无心道";

    @Test
    public void test() throws Exception {
        String logToken = findLogToken();
        test(logToken, 1);
    }

    @Test
    @RepeatedTest(99)
    public void testList() throws Exception {
        String logToken = findLogToken();
        test(logToken, 1000);
    }

    public void test(String logToken, int count) throws Exception {
        List<CompletableFuture<Response<PostText>>> futures = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            RoleRecord record = new RoleRecord();
            record.setGameId(gameId);
            record.setToken(logToken);
            record.setAccount(randomAccount());
            record.setCreateTime(randomCreateTime());
            record.setCreateSid(RandomUtils.random(1, 100));
            record.setCurSid(RandomUtils.random(1, 100));
            record.setRoleId(String.valueOf(i));
            record.setRoleName(StringUtils.randomString(8));
            record.setJob("魔剑士");
            record.setSex("男");
            record.setLv(RandomUtils.random(1, 100));
            record.getData().fluentPut("channel", "huawei");
            CompletableFuture<Response<PostText>> completableFuture = post("role/push", record.toJsonString());
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
