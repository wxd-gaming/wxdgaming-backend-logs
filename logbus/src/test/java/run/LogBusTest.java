package run;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.LogBus;
import wxdgaming.boot2.core.RunApplication;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.threading.TimerJob;
import wxdgaming.boot2.core.util.RandomUtils;

import java.util.concurrent.TimeUnit;

/**
 * 日志上报测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-12 20:48
 **/
@Slf4j
public class LogBusTest {

    public static void test(RunApplication runApplication) {
        LogBus logBus = runApplication.getInstance(LogBus.class);

        logBus.addRoleLogType("role_copy_success", "副本通关");

        for (int i = 1; i <= 20; i++) {
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
            logBus.push("", "server/pushList", record);
        }

        for (int i = 0; i < 20; i++) {
            String account = StringUtils.randomString(6);
            /*创建账号*/
            logBus.registerAccount(account, MapOf.newJSONObject("os", "xiaomi"));
            int sid = RandomUtils.random(1, 20);
            long roleId = logBus.getHexId().newId();
            /*推送角色信息*/
            logBus.pushRole(
                    account, System.currentTimeMillis(),
                    sid, sid,
                    roleId, account,
                    "战士", "女", 1,
                    MapOf.newJSONObject("os", "xiaomi")
            );

            logBus.pushLogin(account, roleId, account, 1, MapOf.newJSONObject("os", "xiaomi"));

            /*同步在线状态*/
            TimerJob timerJob = ExecutorUtil.getInstance().getLogicExecutor().scheduleAtFixedDelay(
                    () -> logBus.online(account, roleId),
                    10,
                    10,
                    TimeUnit.SECONDS
            );

            /*2分钟之后下线*/
            ExecutorUtil.getInstance().getLogicExecutor().schedule(
                    () -> {
                        timerJob.cancel();
                        logBus.pushLogout(account, roleId, account, 1, MapOf.newJSONObject("os", "xiaomi"));
                    },
                    RandomUtils.random(2, 5),
                    TimeUnit.MINUTES
            );

            logBus.pushRoleLv(account, roleId, 2);

            /*充值日志*/
            logBus.pushRecharge(
                    account, roleId, account, 2,
                    "huawei", 600/*单位分*/, "1001", "1002",
                    MapOf.newJSONObject("comment", "首充奖励")
            );

            /*3星通关副本*/
            logBus.pushRoleLog(
                    "role_copy_success",
                    account, roleId, account, 1,
                    MapOf.newJSONObject("copyId", RandomUtils.random(1001, 1102))
                            .fluentPut("star", RandomUtils.random(1, 3))
            );
        }
    }

}
