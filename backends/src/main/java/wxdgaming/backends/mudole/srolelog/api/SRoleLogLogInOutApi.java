package wxdgaming.backends.mudole.srolelog.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.OnlineTimeRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.games.logs.SRoleLog2Login;
import wxdgaming.backends.mudole.srolelog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.threading.ExecutorWith;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.NumberUtil;
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
@RequestMapping(path = "log/role/login")
public class SRoleLogLogInOutApi {

    final GameService gameService;
    final SLogService SLogService;

    @Inject
    public SRoleLogLogInOutApi(GameService gameService, SLogService SLogService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<SRoleLog2Login> recordList) {
        for (SRoleLog2Login record : recordList) {
            push(gameContext, record);
        }
        return RunResult.OK;
    }

    /** 登录日志的批量提交 */
    @HttpRequest(authority = 2)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") SRoleLog2Login record) {
        gameContext.submit(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                record.setLogType(record.tableName());
                if (record.getUid() == 0)
                    record.setUid(gameContext.newId(record.tableName()));

                String logKey = record.tableName() + record.getUid();
                boolean haveLogKey = gameContext.getLogKeyCache().has(logKey);
                if (haveLogKey) {
                    gameContext.recordError("表结构 " + record.tableName() + " 重复日志记录 " + record.getUid(), record.toJSONString());
                } else {

                    SRoleLog2Login.LogEnum logEnum = record.getLogEnum();
                    if (logEnum == null) {
                        gameContext.recordError("登录记录 logEnum 参数异常", record.toJSONString());
                        return;
                    }

                    AccountRecord accountRecord = gameContext.accountGetOrCreate(record.getAccount());
                    RoleRecord roleRecord = gameContext.roleGetOrCreate(record.getAccount(), record.getRoleId());

                    record.checkDataKey();
                    gameContext.getLogKeyCache().put(logKey, true);
                    gameContext.getDataHelper().dataBatch().insert(record);

                    roleRecord.setCurSid(record.getSid());
                    roleRecord.setRoleName(record.getRoleName());
                    roleRecord.setLv(record.getLv());
                    roleRecord.getOther().putAll(record.getOther());

                    if (logEnum == SRoleLog2Login.LogEnum.LOGIN) {
                        roleRecord.setLastJoinTime(record.getCreateTime());
                        roleRecord.setLastJoinSid(record.getSid());
                        roleRecord.setOnlineUpdateTime(record.getCreateTime());
                        long oldOnlineTime = accountRecord.getOnlineUpdateTime();
                        accountRecord.setOnlineUpdateTime(Math.max(record.getCreateTime(), oldOnlineTime));
                        /* todo 上线的时候刷新一下活跃账号记录*/
                        SLogService.refreshActiveAccount(gameContext, accountRecord.getAccount(), oldOnlineTime, accountRecord.getOnlineUpdateTime());

                    } else if (logEnum == SRoleLog2Login.LogEnum.LOGOUT) {
                        roleRecord.setLastExitTime(record.getCreateTime());
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
                        /* todo 下线的时候刷新一下活跃账号记录*/
                        long oldOnlineTime = accountRecord.getOnlineUpdateTime();
                        SLogService.refreshActiveAccount(gameContext, accountRecord.getAccount(), oldOnlineTime, record.getCreateTime());

                        onlineTimeRecord.checkDataKey();
                        gameContext.getDataHelper().getDataBatch().insert(onlineTimeRecord);
                    }
                }
            }
        });
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult online(HttpContext httpSession,
                            @ThreadParam GameContext gameContext,
                            @Param(path = "account") String account,
                            @Param(path = "roleId") long roleId,
                            @Param(path = "time") long time) {
        gameContext.submit(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                AccountRecord accountRecord = gameContext.accountGetOrCreate(account);
                RoleRecord roleRecord = gameContext.roleGetOrCreate(account, roleId);
                roleRecord.setOnlineUpdateTime(Math.max(roleRecord.getOnlineUpdateTime(), time));
                long oldOnlineTime = accountRecord.getOnlineUpdateTime();
                accountRecord.setOnlineUpdateTime(Math.max(oldOnlineTime, time));
                /* todo 同步在线状态的时候刷新一下活跃账号记录*/
                SLogService.refreshActiveAccount(gameContext, account, oldOnlineTime, accountRecord.getOnlineUpdateTime());
            }
        });
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult onlineList(HttpContext httpSession,
                                @ThreadParam GameContext gameContext,
                                @Param(path = "data") List<JSONObject> data) {

        for (JSONObject datum : data) {
            online(httpSession, gameContext, datum.getString("account"), datum.getLongValue("roleId"), datum.getLongValue("time"));
        }

        return RunResult.OK;
    }

    @HttpRequest(authority = 9)
    @ExecutorWith(useVirtualThread = true)
    public RunResult list(HttpContext httpSession,
                          @Param(path = "gameId") int gameId,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "minDay", required = false) String minDay,
                          @Param(path = "maxDay", required = false) String maxDay,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName,
                          @Param(path = "other", required = false) String other) {

        GameContext gameContext = gameService.gameContext(gameId);

        if (gameContext == null) {
            return RunResult.error("gameId error");
        }

        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();

        queryBuilder.sqlByEntity(SRoleLog2Login.class);
        queryBuilder.pushWhereByValueNotNull("account=?", account);
        if (StringUtils.isNotBlank(roleId)) {
            queryBuilder.pushWhereByValueNotNull("roleid=?", NumberUtil.parseLong(roleId, 0L));
        }
        queryBuilder.pushWhereByValueNotNull("rolename=?", roleName);

        if (StringUtils.isNotBlank(minDay)) {
            queryBuilder.pushWhereByValueNotNull("daykey>=?", NumberUtil.retainNumber(minDay));
        }

        if (StringUtils.isNotBlank(maxDay)) {
            queryBuilder.pushWhereByValueNotNull("daykey<=?", NumberUtil.retainNumber(maxDay));
        }

        if (StringUtils.isNotBlank(other)) {
            String[] split = other.split(",");
            for (String s : split) {
                String[] strings = s.split("=");
                queryBuilder.pushWhere("json_extract_path_text(other,'" + strings[0] + "') = ?", strings[1]);
            }
        }

        queryBuilder.setOrderBy("createtime desc");

        queryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);

        long rowCount = queryBuilder.findCount();

        List<SRoleLog2Login> slogs = queryBuilder.findList2Entity(SRoleLog2Login.class);

        List<JSONObject> list = slogs.stream()
                .map(SRoleLog2Login::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("other", jsonObject.getString("other"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }

}
