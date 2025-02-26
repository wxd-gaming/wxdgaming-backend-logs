package wxdgaming.backends.mudole.slog.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.OnlineTimeRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.games.logs.SLog2Login;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;

import java.util.List;

/**
 * 日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 16:54
 **/
@Slf4j
@Singleton
@RequestMapping(path = "log/login")
public class SLogLogInOutApi {

    final GameService gameService;
    final SLogService SLogService;

    @Inject
    public SLogLogInOutApi(GameService gameService, SLogService SLogService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<SLog2Login> recordList) {
        ExecutorUtil.getInstance().getLogicExecutor().execute(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                for (SLog2Login record : recordList) {
                    push(gameContext, record);
                }
            }
        });
        return RunResult.ok();
    }

    /** 登录日志的批量提交 */
    @HttpRequest(authority = 2)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") SLog2Login record) {
        record.setLogType(record.tableName());
        if (record.getUid() == 0)
            record.setUid(gameContext.newId(record.tableName()));

        String logKey = record.tableName() + record.getUid();
        boolean haveLogKey = gameContext.getLogKeyCache().containsKey(logKey);
        if (haveLogKey) {
            gameContext.recordError("表结构 " + record.tableName() + " 重复日志记录 " + record.getUid(), record.toJsonString());
        } else {


            SLog2Login.LogEnum logEnum = record.getLogEnum();
            if (logEnum == null) {
                gameContext.recordError("登录记录 logEnum 参数异常", record.toJsonString());
                return RunResult.ok();
            }
            AccountRecord accountRecord = gameContext.getAccountRecord(record.getAccount());
            if (accountRecord == null) {
                gameContext.recordError("登录记录 找不到账号 " + record.getAccount(), record.toJsonString());
                return RunResult.ok();
            }

            RoleRecord roleRecord = gameContext.getRoleRecord(record.getRoleId());
            if (roleRecord == null) {
                gameContext.recordError("登录记录 找不到角色 " + record.getRoleId(), record.toJsonString());
                return RunResult.ok();
            }
            record.checkDataKey();
            gameContext.getLogKeyCache().put(logKey, true);
            gameContext.getDataHelper().dataBatch().insert(record);

            if (logEnum == SLog2Login.LogEnum.LOGIN && !roleRecord.isOnline()) {
                roleRecord.setLastJoinTime(record.getCreateTime());
                roleRecord.setLastJoinSid(record.getSid());
                roleRecord.setOnline(true);
            } else if (logEnum == SLog2Login.LogEnum.LOGOUT && roleRecord.isOnline()) {
                roleRecord.setLastExitTime(record.getCreateTime());
                roleRecord.setOnline(false);
                OnlineTimeRecord onlineTimeRecord = new OnlineTimeRecord();
                onlineTimeRecord.setCreateTime(MyClock.millis());/* 产生记录的时间 */
                onlineTimeRecord.setUid(gameContext.newId("OnlineTimeRecord"));
                onlineTimeRecord.setAccount(record.getAccount());
                onlineTimeRecord.setRoleId(record.getRoleId());
                onlineTimeRecord.setRoleName(record.getRoleName());
                onlineTimeRecord.setLv(record.getLv());
                onlineTimeRecord.setSid(record.getSid());
                onlineTimeRecord.setJoinTime(roleRecord.getLastJoinTime());/*记录的上次进入游戏的时间*/
                onlineTimeRecord.setExitTime(record.getCreateTime());/*退出日志创建时间*/
                long onlineTime = roleRecord.getLastExitTime() - roleRecord.getLastJoinTime();
                onlineTimeRecord.setOnlineTime(onlineTime);
                onlineTimeRecord.setTotalOnlineTime(roleRecord.getTotalOnlineTime() + onlineTime);

                /*角色单独记录在线时长*/
                roleRecord.setLastOnlineTime(onlineTime);
                roleRecord.setTotalOnlineTime(roleRecord.getTotalOnlineTime() + onlineTime);

                accountRecord.setTotalOnlineTime(accountRecord.getTotalOnlineTime() + onlineTime);

                onlineTimeRecord.checkDataKey();
                gameContext.getDataHelper().getDataBatch().insert(onlineTimeRecord);
            }
        }
        return RunResult.ok();
    }


}
