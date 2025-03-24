package wxdgaming.backends.admin.game.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.OnlineStat;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.*;

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

    final GameService gameService;

    @Inject
    public GameRealApi(GameService gameService) {
        this.gameService = gameService;
    }

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

        {
            List<JSONObject> jsonObjects = gameContext.queryRechargeGroup(dayInt);
            jsonObjects.sort((o1, o2) -> Long.compare(o2.getLongValue("count"), o1.getLongValue("count")));

            Object[] objectsTitle = new Object[jsonObjects.size()];
            Object[] objectsValue = new Object[jsonObjects.size()];

            for (int i = 0; i < jsonObjects.size(); i++) {
                JSONObject jsonObject = jsonObjects.get(i);
                objectsTitle[i] = jsonObject.getIntValue("amount") / 100 + "(" + jsonObject.getIntValue("count") + ")";
                objectsValue[i] = jsonObject.getIntValue("count");
            }

            Object[] objects = new Object[]{objectsTitle, objectsValue};
            runResult.fluentPut("rechargeGroup", objects);
        }

        return runResult;
    }

    @HttpRequest(authority = 9)
    public RunResult queryAll(HttpContext session, @ThreadParam User user) {

        RunResult runResult = RunResult.ok();

        int dayInt = MyClock.dayInt();

        ArrayList<GameContext> gameContexts = new ArrayList<>(gameService.getGameContextHashMap().values());
        TreeMap<Integer, Integer> onlineHour = new TreeMap<>();
        HashMap<Integer, JSONObject> rechargeGroup = new HashMap<>();

        long onlineAccount = 0;
        long registerAccountNum = 0;
        long registerAccountRechargeNum = 0;
        long loginAccountNum = 0;
        long rechargeAccountNum = 0;
        long amountNum = 0;
        long rechargeOrderNum = 0;
        for (GameContext gameContext : gameContexts) {
            if (!user.checkAuthorGame(gameContext.getGameId())) continue;
            onlineAccount += gameContext.onlineAccount();
            registerAccountNum += gameContext.registerAccountNum(dayInt);
            registerAccountRechargeNum += gameContext.registerAccountRechargeNum(dayInt);
            loginAccountNum += gameContext.loginAccountNum(dayInt);
            rechargeAccountNum += gameContext.rechargeAccountNum(dayInt);
            amountNum += gameContext.rechargeAmountNum(dayInt);
            rechargeOrderNum += gameContext.rechargeOrderNum(dayInt);

            OnlineStat onlineStat = gameContext.getDataHelper().findByKey(OnlineStat.class, dayInt);
            Map<Integer, Integer> array = Map.of();
            if (onlineStat != null) {
                array = onlineStat.getOnlineMap();
            }

            for (int i = 0; i <= MyClock.getHour(); i++) {
                int num = array.getOrDefault(i, 0);
                onlineHour.merge(i, num, Math::addExact);
            }

            List<JSONObject> jsonObjects = gameContext.queryRechargeGroup(dayInt);
            for (JSONObject jsonObject : jsonObjects) {
                int amount = jsonObject.getIntValue("amount");
                int count = jsonObject.getIntValue("count");
                JSONObject cur = rechargeGroup.computeIfAbsent(amount, l -> new JSONObject().fluentPut("amount", amount));
                int oldCount = cur.getIntValue("count");
                cur.fluentPut("count", oldCount + count);
            }
        }


        runResult.fluentPut("onlineUser", onlineAccount);
        runResult.fluentPut("loginAccountNum", loginAccountNum);
        runResult.fluentPut("registerAccountNum", registerAccountNum);
        runResult.fluentPut("registerAccountRechargeNum", registerAccountRechargeNum);
        runResult.fluentPut("rechargeAccountNum", rechargeAccountNum);
        runResult.fluentPut("rechargeAmountNum", amountNum / 100);
        runResult.fluentPut("rechargeOrderNum", rechargeOrderNum);

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

        runResult.fluentPut("onlineHour", new ArrayList<>(onlineHour.values()));

        {
            ArrayList<JSONObject> rechargeGroupList = new ArrayList<>(rechargeGroup.values());
            rechargeGroupList.sort((o1, o2) -> Long.compare(o2.getLongValue("count"), o1.getLongValue("count")));

            Object[] objectsTitle = new Object[rechargeGroupList.size()];
            Object[] objectsValue = new Object[rechargeGroupList.size()];

            for (int i = 0; i < rechargeGroupList.size(); i++) {
                JSONObject jsonObject = rechargeGroupList.get(i);
                objectsTitle[i] = jsonObject.getIntValue("amount") / 100 + "(" + jsonObject.getIntValue("count") + ")";
                objectsValue[i] = jsonObject.getIntValue("count");
            }

            Object[] objects = new Object[]{objectsTitle, objectsValue};
            runResult.fluentPut("rechargeGroup", objects);
        }
        return runResult;
    }

}
