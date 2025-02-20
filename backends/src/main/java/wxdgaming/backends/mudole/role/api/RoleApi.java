package wxdgaming.backends.mudole.role.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.system.User;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.mudole.role.RoleService;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ExecutorWith;
import wxdgaming.boot2.core.threading.ThreadContext;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.Arrays;
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
    public RunResult push(HttpContext httpContext, @Body RoleRecord record) {

        log.info("role - {}", record.toJsonString());

        if (record.getGameId() == 0) return RunResult.error("gameId is null");
        if (StringUtils.isBlank(record.getToken())) return RunResult.error("token is null");

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(record.getGameId());

        RoleRecord entity = roleService.roleRecord(record.getGameId(), record.getAccount(), record.getRoleId());

        if (entity == null) {
            if (record.getUid() == 0) {
                long newId = gameService.newId(record.getGameId());
                record.setUid(newId);
            }
            if (record.getCreateTime() == 0) {
                record.setCreateTime(System.currentTimeMillis());
            }
            record.checkDataKey();
            pgsqlDataHelper.getSqlDataBatch().insert(record);
            return RunResult.ok().msg("新增");
        } else {
            record.setUid(entity.getUid());
            record.setAccount(entity.getAccount());
            record.setCreateTime(entity.getCreateTime());
            record.setDayKey(entity.getDayKey());
            record.setCreateSid(entity.getCreateSid());
            pgsqlDataHelper.getSqlDataBatch().update(record);
            return RunResult.ok().msg("修改");
        }
    }

    @HttpRequest(authority = 2)
    public RunResult delete(HttpContext httpContext,
                            @Param(path = "gameId") Integer gameId,
                            @Param(path = "account") String account,
                            @Param(path = "roleId") String roleId) {

        User user = ThreadContext.context("user");
        log.info("{}", user);

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
        RoleRecord entity = roleService.roleRecord(gameId, account, roleId);
        if (entity != null) {
            entity.setDel(1);
            pgsqlDataHelper.getSqlDataBatch().update(entity);
            return RunResult.ok();
        }
        return RunResult.error("角色不存在");
    }

    @HttpRequest
    public RunResult list(HttpContext httpContext,
                          @Param(path = "gameId") Integer gameId,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName,
                          @Param(path = "curSid", required = false) int curSid,
                          @Param(path = "createSid", required = false) int createSid) {

        User user = ThreadContext.context("user");
        log.info("{}", user);

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);

        String sqlWhere = "";
        Object[] args = new Object[1];
        if (StringUtils.isNotBlank(account)) {
            sqlWhere = "account = ?";
            args[args.length - 1] = account;
        }
        if (StringUtils.isNotBlank(roleId)) {
            if (!sqlWhere.isEmpty()) {
                sqlWhere += " AND ";
                args = Arrays.copyOf(args, args.length + 1);
            }
            sqlWhere += "roleid = ?";
            args[args.length - 1] = roleId;

        }
        if (StringUtils.isNotBlank(roleName)) {
            if (!sqlWhere.isEmpty()) {
                sqlWhere += " AND ";
                args = Arrays.copyOf(args, args.length + 1);
            }
            sqlWhere += "rolename = ?";
            args[args.length - 1] = roleName;
        }
        if (curSid > 0) {
            if (!sqlWhere.isEmpty()) {
                sqlWhere += " AND ";
                args = Arrays.copyOf(args, args.length + 1);
            }
            sqlWhere += "cursid = ?";
            args[args.length - 1] = curSid;
        }
        if (createSid > 0) {
            if (!sqlWhere.isEmpty()) {
                sqlWhere += " AND ";
                args = Arrays.copyOf(args, args.length + 1);
            }
            sqlWhere += "createsid = ?";
            args[args.length - 1] = createSid;
        }

        List<RoleRecord> accountRecords;
        if (StringUtils.isBlank(sqlWhere)) {
            accountRecords = pgsqlDataHelper.findList(RoleRecord.class);
        } else {
            accountRecords = pgsqlDataHelper.findListBySql(RoleRecord.class, sqlWhere, args);
        }

        List<JSONObject> list = accountRecords.stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("data", jsonObject.getString("data"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", list.size());
    }

}
