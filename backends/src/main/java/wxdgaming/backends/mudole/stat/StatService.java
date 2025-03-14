package wxdgaming.backends.mudole.stat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.AccountStat;
import wxdgaming.backends.entity.games.GameStat;
import wxdgaming.backends.entity.games.OnlineStat;
import wxdgaming.backends.entity.games.ServerOnlineStat;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.games.logs.ServerRecord;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统计信息
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-21 20:34
 **/
@Slf4j
@Singleton
public class StatService {

    final GameService gameService;

    @Inject
    public StatService(GameService gameService) {
        this.gameService = gameService;
    }

    @Scheduled("0 */20")
    public void gameStat() {
        Collection<GameContext> values = gameService.getGameContextHashMap().values();
        final long dayOfStartMillis = MyClock.dayOfStartMillis();
        int days = 50;
        LocalDateTime localDateTime = LocalDateTime.now().plusDays(-days);
        long startTime = MyClock.time2Milli(localDateTime);
        for (GameContext gameContext : values) {
            Game game = gameContext.getGame();
            PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
            /*统计留存开始时间*/
            AtomicLong statTime = new AtomicLong(startTime);
            if (statTime.get() < game.getCreateTime()) {
                statTime.set(game.getCreateTime());
            }
            Event event = new Event(TimeUnit.HOURS.toMillis(2), TimeUnit.HOURS.toMillis(2)) {
                @Override public void onEvent() throws Exception {
                    /*统计留存开始时间*/
                    for (int i = 0; i < days; i++) {
                        final int day = i;
                        LocalDateTime statLocalDateTime = MyClock.localDateTime(statTime.get()).plusDays(day);
                        if (MyClock.time2Milli(statLocalDateTime) > MyClock.dayOfEndMillis()) {
                            /*TODO 已经大于当前时间，说明是明天了，不在处理*/
                            break;
                        }
                        int dayKey = (statLocalDateTime.getYear() * 10000 + statLocalDateTime.getMonthValue() * 100 + statLocalDateTime.getDayOfMonth());
                        GameStat gameStat = pgsqlDataHelper.findByKey(GameStat.class, dayKey);
                        if (gameStat == null) {
                            gameStat = new GameStat();
                            gameStat.setUid(dayKey);
                        }
                        {
                            Long registerAccountNum = pgsqlDataHelper.executeScalar("SELECT \"count\"(DISTINCT ll.account) FROM record_account as ll WHERE ll.daykey = ?;", Long.class, dayKey);
                            registerAccountNum = Objects.returnNonNull(registerAccountNum, 0L);
                            gameStat.setRegisterAccountNum(registerAccountNum);
                        }
                        {
                            /*今日登录的账号数*/
                            Long loginAccountNum = pgsqlDataHelper.executeScalar("SELECT \"count\"(DISTINCT account) FROM record_role_login WHERE daykey=?", Long.class, dayKey);
                            loginAccountNum = Objects.returnNonNull(loginAccountNum, 0L);
                            gameStat.setLoginAccountNum(loginAccountNum);
                        }
                        {
                            Long rechargeAmountNum = pgsqlDataHelper.executeScalar(
                                    """
                                            SELECT
                                            	"sum" ( amount )\s
                                            FROM
                                            	record_recharge rr
                                            	RIGHT JOIN ( SELECT "min" ( uid ) AS "uid" FROM record_recharge WHERE daykey = ? GROUP BY sporder ) rrt ON rr.uid = rrt.uid
                                            """,
                                    Long.class,
                                    dayKey
                            );
                            rechargeAmountNum = Objects.returnNonNull(rechargeAmountNum, 0L);
                            gameStat.setRechargeAmountNum(rechargeAmountNum);
                        }
                        {
                            /*今日充值的账号数*/
                            Long rechargeAccountNum = pgsqlDataHelper.executeScalar("SELECT \"count\"(DISTINCT account) FROM record_recharge WHERE daykey=?", Long.class, dayKey);
                            rechargeAccountNum = Objects.returnNonNull(rechargeAccountNum, 0L);
                            gameStat.setRechargeAccountNum(rechargeAccountNum);
                        }
                        {
                            /*今天注册就充值的账号数量*/
                            Long registerAccountRechargeNum = pgsqlDataHelper.executeScalar(
                                    """
                                            SELECT
                                            	"count" ( DISTINCT rr.account )\s
                                            FROM
                                            	record_recharge AS rr
                                            	RIGHT JOIN (SELECT account FROM record_account as ll WHERE ll.daykey = ? GROUP BY account) as ra ON ra.account = rr.account
                                            """,
                                    Long.class,
                                    dayKey
                            );
                            registerAccountRechargeNum = Objects.returnNonNull(registerAccountRechargeNum, 0L);
                            gameStat.setRegisterAccountRechargeNum(registerAccountRechargeNum);
                        }
                        {
                            Long rechargeOrderNum = pgsqlDataHelper.executeScalar("SELECT \"count\"(DISTINCT sporder) FROM record_recharge WHERE daykey=?", Long.class, dayKey);
                            rechargeOrderNum = Objects.returnNonNull(rechargeOrderNum, 0L);
                            gameStat.setRechargeOrderNum(rechargeOrderNum);
                        }
                        {
                            /*ARPU = 今日充值金额 / 今日登录的账号数*/
                            long rechargeAmountNum = gameStat.getRechargeAmountNum();
                            long loginAccountNum = gameStat.getLoginAccountNum();
                            if (loginAccountNum == 0 || rechargeAmountNum == 0) {
                                gameStat.setArpu("0");
                            } else {
                                gameStat.setArpu(String.format("%.2f", rechargeAmountNum / 100f / loginAccountNum));
                            }
                        }
                        {
                            /*ARPPU = 今日充值金额 / 今日充值的账号数*/
                            long rechargeAmountNum = gameStat.getRechargeAmountNum();
                            long rechargeAccountNum = gameStat.getRechargeAccountNum();
                            if (rechargeAccountNum == 0 || rechargeAmountNum == 0) {
                                gameStat.setArppu("0");
                            } else {
                                gameStat.setArppu(String.format("%.2f", rechargeAmountNum / 100f / rechargeAccountNum));
                            }
                        }
                        {
                            /*今日注册账号付费数 / 今日注册账号数*/
                            long registerAccountNum = gameStat.getRegisterAccountNum();
                            float registerAccountRechargeNum = gameStat.getRegisterAccountRechargeNum();
                            if (registerAccountNum == 0 || registerAccountRechargeNum == 0) {
                                gameStat.setFufeilv("-");
                            } else {
                                gameStat.setFufeilv(String.format("%.2f", registerAccountRechargeNum / registerAccountNum * 100) + "%");
                            }
                        }
                        pgsqlDataHelper.getDataBatch().save(gameStat);
                    }
                }
            };
            gameContext.submit(event);
        }
    }

    @Scheduled("0 */20")
    public void accountStat() {
        Collection<GameContext> values = gameService.getGameContextHashMap().values();
        final long dayOfStartMillis = MyClock.dayOfStartMillis();
        int days = 121;
        LocalDateTime localDateTime = LocalDateTime.now().plusDays(-days);
        long startTime = MyClock.time2Milli(localDateTime);
        for (GameContext gameContext : values) {
            Game game = gameContext.getGame();
            PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
            /*统计留存开始时间*/
            AtomicLong statTime = new AtomicLong(startTime);
            if (statTime.get() < game.getCreateTime()) {
                statTime.set(game.getCreateTime());
            }
            Event event = new Event(TimeUnit.HOURS.toMillis(2), TimeUnit.HOURS.toMillis(2)) {
                @Override public void onEvent() throws Exception {
                    /*统计留存开始时间*/
                    for (int i = 0; i < days; i++) {
                        final int day = i;
                        LocalDateTime statLocalDateTime = MyClock.localDateTime(statTime.get()).plusDays(day);
                        if (MyClock.time2Milli(statLocalDateTime) > MyClock.dayOfEndMillis()) {
                            /*TODO 已经大于当前时间，说明是明天了，不在处理*/
                            break;
                        }
                        int registerDayKey = (statLocalDateTime.getYear() * 10000 + statLocalDateTime.getMonthValue() * 100 + statLocalDateTime.getDayOfMonth());
                        log.info("{} 账号留存 {} 统计开始", game.getName(), registerDayKey);
                        AccountStat accountStat = pgsqlDataHelper.findByKey(AccountStat.class, registerDayKey);
                        if (accountStat == null) {
                            accountStat = new AccountStat();
                            accountStat.setUid(registerDayKey);
                        }
                        {
                            Long registerAccountNum = pgsqlDataHelper.executeScalar("SELECT \"count\"(DISTINCT ll.account) FROM record_account as ll WHERE ll.daykey = ?;", Long.class, registerDayKey);
                            registerAccountNum = Objects.returnNonNull(registerAccountNum, 0L);
                            accountStat.setRegisterNum(registerAccountNum);
                            accountStat.getDayStatNumMap().put("1", String.valueOf(registerAccountNum));
                        }
                        long registerNum = accountStat.getRegisterNum();
                        for (int j = 1; j <= 120; j++) {
                            LocalDateTime loginLocalDateTime = statLocalDateTime.plusDays(j);
                            long time2Milli = MyClock.time2Milli(loginLocalDateTime);
                            if (time2Milli >= dayOfStartMillis || registerNum == 0) {
                                /*必须是今天凌晨以前的日期才统计*/
                                accountStat.getDayStatNumMap().put(String.valueOf(j + 1), "-");
                            } else {

                                int loginDayKey = (loginLocalDateTime.getYear() * 10000 + loginLocalDateTime.getMonthValue() * 100 + loginLocalDateTime.getDayOfMonth());
                                Long loginNum = pgsqlDataHelper.executeScalar(
                                        "SELECT \"count\"(DISTINCT ll.account) FROM record_role_login as ll WHERE ll.account in(SELECT ra.account FROM record_account as ra WHERE ra.daykey= ?) AND ll.daykey=?;",
                                        Long.class,
                                        registerDayKey, loginDayKey);

                                loginNum = Objects.returnNonNull(loginNum, 0L);
                                String string = String.format("%.2f", loginNum * 10000 / registerNum / 100f);
                                accountStat.getDayStatNumMap().put(String.valueOf(j + 1), string + "%");
                            }

                        }
                        pgsqlDataHelper.dataBatch().save(accountStat);
                        log.info("{} 账号留存 {} 统计结束", game.getName(), registerDayKey);
                    }
                }
            };

            gameContext.submit(event);
        }
    }

    /** 每分钟 */
    @Scheduled("0 *")
    public void onlineStat() {
        LocalDateTime localDateTime = LocalDateTime.now();
        int dayKey = (localDateTime.getYear() * 10000 + localDateTime.getMonthValue() * 100 + localDateTime.getDayOfMonth());

        long millis = TimeUnit.MINUTES.toMillis(3);

        Collection<GameContext> values = gameService.getGameContextHashMap().values();
        for (GameContext gameContext : values) {
            PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
            Collection<AccountRecord> accountRecords = gameContext.getAccountRecordJdbcCache().values();
            Collection<RoleRecord> roleRecords = gameContext.getRoleRecordJdbcCache().values();
            {
                OnlineStat onlineStat = pgsqlDataHelper.findByKey(OnlineStat.class, dayKey);
                if (onlineStat == null) {
                    onlineStat = new OnlineStat();
                    onlineStat.setUid(dayKey);
                }

                long accountOnlineCount = accountRecords.stream()
                        .filter(accountRecord -> Math.abs(System.currentTimeMillis() - accountRecord.getOnlineUpdateTime()) < millis)
                        .count();
                onlineStat.update(localDateTime.getHour(), (int) accountOnlineCount);

                pgsqlDataHelper.getDataBatch().save(onlineStat);
            }
            {
                ArrayList<ServerRecord> sidList = new ArrayList<>(gameContext.getServerRecordMap().values());
                for (ServerRecord serverRecord : sidList) {
                    if (serverRecord.getMainSid() != 0) continue;/*合服之后不处理*/
                    ServerOnlineStat onlineStat = pgsqlDataHelper.findByKey(ServerOnlineStat.class, dayKey, serverRecord.getUid());
                    if (onlineStat == null) {
                        onlineStat = new ServerOnlineStat();
                        onlineStat.setUid(dayKey);
                        onlineStat.setSid(serverRecord.getUid());
                    }

                    long roleOnlineCount = roleRecords.stream()
                            .filter(roleRecord -> roleRecord.getCurSid() == serverRecord.getUid())
                            .filter(roleRecord -> Math.abs(System.currentTimeMillis() - roleRecord.getOnlineUpdateTime()) < millis)
                            .map(RoleRecord::getAccount)
                            .distinct()
                            .count();

                    onlineStat.update(localDateTime.getHour(), (int) roleOnlineCount);
                    pgsqlDataHelper.getDataBatch().save(onlineStat);
                }
            }
        }
    }

}
