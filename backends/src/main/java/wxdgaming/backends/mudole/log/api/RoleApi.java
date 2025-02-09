package wxdgaming.backends.mudole.log.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.RoleRecord;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.backends.mudole.log.LogsService;
import wxdgaming.boot.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot.core.lang.RunResult;
import wxdgaming.boot.core.str.StringUtil;
import wxdgaming.boot.core.str.json.FastJsonUtil;
import wxdgaming.boot.core.threading.ThreadInfo;
import wxdgaming.boot.core.timer.MyClock;
import wxdgaming.boot.net.controller.ann.Body;
import wxdgaming.boot.net.controller.ann.Param;
import wxdgaming.boot.net.controller.ann.TextController;
import wxdgaming.boot.net.controller.ann.TextMapping;
import wxdgaming.boot.net.web.hs.HttpSession;

import java.util.Arrays;
import java.util.List;

/**
 * 角色接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-23 20:55
 **/
@Slf4j
@TextController(path = "role")
public class RoleApi {

    final GameService gameService;
    final LogsService logsService;

    @Inject
    public RoleApi(GameService gameService, LogsService logsService) {
        this.gameService = gameService;
        this.logsService = logsService;
    }

    @TextMapping
    @ThreadInfo(vt = true)
    public RunResult push(HttpSession session, @Body RoleRecord roleRecord) {

        log.info("role - {}", roleRecord.toJson());

        if (roleRecord.getGameId() == 0) return RunResult.error("gameId is null");
        if (StringUtil.emptyOrNull(roleRecord.getToken())) return RunResult.error("token is null");

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(roleRecord.getGameId());

        RoleRecord entity = pgsqlDataHelper.queryEntityByWhere(
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
            return RunResult.ok().errorMsg("新增");
        } else {
            roleRecord.setUid(entity.getUid());
            roleRecord.setCreateTime(Math.min(roleRecord.getCreateTime(), entity.getCreateTime()));
            roleRecord.setCreateSid(entity.getCreateSid());
            pgsqlDataHelper.update(roleRecord);
            return RunResult.ok().errorMsg("修改");
        }
    }

    @TextMapping
    public RunResult list(HttpSession httpSession,
                          @Param("gameId") Integer gameId,
                          @Param(value = "account", required = false) String account,
                          @Param(value = "roleId", required = false) String roleId,
                          @Param(value = "roleName", required = false) String roleName) {
        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
        List<RoleRecord> accountRecords;
        String sqlWhere = "";
        Object[] args = new Object[1];
        if (StringUtil.notEmptyOrNull(account)) {
            sqlWhere = "account = ?";
            args[args.length - 1] = account;
        }
        if (StringUtil.notEmptyOrNull(roleId)) {
            if (!sqlWhere.isEmpty()) {
                sqlWhere += " AND ";
                args = Arrays.copyOf(args, args.length + 1);
            }
            sqlWhere += "roleid = ?";
            args[args.length - 1] = roleId;

        }
        if (StringUtil.notEmptyOrNull(roleName)) {
            if (!sqlWhere.isEmpty()) {
                sqlWhere += " AND ";
                args = Arrays.copyOf(args, args.length + 1);
            }
            sqlWhere += "rolename = ?";
            args[args.length - 1] = roleName;
        }
        if (StringUtil.emptyOrNull(sqlWhere)) {
            accountRecords = pgsqlDataHelper.queryEntities(RoleRecord.class);
        } else {
            accountRecords = pgsqlDataHelper.queryEntitiesWhere(RoleRecord.class, sqlWhere, args);
        }
        List<JSONObject> list = accountRecords.stream()
                .map(FastJsonUtil::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("createTime")));
                    jsonObject.put("data", jsonObject.getString("data"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("length", list.size());
    }

}
