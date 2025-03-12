package wxdgaming.backends;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.collection.SplitCollection;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.io.FileWriteUtil;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.timer.MyClock;
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
                                        String uriPath = config.getPortUrl() + "/" + entry.getKey();
                                        try {
                                            Response<PostText> request = HttpBuilder.postJson(uriPath, postData.toJSONString())
                                                    .readTimeout(130000)
                                                    .retry(2)
                                                    .request();
                                            if (request.responseCode() != 200) {
                                                log.info("logbus push {} fail", entry.getKey());
                                            }
                                        } catch (Exception e) {
                                            String errorPath = "target/post/" + MyClock.formatDate("yyyy/MM/DD/HH") + "/error-" + System.nanoTime() + StringUtils.randomString(4) + ".log";
                                            log.error("logbus push {} fail error log file {}", uriPath, errorPath, e);
                                            FileWriteUtil.writeString(errorPath, "%s\n%s".formatted(uriPath, postData.toString()));
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

    /** 创建一个账号 */
    public void pushAccount(String account, JSONObject other) {
        pushAccount(hexId.newId(), account, System.currentTimeMillis(), other);
    }

    /** 创建一个账号 */
    public void pushAccount(long uid, String account, long createTime, JSONObject other) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("uid", uid)
                .fluentPut("account", account)
                .fluentPut("createTime", createTime)
                .fluentPut("other", other);
        push("log/account/pushList", jsonObject);
    }

    /** 创建角色或者修改角色数据 */
    public void pushRole(String account, long createTime, int sid, int curSid, long roleId, String roleName, String job, String sex, int lv, JSONObject other) {

        JSONObject record = new JSONObject();
        record.put("uid", roleId);
        record.put("account", account);
        record.put("createTime", createTime);
        record.put("createSid", sid);
        record.put("curSid", curSid);
        record.put("roleName", roleName);
        record.put("Job", job);
        record.put("sex", sex);
        record.put("lv", lv);
        record.put("other", other);/*附加参数，本身也是一个json*/

        push("log/role/pushList", record);
    }

    /** 修改角色等级 */
    public void pushRoleLv(long roleId, int lv) {

        JSONObject record = new JSONObject();
        record.put("roleId", roleId);
        record.put("lv", lv);

        push("log/role/lvList", record);
    }

    public void pushLogin(long uid, long createTime, int sid, String account, long roleId, String roleName, int lv, JSONObject other) {
        JSONObject jsonObject = MapOf.newJSONObject()
                .fluentPut("logEnum", "LOGIN")
                .fluentPut("uid", uid)/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", createTime)
                .fluentPut("sid", sid)
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("other", other);
        push("log/role/login/pushList", jsonObject);
    }

    public void pushLogout(long uid, long createTime, int sid, String account, long roleId, String roleName, int lv, JSONObject other) {
        JSONObject jsonObject = MapOf.newJSONObject()
                .fluentPut("logEnum", "LOGOUT")
                .fluentPut("uid", uid)/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", createTime)
                .fluentPut("sid", sid)
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("other", other);
        push("log/role/login/pushList", jsonObject);
    }

    public void pushRecharge(long uid, long createTime,
                             int sid, String account, long roleId, String roleName, int lv,
                             String channel, long amount, String spOrder, String cpOrder,
                             JSONObject other) {
        JSONObject jsonObject = MapOf.newJSONObject()
                .fluentPut("uid", uid)/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", createTime)
                .fluentPut("sid", sid)
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("channel", channel)
                .fluentPut("amount", amount)
                .fluentPut("spOrder", spOrder)
                .fluentPut("cpOrder", cpOrder)
                .fluentPut("other", other);
        push("log/recharge/pushList", jsonObject);
    }

    public void push(String url, JSONObject data) {
        synchronized (this) {
            SplitCollection<JSONObject> collection = logMap.computeIfAbsent(url, k -> new SplitCollection<>(config.getBatchSize()));
            collection.add(data);
        }
    }

}
