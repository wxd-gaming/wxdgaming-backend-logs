package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.logs.SLog;
import wxdgaming.boot.core.collection.MapOf;
import wxdgaming.boot.core.format.HexId;
import wxdgaming.boot.core.lang.RandomUtils;
import wxdgaming.boot.core.str.StringUtil;
import wxdgaming.boot.httpclient.apache.HttpBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志push
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-23 09:26
 **/
@Slf4j
public class LogPushTest extends RoleApiTest {

    static HexId hexId = new HexId(1);

    @Test
    @RepeatedTest(590000)
    public void pushItemLog() {
        AtomicInteger forCount = new AtomicInteger(50);
        int ff = forCount.get();
        for (int i = 0; i < ff; i++) {

            // List<String> strings = List.of("item_log");
            SLog sLog = new SLog();
            sLog.setGameId(gameId);
            sLog.setToken(logToken);
            sLog.setLogType("log_item");
            sLog.setUid(hexId.newId());
            sLog.setLogTime(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(RandomUtils.random(0, 45)));
            sLog.setAccount(StringUtil.getRandomString(8));
            sLog.setRoleId(String.valueOf(RandomUtils.random(1, 1000)));
            sLog.setRoleName(StringUtil.getRandomString(8));
            sLog.setMainId(1);
            sLog.setSId(1);
            sLog.getData().fluentPut("a", "b")
                    .fluentPut("itemId", RandomUtils.random(1, 100))
                    .fluentPut("itemNum", RandomUtils.random(1, 100))
                    .fluentPut("item_name", "货币")
                    .fluentPut("bind", "true");

            String json = sLog.toJson();
            // log.info("{}", json);
            // HttpBuilder.postJson("http://127.0.0.1:19000/log/push", json).request().bodyString();
            // forCount.decrementAndGet();

            HttpBuilder.postJson("http://127.0.0.1:19000/log/push", json)
                    .retry(2)
                    .async()
                    .whenComplete((resp, throwable) -> {
                        forCount.decrementAndGet();
                        System.out.println(resp.bodyString());
                    });
        }
        while (forCount.get() > 0) {}
    }

    @Test
    public void convert() {
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject.put("a", "b");
        String javaObject = jsonObject.toJavaObject(String.class);
        log.info("{}", javaObject);
    }

}
