package wxdgaming.backends.mudole.log.api;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.SLog;
import wxdgaming.backends.mudole.log.LogsService;
import wxdgaming.boot.core.str.json.FastJsonUtil;
import wxdgaming.boot.core.timer.MyClock;
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
@TextController(path = "/log")
public class LogController {

    LogsService logsService;
    PgsqlService psqlService;

    @Inject
    public LogController(LogsService logsService, PgsqlService psqlService) {
        this.logsService = logsService;
        this.psqlService = psqlService;
    }

    @TextMapping()
    public String push(HttpSession httpSession) {
        log.info("{}", httpSession.getReqContent());
        SLog sLog = FastJsonUtil.parse(httpSession.getReqContent(), SLog.class);
        if (sLog.getCreateTime() == 0) {
            sLog.setCreateTime(System.currentTimeMillis());
        }
        LocalDate localDate = MyClock.localDate(sLog.getCreateTime());
        sLog.setYear(localDate.getYear());
        sLog.setMonth(localDate.getMonthValue());
        sLog.setDay(localDate.getDayOfMonth());
        this.logsService.checkLogTable(sLog.getTableName());
        this.psqlService.getBatchPool().insert(sLog);
        return "ok";
    }

}
