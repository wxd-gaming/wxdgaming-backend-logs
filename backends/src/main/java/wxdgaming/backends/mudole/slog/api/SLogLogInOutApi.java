package wxdgaming.backends.mudole.slog.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.games.logs.SLog;
import wxdgaming.backends.entity.games.logs.SLog2Login;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.threading.ExecutorWith;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.TableMapping;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.List;

/**
 * 日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 16:54
 **/
@Slf4j
@Singleton
@RequestMapping(path = "log")
public class SLogLogInOutApi {

    final GameService gameService;
    final SLogService SLogService;

    @Inject
    public SLogLogInOutApi(GameService gameService, SLogService SLogService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList4Login(@ThreadParam GameContext gameContext, @Param(path = "data") List<SLog2Login> recordList) {
        ExecutorUtil.getInstance().getVirtualExecutor().execute(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                for (SLog2Login record : recordList) {
                    push4Login(gameContext, record);
                }
            }
        });
        return RunResult.ok();
    }

    /** 登录日志的批量提交 */
    @HttpRequest(authority = 2)
    public RunResult push4Login(@ThreadParam GameContext gameContext, @Param(path = "data") SLog2Login record) {
        if (record.getUid() == 0)
            record.setUid(gameContext.newId(TableMapping.tableName(SLog2Login.class)));
        String logKey = record.getLogType() + record.getUid();
        boolean haveLogKey = gameContext.getLogKeyCache().containsKey(logKey);
        if (haveLogKey) {
            gameContext.recordError(record.toJsonString(), "表结构 " + record.getLogType() + " 重复日志记录 " + record.getUid());
        } else {

            record.checkDataKey();

            gameContext.getLogKeyCache().put(logKey, true);
            gameContext.getDataHelper().getDataBatch().insert(record);

            AccountRecord accountRecord = gameContext.getAccountRecord(record.getAccount());
            if (accountRecord == null) {
                gameContext.recordError(record.toJsonString(), "登录记录 找不到账号 " + record.getAccount());
            } else {
                accountRecord.setLastJoinTime(record.getCreateTime());
                accountRecord.setLastJoinSid(record.getSId());
                accountRecord.setOnline(true);
            }
            RoleRecord roleRecord = gameContext.getRoleRecord(record.getUid());
            if (roleRecord == null) {
                gameContext.recordError(record.toJsonString(), "登录记录 找不到角色 " + record.getRoleId());
            } else {
                roleRecord.setLastJoinTime(record.getCreateTime());
                roleRecord.setLastJoinSid(record.getSId());
                roleRecord.setOnline(true);
            }
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 2)
    public RunResult pushList4Logout(@ThreadParam GameContext gameContext, @Param(path = "data") List<SLogLogoutParams> recordList) {
        ExecutorUtil.getInstance().getVirtualExecutor().execute(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                for (SLogLogoutParams record : recordList) {
                    push4Logout(gameContext, record);
                }
            }
        });
        return RunResult.ok();
    }

    /** 登录日志的批量提交 */
    @HttpRequest(authority = 2)
    public RunResult push4Logout(@ThreadParam GameContext gameContext,
                                 @Param(path = "data") SLogLogoutParams sqlParams) {
        AccountRecord accountRecord = gameContext.getAccountRecord(sqlParams.getAccount());
        if (accountRecord != null) {
            accountRecord.setLastExitTime(System.currentTimeMillis());
            accountRecord.setOnline(false);
        }
        if (sqlParams.getRoleId() != null) {
            RoleRecord roleRecord = gameContext.getRoleRecord(sqlParams.getRoleId());
            if (roleRecord != null) {
                roleRecord.setLastExitTime(System.currentTimeMillis());
                roleRecord.setOnline(false);
            }
        }
        return RunResult.ok();
    }

    @Getter
    @Setter
    public static class SLogLogoutParams {
        private String account;
        private Long roleId;
    }

}
