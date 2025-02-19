package push;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.logs.ServerRecord;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * test
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 14:02
 **/
@Slf4j
public class ServerApiTest extends GameApiTest {

    @Test
    public void pushServer() throws Exception {
        String logToken = findLogToken();
        pushServer(logToken, 1);
    }

    @Test
    public void pushServerList() throws Exception {
        String logToken = findLogToken();
        pushServer(logToken, 100);
    }

    public void pushServer(String loginToken, int count) throws Exception {
        List<CompletableFuture> futures = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ServerRecord serverRecord = new ServerRecord();
            serverRecord.setGameId(gameId);
            serverRecord.setSid(RandomUtils.random(1, 16000));
            serverRecord.setMainSid(0);
            serverRecord.setName("测试服");
            serverRecord.setShowName("测试服");
            serverRecord.setOpenTime("2025-01-24 14:02");
            serverRecord.setMaintainTime("2025-01-24 14:02");
            serverRecord.setWlan("wxd-gaming");
            serverRecord.setLan("192.168.137.10");
            serverRecord.setPort(19000);
            serverRecord.setWebPort(19001);
            serverRecord.setStatus("online");
            serverRecord.setVersion("1.0.0");
            serverRecord.setRegisterUserCount(RandomUtils.random(1, 1000));
            serverRecord.setRegisterRoleCount(RandomUtils.random(1, 1000));
            serverRecord.setOnlineRoleCount(RandomUtils.random(1, 1000));
            serverRecord.setActiveRoleCount(RandomUtils.random(1, 1000));
            serverRecord.setRechargeCount(RandomUtils.random(1, 1000));
            serverRecord.setUpdateTime(System.currentTimeMillis());
            serverRecord.setToken(loginToken);
            CompletableFuture<Response<PostText>> post = post("server/push", serverRecord.toJsonString());
            futures.add(post);
        }
        for (CompletableFuture future : futures) {
            future.join();
        }
    }


}
