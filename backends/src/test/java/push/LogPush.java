package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.logs.SLog;
import wxdgaming.boot.core.collection.MapOf;
import wxdgaming.boot.core.format.HexId;
import wxdgaming.boot.core.lang.RandomUtils;
import wxdgaming.boot.httpclient.apache.HttpBuilder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志push
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-23 09:26
 **/
@Slf4j
public class LogPush {

    static HexId hexId = new HexId(1);

    @Test
    @RepeatedTest(10)
    public void push() {
        AtomicInteger forCount = new AtomicInteger(100);
        int ff = forCount.get();
        for (int i = 0; i < ff; i++) {
            List<String> strings = List.of("item_log", "login_log", "pay_log");
            // List<String> strings = List.of("item_log");
            String logTableName = RandomUtils.randomItem(strings);
            SLog sLog = new SLog();
            sLog.setLogType(logTableName);
            sLog.setUid(hexId.newId());
            sLog.setCreateTime(System.currentTimeMillis());
            sLog.setAccount("test");
            sLog.setRoleId(1);
            sLog.setRoleName("1");
            sLog.setGameId(1);
            sLog.setMainId(1);
            sLog.setSId(1);
            sLog.setData(MapOf.newJSONObject("a", "b").fluentPut("itemId", 111).fluentPut("itemNum", 111).fluentPut("item_name", "金币").fluentPut("bind", "true"));

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
