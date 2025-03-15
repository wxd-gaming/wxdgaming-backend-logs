package wxdgaming.backends;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.BootConfig;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.collection.LinkedTable;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.collection.SplitCollection;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.io.FileWriteUtil;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.httpclient.HttpBuilder;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.util.LinkedHashMap;
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
@Getter
@Singleton
public class LogBus {

    private LogBusConfig config;
    private LinkedTable<String, String, SplitCollection<JSONObject>> logMap = new LinkedTable<>();
    private HexId hexId;

    @Start
    public void init(@Value(path = "logbus") LogBusConfig config) {
        this.config = config;
        this.hexId = new HexId(this.config.getAppId());
        ExecutorUtil.getInstance().getLogicExecutor().scheduleAtFixedDelay(
                () -> {
                    LinkedTable<String, String, SplitCollection<JSONObject>> tmpMap;
                    synchronized (this) {
                        if (logMap.isEmpty()) return;
                        tmpMap = logMap;
                        logMap = new LinkedTable<>();
                    }
                    for (Map.Entry<String, LinkedHashMap<String, SplitCollection<JSONObject>>> mapEntry : tmpMap.entrySet()) {
                        String queueKey = mapEntry.getKey();
                        for (Map.Entry<String, SplitCollection<JSONObject>> entry : mapEntry.getValue().entrySet()) {
                            ExecutorUtil.getInstance().getVirtualExecutor().submit(
                                    queueKey,
                                    () -> {
                                        SplitCollection<JSONObject> value = entry.getValue();
                                        while (!value.isEmpty()) {
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
                    }
                },
                3_000,
                33,
                TimeUnit.MILLISECONDS
        );
    }

    public void addRoleLogType(String logType, String logComment) {
        JSONObject postData = MapOf.newJSONObject();
        postData.put("gameId", config.getAppId());
        postData.put("token", config.getAppToken());
        postData.put("logType", logType);
        postData.put("logComment", logComment);
        String uriPath = config.getPortUrl() + "/game/addRoleLogType";
        try {
            Response<PostText> request = HttpBuilder.postJson(uriPath, postData.toJSONString())
                    .readTimeout(130000)
                    .retry(2)
                    .request();
            log.info("logbus push {} result {}", uriPath, request.bodyString());
        } catch (Exception e) {
            log.error("logbus push {} fail", uriPath, e);
        }
    }

    public void addServerLogType(String logType, String logComment) {
        JSONObject postData = MapOf.newJSONObject();
        postData.put("gameId", config.getAppId());
        postData.put("token", config.getAppToken());
        postData.put("logType", logType);
        postData.put("logComment", logComment);
        String uriPath = config.getPortUrl() + "/game/addServerLogType";
        try {
            Response<PostText> request = HttpBuilder.postJson(uriPath, postData.toJSONString())
                    .readTimeout(130000)
                    .retry(2)
                    .request();
            log.info("logbus push {} result {}", uriPath, request.bodyString());
        } catch (Exception e) {
            log.error("logbus push {} fail", uriPath, e);
        }
    }

    /** 创建一个账号 */
    public void registerAccount(String account, JSONObject other) {
        registerAccount(account, System.currentTimeMillis(), other);
    }

    /** 创建一个账号 */
    public void registerAccount(String account, long createTime, JSONObject other) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("account", account)
                .fluentPut("createTime", createTime)
                .fluentPut("other", other);
        push(account, "log/account/pushList", jsonObject);
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

        push(account, "log/role/pushList", record);
    }

    /** 修改角色等级 */
    public void pushRoleLv(String account, long roleId, int lv) {

        JSONObject record = new JSONObject();
        record.put("account", account);
        record.put("roleId", roleId);
        record.put("lv", lv);

        push(account, "log/role/lvList", record);
    }

    public void pushLogin(String account, long roleId, String roleName, int lv, JSONObject other) {
        JSONObject jsonObject = MapOf.newJSONObject()
                .fluentPut("logEnum", "LOGIN")
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", System.currentTimeMillis())
                .fluentPut("sid", BootConfig.getIns().sid())
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("other", other);
        push(account, "log/role/login/pushList", jsonObject);
    }

    /** 同步在线状态 建议每分钟一次 */
    public void online(String account, long roleId) {
        JSONObject jsonObject = MapOf.newJSONObject()
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("time", System.currentTimeMillis());
        push(account, "log/role/login/onlineList", jsonObject);
    }

    public void pushLogout(String account, long roleId, String roleName, int lv, JSONObject other) {
        JSONObject jsonObject = MapOf.newJSONObject()
                .fluentPut("logEnum", "LOGOUT")
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", System.currentTimeMillis())
                .fluentPut("sid", BootConfig.getIns().sid())
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("other", other);
        push(account, "log/role/login/pushList", jsonObject);
    }

    public void pushRecharge(String account, long roleId, String roleName, int lv,
                             String channel, long amount, String spOrder, String cpOrder,
                             JSONObject other) {
        JSONObject jsonObject = MapOf.newJSONObject()
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", System.currentTimeMillis())
                .fluentPut("sid", BootConfig.getIns().sid())
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("channel", channel)
                .fluentPut("amount", amount)
                .fluentPut("spOrder", spOrder)
                .fluentPut("cpOrder", cpOrder)
                .fluentPut("other", other);
        push(account, "log/recharge/pushList", jsonObject);
    }

    /**
     * 根据角色相关的通用的日志
     *
     * @param logType  日志类型
     * @param account  玩家账号
     * @param roleId   玩家的id
     * @param roleName 玩家名字
     * @param lv       玩家等级
     * @param other    附加的信息
     * @author: wxd-gaming(無心道, 15388152619)
     * @version: 2025-03-13 09:03
     */
    public void pushRoleLog(String logType, String account, long roleId, String roleName, int lv, JSONObject other) {
        JSONObject jsonObject = MapOf.newJSONObject()
                .fluentPut("logType", logType)
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", System.currentTimeMillis())
                .fluentPut("sid", BootConfig.getIns().sid())
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("other", other);
        push(account, "log/role/slog/pushList", jsonObject);
    }

    /**
     * @param account     账号
     * @param roleId      角色id
     * @param roleName    角色名称
     * @param lv          角色等级
     * @param changeType  变更类型，是获得还是消耗
     * @param itemId      道具第
     * @param itemBind    是否绑定
     * @param itemCount   当前数量
     * @param change      本次变更的数量
     * @param itemType    道具类型
     * @param itemSubType 道具子类型
     * @param source      来源。比如是某系统
     * @param comment     备注。比如来源是任务系统，具体的任务id
     * @param other       其它备注的记录，比如交易，谁交易给谁
     * @author: wxd-gaming(無心道, 15388152619)
     * @version: 2025-03-12 20:38
     */
    public void pushRoleItem(String account, long roleId, String roleName, int lv,
                             ChangeTypeEnum changeType,
                             int itemId, String itemName, boolean itemBind, long itemCount, long change,
                             String itemType, String itemSubType,
                             String source, String comment,
                             JSONObject other) {
        JSONObject jsonObject = MapOf.newJSONObject()
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", System.currentTimeMillis())
                .fluentPut("sid", BootConfig.getIns().sid())
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("changeType", changeType)
                .fluentPut("itemId", itemId)
                .fluentPut("itemName", itemName)
                .fluentPut("itemBind", itemBind)
                .fluentPut("itemCount", itemCount)
                .fluentPut("change", change)
                .fluentPut("itemType", itemType)
                .fluentPut("itemSubType", itemSubType)
                .fluentPut("source", source)
                .fluentPut("comment", comment)
                .fluentPut("other", other);
        push(account, "log/role/item/pushList", jsonObject);
    }


    public void push(String key, String url, JSONObject data) {
        synchronized (this) {
            key = String.valueOf(key.hashCode() % 20);
            key = "";
            SplitCollection<JSONObject> collection = logMap.computeIfAbsent(key, url, k -> new SplitCollection<>(config.getBatchSize()));
            collection.add(data);
        }
    }

    public enum ChangeTypeEnum {
        /** 获取 */
        Get,
        /** 消耗 */
        COST
    }
}
