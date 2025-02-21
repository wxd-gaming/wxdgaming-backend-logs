package wxdgaming.backends.mudole.role.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.system.User;
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

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
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
                    jsonObject.put("data", jsonObject.getString("data"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }

}
