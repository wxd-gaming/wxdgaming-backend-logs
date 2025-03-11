package wxdgaming.backends;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.Throw;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.collection.SplitCollection;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.io.FileWriteUtil;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.HttpBuilder;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 日志载体
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-11 20:15
 **/
@Slf4j
@Singleton
public class LogBus {

    private LogBusConfig config;
    private HashMap<String, SplitCollection<JSONObject>> logMap = new HashMap<>();
    private HexId hexId;

    @Start
    public void init(@Value(path = "logbus") LogBusConfig config) {
        this.config = config;
        this.hexId = new HexId(this.config.getAppId());
        ExecutorUtil.getInstance().getLogicExecutor().scheduleAtFixedDelay(
                () -> {
                    HashMap<String, SplitCollection<JSONObject>> tmpMap;
                    synchronized (this) {
                        if (logMap.isEmpty()) return;
                        tmpMap = logMap;
                        logMap = new HashMap<>();
                    }
                    for (Map.Entry<String, SplitCollection<JSONObject>> entry : tmpMap.entrySet()) {
                        ExecutorUtil.getInstance().getLogicExecutor().submit(
                                entry.getKey(),
                                () -> {
                                    SplitCollection<JSONObject> value = entry.getValue();
                                    while (value.isEmpty()) {
                                        List<JSONObject> jsonObjects = value.removeFirst();
                                        JSONObject postData = MapOf.newJSONObject();
                                        postData.put("gameId", config.getAppId());
                                        postData.put("token", config.getLogToken());
                                        postData.put("data", jsonObjects);
                                        try {
                                            Response<PostText> request = HttpBuilder.postJson(config.getPortUrl() + "/" + entry.getKey(), postData.toJSONString())
                                                    .readTimeout(130000)
                                                    .retry(2)
                                                    .request();
                                            if (request.responseCode() != 200) {
                                                log.info("logbus push {} fail", entry.getKey());
                                            }
                                        } catch (Exception e) {
                                            String errorPath = "target/post/" + MyClock.formatDate("yyyy/MM/DD/HH") + "/error-" + System.nanoTime() + StringUtils.randomString(4) + ".log";
                                            log.error("logbus push {} fail error log file {}", entry.getKey(), errorPath, e);
                                            FileWriteUtil.writeString(errorPath, "%s\n%s\n\nError:\n%s".formatted(entry.getKey(), postData.toString(), Throw.ofString(e)));
                                        }
                                    }
                                }
                        );
                    }
                },
                10_000,
                33,
                TimeUnit.MILLISECONDS
        );
    }

    public void pushAccount(String account, JSONObject other) {
        pushAccount(hexId.newId(), account, System.currentTimeMillis(), other);
    }

    public void pushAccount(long uid, String account, long createTime, JSONObject other) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("uid", uid)
                .fluentPut("account", account)
                .fluentPut("createTime", createTime)
                .fluentPut("other", other);
        push("log/account/pushList", jsonObject);
    }

    public void pushRole() {
        JSONObject record = new JSONObject();
        record.put("uid", 1);
        record.put("account", "xxxx");
        record.put("createTime", System.currentTimeMillis());
        record.put("createSid", RandomUtils.random(1, 100));
        record.put("curSid", RandomUtils.random(1, 100));
        record.put("roleName", StringUtils.randomString(8));
        record.put("Job", "魔剑士");
        record.put("sex", "男");
        record.put("lv", RandomUtils.random(1, 100));
        record.put("other", new JSONObject().fluentPut("channel", "huawei"));/*附加参数，本身也是一个json*/

        push("log/role/pushList", record);
    }

    public void push(String url, JSONObject data) {
        synchronized (this) {
            SplitCollection<JSONObject> collection = logMap.computeIfAbsent(url, k -> new SplitCollection<>(config.getBatchSize()));
            collection.add(data);
        }
    }

}
