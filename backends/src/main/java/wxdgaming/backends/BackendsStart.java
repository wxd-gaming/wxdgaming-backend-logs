package wxdgaming.backends;


import wxdgaming.boot2.core.CoreScan;
import wxdgaming.boot2.core.RunApplication;
import wxdgaming.boot2.starter.WxdApplication;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlScan;
import wxdgaming.boot2.starter.js.JsScan;
import wxdgaming.boot2.starter.net.SocketScan;
import wxdgaming.boot2.starter.scheduled.ScheduledScan;

import java.util.concurrent.TimeUnit;

/**
 * 后台管理系统
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-18 10:15
 */
public class BackendsStart {

    public static long ONLINE_TIME_DIFF = TimeUnit.MINUTES.toMillis(3);

    public static void main(String[] args) {
        RunApplication run = WxdApplication.run(
                CoreScan.class,
                ScheduledScan.class,
                SocketScan.class,
                PgsqlScan.class,
                JsScan.class,
                BackendsStart.class
        );

        // HttpListenerFactory httpListenerFactory = run.getInstance(HttpListenerFactory.class);
        // HttpListenerContent httpListenerContent = httpListenerFactory.getHttpListenerContent();
        // Collection<HttpMapping> values = httpListenerContent.getHttpMappingMap().values();
        // for (HttpMapping value : values) {
        //     System.out.println(value.path() + " - " + value.comment());
        // }
        //
        // Stream<Tuple2<Path, byte[]>> html = FileUtil.resourceStreams("html", ".html");
        // html.forEach(tuple2 -> {
        //     Path left = tuple2.getLeft();
        //     String pathString = left.toString();
        //     int indexOf = pathString.indexOf("html" + File.separator);
        //     if (indexOf < 0) {
        //         return;
        //     }
        //     pathString = "/" + pathString.substring(indexOf + 5);
        //     System.out.println(pathString);
        // });

    }

}