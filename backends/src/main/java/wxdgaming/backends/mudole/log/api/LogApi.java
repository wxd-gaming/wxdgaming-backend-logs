package wxdgaming.backends.mudole.log.api;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.SLog;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.backends.mudole.log.LogsService;
import wxdgaming.boot.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot.core.str.StringUtil;
import wxdgaming.boot.core.threading.ThreadInfo;
import wxdgaming.boot.core.timer.MyClock;
import wxdgaming.boot.net.controller.ann.Body;
import wxdgaming.boot.net.controller.ann.TextController;
import wxdgaming.boot.net.controller.ann.TextMapping;
import wxdgaming.boot.net.web.hs.HttpSession;
import wxdgaming.boot.starter.pgsql.PgsqlService;

import java.time.LocalDate;

/**
 * 日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 16:54
 **/
@Slf4j
@TextController(path = "log")
public class LogApi {

    final GameService gameService;
    final LogsService logsService;

    @Inject
    public LogApi(GameService gameService, LogsService logsService) {
        this.gameService = gameService;
        this.logsService = logsService;
    }

    @TextMapping()
    @ThreadInfo(vt = true)
    public String push(HttpSession httpSession, @Body SLog sLog) {

        log.info("sLog - {}", sLog.toJson());

        if (sLog.getGameId() == 0) return "gameId is null";
        if (StringUtil.emptyOrNull(sLog.getToken())) return "token is null";

        PgsqlDataHelper pgsqlDataHelper = this.gameService.pgsqlDataHelper(sLog.getGameId());

        if (sLog.getUid() == 0)
            sLog.setUid(gameService.newId(sLog.getGameId()));

        if (sLog.getLogTime() == 0) {
            sLog.setLogTime(System.currentTimeMillis());
        }

        LocalDate localDate = MyClock.localDate(sLog.getLogTime());
        sLog.setDayKey(localDate.getYear() * 10000 + localDate.getMonthValue() * 100 + localDate.getDayOfMonth());
        pgsqlDataHelper.getBatchPool().insert(sLog);
        return "ok";
    }

}
