package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import wxdgaming.backends.entity.games.logs.ServerRecord;
import wxdgaming.boot2.core.chatset.StringUtils;
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

    public void pushServer(String logToken, int count) throws Exception {
        List<CompletableFuture<Response<PostText>>> futures = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ServerRecord record = new ServerRecord();
            record.setSid(i);
            record.setMainSid(0);
            record.setName("测试服");
            record.setShowName(StringUtils.randomString(4));
            record.setOpenTime("2025-01-24 14:02");
            record.setMaintainTime("2025-01-24 14:02");
            record.setWlan("wxd-gaming");
            record.setLan("192.168.137.10");
            record.setPort(19000);
            record.setWebPort(19001);
            record.setStatus("online");
            record.setRegisterUserCount(RandomUtils.random(1, 1000));
            record.setRegisterRoleCount(RandomUtils.random(1, 1000));
            record.setOnlineRoleCount(RandomUtils.random(1, 1000));
            record.setActiveRoleCount(RandomUtils.random(1, 1000));
            record.setRechargeCount(RandomUtils.random(1, 1000));
            record.setUpdateTime(System.currentTimeMillis());
            record.getOther().fluentPut("version", "v1.0.1");

            JSONObject push = new JSONObject()
                    .fluentPut("gameId", gameId)
                    .fluentPut("token", logToken)
                    .fluentPut("data", record);


            CompletableFuture<Response<PostText>> post = post("server/push", push.toJSONString());
            futures.add(post);
        }

        for (CompletableFuture<Response<PostText>> future : futures) {
            future.join();
        }

    }


}
