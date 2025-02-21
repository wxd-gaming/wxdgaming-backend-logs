package wxdgaming.backends.mudole.recharge.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.RechargeRecord;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ExecutorWith;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

/**
 * 充值接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-20 21:00
 **/
@Slf4j
@Singleton
@RequestMapping(path = "recharge")
public class RechargeApi {

    final GameService gameService;

    @Inject
    public RechargeApi(GameService gameService) {
        this.gameService = gameService;
    }

    @HttpRequest(authority = 2)
    @ExecutorWith(useVirtualThread = true)
    public RunResult push(HttpContext httpContext, @Body RechargeRecord record) {
        if (record.getUid() == 0) {
            record.setUid(gameService.newId(record.getGameId()));
        }
        if (record.getCreateTime() == 0) {
            record.setCreateTime(System.currentTimeMillis());
        }
        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(record.getGameId());
        RechargeRecord entity = pgsqlDataHelper.findByWhere(RechargeRecord.class, "uid=?", record.getUid());
        if (entity == null) {
            record.checkDataKey();
            pgsqlDataHelper.dataBatch().insert(record);
        } else {
            record.setUid(entity.getUid());
            record.setCreateTime(entity.getCreateTime());
            record.setDayKey(entity.getDayKey());
            pgsqlDataHelper.dataBatch().update(record);
        }
        return RunResult.ok().data(record);
    }

}
