package wxdgaming.backends.mudole.role.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.system.User;
import wxdgaming.backends.mudole.role.RoleService;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.threading.ExecutorWith;
import wxdgaming.boot2.core.threading.ThreadContext;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.NumberUtil;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.List;

/**
 * 角色接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-23 20:55
 **/
@Slf4j
@Singleton
@RequestMapping(path = "role")
public class RoleApi {

    final GameService gameService;
    final SLogService SLogService;
    final RoleService roleService;

    @Inject
    public RoleApi(GameService gameService, SLogService SLogService, RoleService roleService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
        this.roleService = roleService;
    }

    @HttpRequest(authority = 2)
    @ExecutorWith(useVirtualThread = true)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") RoleRecord record) {

        log.info("role - {}", record.toJsonString());
        AccountRecord accountRecord = gameContext.getAccountRecord(record.getAccount());
        if (accountRecord == null) {
            gameContext.recordError(record.toJsonString(), "角色记录 找不到账号 " + record.getAccount());
        } else {
            RoleRecord entity = gameContext.getRoleRecord(record.getUid());
            if (entity == null) {
                if (record.getUid() == 0) {
                    record.setUid(gameContext.getGameId());
                }
                if (record.getCreateTime() == 0) {
                    record.setCreateTime(System.currentTimeMillis());
                }
                record.checkDataKey();
                gameContext.getRoleRecordJdbcCache().put(record.getUid(), record);
            } else {
                entity.setCurSid(record.getCurSid());
                entity.setRoleName(record.getRoleName());
                entity.setJob(record.getJob());
                entity.setSex(record.getSex());
                entity.setLv(record.getLv());
                entity.getData().clear();
                entity.getData().putAll(record.getData());
            }
            if (!accountRecord.getRoleList().contains(record.getUid())) {
                accountRecord.getRoleList().add(record.getUid());
            }
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<RoleRecord> recordList) {
        ExecutorUtil.getInstance().getVirtualExecutor().execute(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                for (RoleRecord record : recordList) {
                    push(gameContext, record);
                }
            }
        });
        return RunResult.ok();
    }

    @HttpRequest(authority = 2)
    public RunResult delete(HttpContext httpContext,
                            @ThreadParam GameContext gameContext,
                            @Param(path = "roleId") long roleId) {

        User user = ThreadContext.context("user");
        log.info("{}", user);
        RoleRecord entity = gameContext.getRoleRecord(roleId);
        if (entity != null) {
            entity.setDel(1);
            return RunResult.ok();
        }
        return RunResult.error("角色不存在");
    }

    @HttpRequest
    public RunResult list(HttpContext httpContext,
                          @Param(path = "gameId") int gameId,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName,
                          @Param(path = "curSid", required = false) String curSid,
                          @Param(path = "createSid", required = false) String createSid) {

        User user = ThreadContext.context("user");
        log.info("{}", user);

        PgsqlDataHelper pgsqlDataHelper = gameService.gameContext(gameId).getDataHelper();
        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();
        queryBuilder.sqlByEntity(RoleRecord.class);
        queryBuilder.pushWhereByValueNotNull("account=?", account);
        queryBuilder.pushWhereByValueNotNull("roleid=?", roleId);
        queryBuilder.pushWhereByValueNotNull("rolename=?", roleName);
        if (StringUtils.isNotBlank(curSid)) {
            queryBuilder.pushWhereByValueNotNull("cursid=?", NumberUtil.parseInt(curSid, 0));
        }
        if (StringUtils.isNotBlank(createSid)) {
            queryBuilder.pushWhereByValueNotNull("createsid=?", NumberUtil.parseInt(createSid, 0));
        }

        queryBuilder.setOrderBy("createtime desc");
        if (pageIndex > 0) {
            queryBuilder.setSkip((pageIndex - 1) * pageSize);
        }

        if (pageSize <= 10) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;
        queryBuilder.setLimit(pageSize);

        long rowCount = queryBuilder.findCount();
        List<RoleRecord> accountRecords = queryBuilder.findList2Entity(RoleRecord.class);

        List<JSONObject> list = accountRecords.stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("rechargeFirstTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("rechargeFirstTime")));
                    jsonObject.put("rechargeLastTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("rechargeLastTime")));
                    jsonObject.put("lastJoinTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("lastJoinTime")));
                    jsonObject.put("lastExitTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("lastExitTime")));
                    jsonObject.put("data", jsonObject.getString("data"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }

}
