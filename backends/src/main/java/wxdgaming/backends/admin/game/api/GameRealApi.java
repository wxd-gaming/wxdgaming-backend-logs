package wxdgaming.backends.admin.game.api;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.entity.games.OnlineStat;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.ArrayList;
import java.util.Map;

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
        long registerAccountNum = gameContext.registerAccountNum(dayInt);
        long registerAccountRechargeNum = gameContext.registerAccountRechargeNum(dayInt);
        long loginAccountNum = gameContext.loginAccountNum(dayInt);
        long rechargeAccountNum = gameContext.rechargeAccountNum(dayInt);
        long amountNum = gameContext.rechargeAmountNum(dayInt);
        runResult.fluentPut("loginAccountNum", loginAccountNum);
        runResult.fluentPut("registerAccountNum", registerAccountNum);
        runResult.fluentPut("registerAccountRechargeNum", registerAccountRechargeNum);
        runResult.fluentPut("rechargeAccountNum", rechargeAccountNum);
        runResult.fluentPut("rechargeAmountNum", amountNum / 100);
        runResult.fluentPut("rechargeOrderNum", gameContext.rechargeOrderNum(dayInt));
        {
            String arpu = "0";
            if (loginAccountNum > 0 && amountNum > 0) {
                arpu = String.format("%.2f", amountNum / 100f / loginAccountNum);
            }
            runResult.fluentPut("arpu", arpu);
        }
        {
            String arppu = "0";
            if (loginAccountNum > 0 && amountNum > 0) {
                arppu = String.format("%.2f", amountNum / 100f / rechargeAccountNum);
            }
            runResult.fluentPut("arppu", arppu);
        }
        {
            /*今日注册账号付费数 / 今日注册账号数*/
            if (registerAccountNum == 0 || registerAccountRechargeNum == 0) {
                runResult.fluentPut("fufeilv", "0");
            } else {
                runResult.fluentPut("fufeilv", String.format("%.2f", ((float) registerAccountRechargeNum) / registerAccountNum * 100) + "%");
            }
        }

        OnlineStat onlineStat = gameContext.getDataHelper().findByKey(OnlineStat.class, dayInt);
        Map<Integer, Integer> array = Map.of();
        if (onlineStat != null) {
            array = onlineStat.getOnlineMap();
        }
        ArrayList<Integer> onlineHour = new ArrayList<>();
        for (int i = 0; i <= MyClock.getHour(); i++) {
            int num = array.getOrDefault(i, 0);
            onlineHour.add(num);
        }
        runResult.fluentPut("onlineHour", onlineHour);

        Object[] objects = gameContext.queryRechargeGroup(dayInt);
        runResult.fluentPut("rechargeGroup", objects);

        return runResult;
    }

}
