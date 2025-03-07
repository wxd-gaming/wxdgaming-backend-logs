package wxdgaming.backends;


import org.graalvm.polyglot.Value;
import wxdgaming.backends.entity.games.GameStat;
import wxdgaming.boot2.core.CoreScan;
import wxdgaming.boot2.core.RunApplication;
import wxdgaming.boot2.starter.WxdApplication;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlScan;
import wxdgaming.boot2.starter.js.JsScan;
import wxdgaming.boot2.starter.js.JsService;
import wxdgaming.boot2.starter.net.NetScan;
import wxdgaming.boot2.starter.scheduled.ScheduledScan;

/**
 * 后台管理系统
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-18 10:15
 */
public class BackendsStart {

    public static void main(String[] args) {
        RunApplication run = WxdApplication.run(
                CoreScan.class,
                ScheduledScan.class,
                NetScan.class,
                PgsqlScan.class,
                JsScan.class,
                BackendsStart.class
        );

        JsService jsService = run.getInstance(JsService.class);
        jsService.evalFile("script/game-stat.js");
        GameStat gameStat = new GameStat();
        Value execute = jsService.threadContext().execute("GameStat", 2, gameStat);
        System.out.println(gameStat.getArpu());
    }

}