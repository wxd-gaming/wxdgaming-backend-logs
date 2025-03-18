package wxdgaming.backends.mudole.role.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.system.User;
import wxdgaming.backends.mudole.role.RoleService;
import wxdgaming.backends.mudole.srolelog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.format.TimeFormat;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
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
@RequestMapping(path = "log/role")
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
        gameContext.submit(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                RoleRecord entity = gameContext.roleGetOrCreate(record.getAccount(), record.getUid());

                entity.setCreateSid(record.getCreateSid());
                entity.setCurSid(record.getCurSid());
                entity.setRoleName(record.getRoleName());
                entity.setJob(record.getJob());
                entity.setSex(record.getSex());
                entity.setLv(record.getLv());
                entity.getOther().clear();
                entity.getOther().putAll(record.getOther());

            }
        });
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult lv(HttpContext httpContext,
                        @ThreadParam GameContext gameContext,
                        @Param(path = "account") String account,
                        @Param(path = "roleId") long roleId,
                        @Param(path = "lv") int lv) {
        gameContext.submit(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                RoleRecord entity = gameContext.roleGetOrCreate(account, roleId);
                if (entity == null) {
                    gameContext.recordError("设置角色等级找不到角色 " + roleId, String.valueOf(lv));
                } else {
                    entity.setLv(lv);
                }
            }
        });
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult lvList(HttpContext httpContext,
                            @ThreadParam GameContext gameContext,
                            @Param(path = "data") List<JSONObject> datas) {
        for (JSONObject data : datas) {
            lv(httpContext, gameContext, data.getString("account"), data.getLongValue("roleId"), data.getIntValue("lv"));
        }
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<RoleRecord> recordList) {
        for (RoleRecord record : recordList) {
            push(gameContext, record);
        }
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult delete(HttpContext httpContext,
                            @ThreadParam GameContext gameContext,
                            @Param(path = "account") String account,
                            @Param(path = "roleId") long roleId) {

        User user = ThreadContext.context("user");
        log.info("{}", user);
        RoleRecord entity = gameContext.roleGetOrCreate(account, roleId);
        if (entity != null) {
            entity.setDel(1);
            return RunResult.OK;
        }
        return RunResult.error("角色不存在");
    }

    @HttpRequest
    public RunResult list(HttpContext httpContext,
                          @ThreadParam(path = "user") User user,
                          @Param(path = "gameId") int gameId,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "minDay", required = false) String minDay,
                          @Param(path = "maxDay", required = false) String maxDay,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName,
                          @Param(path = "curSid", required = false) String curSid,
                          @Param(path = "createSid", required = false) String createSid,
                          @Param(path = "online", required = false) String online,
                          @Param(path = "rechargeAmount", required = false) String rechargeAmount,
                          @Param(path = "rechargeCount", required = false) String rechargeCount,
                          @Param(path = "other", required = false) String other) {

        log.info("{}", user);

        GameContext gameContext = gameService.gameContext(gameId);
        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();
        queryBuilder.sqlByEntity(RoleRecord.class);
        queryBuilder.pushWhereByValueNotNull("account=?", account);
        if (StringUtils.isNotBlank(roleId)) {
            queryBuilder.pushWhereByValueNotNull("uid=?", NumberUtil.parseLong(roleId, 0));
        }
        queryBuilder.pushWhereByValueNotNull("rolename=?", roleName);
        if (StringUtils.isNotBlank(curSid)) {
            queryBuilder.pushWhereByValueNotNull("cursid=?", NumberUtil.parseInt(curSid, 0));
        }
        if (StringUtils.isNotBlank(createSid)) {
            queryBuilder.pushWhereByValueNotNull("createsid=?", NumberUtil.parseInt(createSid, 0));
        }

        if (StringUtils.isNotBlank(rechargeAmount)) {
            queryBuilder.pushWhereByValueNotNull("rechargeamount>=?", NumberUtil.parseLong(rechargeAmount, 0L));
        }

        if (StringUtils.isNotBlank(rechargeCount)) {
            queryBuilder.pushWhereByValueNotNull("rechargecount>=?", NumberUtil.parseInt(rechargeCount, 0));
        }
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
        List<RoleRecord> roleRecords = queryBuilder.findList2Entity(RoleRecord.class);

        List<JSONObject> list = roleRecords.stream()
                .filter(record -> {
                    if (StringUtils.isNotBlank(online)) {
                        return online.equals("1") == record.online();
                    } else {
                        return true;
                    }
                })
                .map(roleRecord -> {
                    RoleRecord roleRecordCache = gameContext.getRoleRecordJdbcCache().find(roleRecord.getUid());
                    if (roleRecordCache != null) {
                        roleRecord = roleRecordCache;
                    }
                    JSONObject jsonObject = roleRecord.toJSONObject();
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", roleRecord.getCreateTime()));
                    jsonObject.put("rechargeFirstTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", roleRecord.getRechargeFirstTime()));
                    jsonObject.put("rechargeLastTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", roleRecord.getRechargeLastTime()));
                    jsonObject.put("lastJoinTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", roleRecord.getLastJoinTime()));
                    jsonObject.put("lastExitTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", roleRecord.getLastExitTime()));
                    jsonObject.put("online", roleRecord.online());
                    jsonObject.put("totalOnlineTime", new TimeFormat().addTime(jsonObject.getLong("totalOnlineTime") * 100).toString(TimeFormat.FormatInfo.All));
                    jsonObject.put("lastOnlineTime", new TimeFormat().addTime(jsonObject.getLong("lastOnlineTime") * 100).toString(TimeFormat.FormatInfo.All));
                    jsonObject.put("other", jsonObject.getString("other"));
                    return jsonObject;
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }

}
