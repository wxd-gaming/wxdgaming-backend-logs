package push;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * test
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 14:02
 **/
@Slf4j
public class AccountApiTest extends GameApiTest {

    @Test
    public void pushAccount() throws Exception {
        String logToken = findLogToken();
        pushAccount(logToken, 1);
    }

    @Test
    @RepeatedTest(100)
    public void pushAccountList() throws Exception {
        String logToken = findLogToken();
        pushAccount(logToken, 1000);
    }

    public void pushAccount(String logToken, int count) throws Exception {
        List<CompletableFuture<Response<PostText>>> futures = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            AccountRecord record = new AccountRecord();
            record.setGameId(gameId);
            record.setToken(logToken);
            record.setCreateTime(randomCreateTime());
            record.setAccount(randomAccount());
            record.setLastJoinSid(RandomUtils.random(1, 100));
            record.setLastJoinTime(System.currentTimeMillis());
            record.getData().fluentPut("channel", "huawei").fluentPut("os", "huawei");
            CompletableFuture<Response<PostText>> post = post("account/push", record.toJsonString());
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
    @RepeatedTest(10)
    public void random() {
        int random = RandomUtils.random(0, 120);
        random = random - 120;
        LocalDateTime localDateTime = LocalDateTime.now().plusDays(random);
        System.out.println(localDateTime);
    }

}
