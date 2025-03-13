package wxdgaming.backends;

import wxdgaming.boot2.core.CoreScan;
import wxdgaming.boot2.core.RunApplication;
import wxdgaming.boot2.core.util.JvmUtil;
import wxdgaming.boot2.starter.WxdApplication;

/**
 * 启动器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-11 20:09
 **/
public class LogMain {

    public static void main(String[] args) {
        launch();
    }

    public static RunApplication launch() {
        return launch("log-boot.yml");
    }

    public static RunApplication launch(String configName) {
        JvmUtil.setProperty("boot.config", configName);
        return WxdApplication.run(
                CoreScan.class,
                LogMain.class
        );
    }

}
