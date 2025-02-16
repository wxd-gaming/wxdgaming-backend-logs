package wxdgaming.backends.mudole.role.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.RoleRecord;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ThreadContext;
import wxdgaming.boot2.core.threading.ThreadInfo;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.server.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.ann.RequestMapping;
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

    @Inject
    public RoleApi(GameService gameService, SLogService SLogService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
    }

    @HttpRequest
    @ThreadInfo(vt = true)
    public RunResult push(HttpContext httpContext, @Body RoleRecord roleRecord) {

        log.info("role - {}", roleRecord.toJsonString());

        if (roleRecord.getGameId() == 0) return RunResult.error("gameId is null");
        if (StringUtils.isBlank(roleRecord.getToken())) return RunResult.error("token is null");

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(roleRecord.getGameId());

        RoleRecord entity = pgsqlDataHelper.findByWhere(
                RoleRecord.class,
                "account = ? AND  roleid= ? AND createsid=?",
                roleRecord.getAccount(), roleRecord.getRoleId(), roleRecord.getCreateSid()
        );

        roleRecord.setLogTime(System.currentTimeMillis());

        if (entity == null) {
            if (roleRecord.getUid() == 0) {
                long newId = gameService.newId(roleRecord.getGameId());
                roleRecord.setUid(newId);
            }
            if (roleRecord.getCreateTime() == 0) {
                roleRecord.setCreateTime(System.currentTimeMillis());
            }
            pgsqlDataHelper.insert(roleRecord);
            return RunResult.ok().msg("新增");
        } else {
            roleRecord.setUid(entity.getUid());
            roleRecord.setCreateTime(Math.min(roleRecord.getCreateTime(), entity.getCreateTime()));
            roleRecord.setCreateSid(entity.getCreateSid());
            pgsqlDataHelper.update(roleRecord);
            return RunResult.ok().msg("修改");
        }
    }

    @HttpRequest
    public RunResult list(HttpContext httpContext,
                          @Param("gameId") Integer gameId,
                          @Param(value = "account", required = false) String account,
                          @Param(value = "roleId", required = false) String roleId,
                          @Param(value = "roleName", required = false) String roleName,
                          @Param(value = "curSid", required = false) int curSid,
                          @Param(value = "createSid", required = false) int createSid) {

        log.info("{}", (Object) ThreadContext.context("user"));

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
        List<RoleRecord> accountRecords;
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

        if (StringUtils.isBlank(sqlWhere)) {
            accountRecords = pgsqlDataHelper.findAll(RoleRecord.class);
        } else {
            accountRecords = pgsqlDataHelper.findListByWhere(RoleRecord.class, sqlWhere, args);
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
