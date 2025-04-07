package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import reactor.core.publisher.Mono;
import wxdgaming.boot2.core.collection.MapOf;
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
        List<Mono<Response<PostText>>> futures = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            JSONObject record = new JSONObject();
            record.fluentPut("uid", i);
            record.fluentPut("mainSid", 0);
            record.fluentPut("name", "测试服-" + i);
            record.fluentPut("showName", "测试服-" + i);
            record.fluentPut("openTime", "2025-01-24 14:02");
            record.fluentPut("maintainTime", "2025-01-24 14:02");
            record.fluentPut("wlan", "wxd-gaming");
            record.fluentPut("lan", "192.168.137.10");
            record.fluentPut("port", 19000);
            record.fluentPut("webPort", 19001);
            record.fluentPut("status", "online");
            record.fluentPut("other", MapOf.newJSONObject("version", "v1.0.1"));

            JSONObject push = new JSONObject()
                    .fluentPut("gameId", gameId)
                    .fluentPut("token", logToken)
                    .fluentPut("data", record);


            Mono<Response<PostText>> post = post("server/push", push.toJSONString());
            futures.add(post);
        }

        for (Mono<Response<PostText>> future : futures) {
            future.block();
        }

    }


}
