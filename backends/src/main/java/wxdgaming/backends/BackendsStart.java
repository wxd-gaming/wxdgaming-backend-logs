package wxdgaming.backends;


import org.graalvm.polyglot.Value;
import wxdgaming.backends.entity.games.GameStat;
import wxdgaming.boot2.core.CoreScan;
import wxdgaming.boot2.core.RunApplication;
import wxdgaming.boot2.core.io.FileUtil;
import wxdgaming.boot2.core.lang.Tuple2;
import wxdgaming.boot2.starter.WxdApplication;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlScan;
import wxdgaming.boot2.starter.js.JsScan;
import wxdgaming.boot2.starter.js.JsService;
import wxdgaming.boot2.starter.net.NetScan;
import wxdgaming.boot2.starter.net.server.http.HttpListenerContent;
import wxdgaming.boot2.starter.net.server.http.HttpListenerFactory;
import wxdgaming.boot2.starter.net.server.http.HttpMapping;
import wxdgaming.boot2.starter.scheduled.ScheduledScan;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;

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

        HttpListenerFactory httpListenerFactory = run.getInstance(HttpListenerFactory.class);
        HttpListenerContent httpListenerContent = httpListenerFactory.getHttpListenerContent();
        Collection<HttpMapping> values = httpListenerContent.getHttpMappingMap().values();
        for (HttpMapping value : values) {
            System.out.println(value.path() + " - " + value.comment());
        }

        Stream<Tuple2<Path, byte[]>> html = FileUtil.resourceStreams("html", ".html");
        html.forEach(tuple2 -> {
            Path left = tuple2.getLeft();
            String pathString = left.toString();
            int indexOf = pathString.indexOf("html" + File.separator);
            if (indexOf < 0) {
                return;
            }
            pathString = "/" + pathString.substring(indexOf + 5);
            System.out.println(pathString);
        });

    }

}