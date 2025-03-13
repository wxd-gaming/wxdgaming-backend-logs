package run;

import wxdgaming.backends.LogBus;
import wxdgaming.backends.LogMain;
import wxdgaming.boot2.core.RunApplication;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.collection.MapOf;

/**
 * 日志上报测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-12 20:48
 **/
public class LogBusTest {

    public static void main(String[] args) {
        RunApplication launch = LogMain.launch();
        LogBus logBus = launch.getInstance(LogBus.class);

        logBus.addRoleLogType("role_copy_success", "副本通关");

        String account = StringUtils.randomString(6);

        logBus.registerAccount(account, System.currentTimeMillis(), MapOf.newJSONObject("os", "xiaomi"));

        long roleId = logBus.getHexId().newId();
        logBus.pushRole(
                account, System.currentTimeMillis(),
                1, 1,
                roleId, account,
                "战士", "女", 1,
                MapOf.newJSONObject("os", "xiaomi")
        );

        /*3星通关副本*/
        logBus.pushRoleLog(
                "role_copy_success", logBus.getHexId().newId(),
                System.currentTimeMillis(),
                1,
                account, roleId, account, 1,
                MapOf.newJSONObject("copyId", "1001")
                        .fluentPut("star", 3)
        );

    }

}
