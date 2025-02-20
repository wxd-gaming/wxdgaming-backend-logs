package wxdgaming.backends.mudole.account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.AccountStat;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 游戏账号
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:39
 **/
@Slf4j
@Singleton
public class AccountService {

    final GameService gameService;

    @Inject
    public AccountService(GameService gameService) {
        this.gameService = gameService;
    }


    @Scheduled("0 0")
    public void accountStat() {
        Collection<Game> values = gameService.getGameId2GameRecordMap().values();
        final long dayOfStartMillis = MyClock.dayOfStartMillis();
        LocalDateTime localDateTime = LocalDateTime.now().plusDays(-120);
        long startTime = MyClock.time2Milli(localDateTime);
        for (Game game : values) {
            PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(game.intUid());
            /*统计留存开始时间*/
            AtomicLong statTime = new AtomicLong(startTime);
            if (statTime.get() < game.getCreateTime()) {
                statTime.set(game.getCreateTime());
            }
            Event event = new Event(TimeUnit.HOURS.toMillis(2), TimeUnit.HOURS.toMillis(2)) {
                @Override public void onEvent() throws Exception {
                    /*统计留存开始时间*/
                    for (int i = 0; i < 120; i++) {
                        final int day = i;
                        LocalDateTime statLocalDateTime = MyClock.localDateTime(statTime.get()).plusDays(day);
                        int registerDayKey = (statLocalDateTime.getYear() * 10000 + statLocalDateTime.getMonthValue() * 100 + statLocalDateTime.getDayOfMonth());
                        log.info("{} 账号留存 {} 统计开始", game.getName(), registerDayKey);
                        AccountStat accountStat = pgsqlDataHelper.findByKey(AccountStat.class, registerDayKey);
                        if (accountStat == null) {
                            accountStat = new AccountStat();
                            accountStat.setUid(registerDayKey);
                            Long aLong = pgsqlDataHelper.executeScalar("SELECT \"count\"(1) FROM record_account as ll WHERE ll.daykey = ?;", Long.class, registerDayKey);
                            aLong = Objects.returnNonNull(aLong, 0L);
                            accountStat.setRegisterNum(aLong);
                            accountStat.getDayStatNumMap().put("1", String.valueOf(aLong));
                        }
                        long registerNum = accountStat.getRegisterNum();
                        for (int j = 1; j <= 120; j++) {
                            LocalDateTime loginLocalDateTime = statLocalDateTime.plusDays(j);
                            long time2Milli = MyClock.time2Milli(loginLocalDateTime);
                            if (time2Milli >= dayOfStartMillis) {
                                /*必须是今天凌晨以前的日期才统计*/
                                accountStat.getDayStatNumMap().put(String.valueOf(j + 1), "-");
                            } else {

                                int loginDayKey = (loginLocalDateTime.getYear() * 10000 + loginLocalDateTime.getMonthValue() * 100 + loginLocalDateTime.getDayOfMonth());
                                Long loginNum = pgsqlDataHelper.executeScalar(
                                        "SELECT \"count\"(1) FROM log_login as ll WHERE ll.account in(SELECT ra.account FROM record_account as ra WHERE ra.daykey= ?) AND ll.daykey=?;",
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

            ExecutorUtil.getVirtualExecutor().execute(event);
        }
    }

}
