package wxdgaming.backends.admin.game.api;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

/**
 * 实时大盘查询
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-14 09:41
 **/
@Slf4j
@Singleton
@RequestMapping(path = "game/real")
public class GameRealApi {

    @HttpRequest(authority = 9)
    public RunResult query(HttpContext session,
                           @ThreadParam User user,
                           @ThreadParam GameContext gameContext) {

        RunResult runResult = RunResult.ok();

        int dayInt = MyClock.dayInt();

        runResult.fluentPut("onlineUser", gameContext.onlineAccount());
        runResult.fluentPut("loginAccountNum", gameContext.loginAccountNum(dayInt));
        runResult.fluentPut("registerAccountNum", gameContext.registerAccountNum(dayInt));
        runResult.fluentPut("registerAccountRechargeNum", gameContext.registerAccountRechargeNum(dayInt));
        runResult.fluentPut("rechargeAccountNum", gameContext.rechargeAccountNum(dayInt));
        runResult.fluentPut("rechargeAmountNum", gameContext.rechargeAmountNum(dayInt));
        runResult.fluentPut("rechargeOrderNum", gameContext.rechargeOrderNum(dayInt));

        return runResult;
    }

}
