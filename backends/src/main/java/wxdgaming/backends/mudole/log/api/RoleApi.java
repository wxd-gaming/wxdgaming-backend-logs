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
    public String push(HttpSession session, @Body RoleRecord roleRecord) {

        log.info("role - {}", roleRecord.toJson());

        if (roleRecord.getGameId() == 0) return "gameId is null";
        if (StringUtil.emptyOrNull(roleRecord.getToken())) return "token is null";

        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(roleRecord.getGameId());

        RoleRecord entity = pgsqlDataHelper.queryEntityByWhere(
                RoleRecord.class,
                "account = ? AND  roleid= ? AND createsid=?",
                roleRecord.getAccount(), roleRecord.getRoleId(), roleRecord.getCreateSid()
        );

        roleRecord.setUpdateTime(System.currentTimeMillis());
        if (entity == null) {
            if (roleRecord.getUid() == 0) {
                long newId = gameService.newId(roleRecord.getGameId());
                roleRecord.setUid(newId);
            }
            if (roleRecord.getCreateTime() == 0) {
                roleRecord.setCreateTime(System.currentTimeMillis());
            }
            pgsqlDataHelper.insert(roleRecord);
        } else {
            roleRecord.setUid(entity.getUid());
            roleRecord.setCreateTime(Math.min(roleRecord.getCreateTime(), entity.getCreateTime()));
            roleRecord.setCreateSid(entity.getCreateSid());
            pgsqlDataHelper.update(roleRecord);
        }
        return "ok";
    }

    @TextMapping
    public RunResult list(HttpSession httpSession,
                          @Param("gameId") Integer gameId,
                          @Param(value = "search", required = false) String search) {
        PgsqlDataHelper pgsqlDataHelper = gameService.pgsqlDataHelper(gameId);
        List<RoleRecord> accountRecords;
        if (StringUtil.emptyOrNull(search)) {
            accountRecords = pgsqlDataHelper.queryEntities(RoleRecord.class);
        } else {
            accountRecords = pgsqlDataHelper.queryEntitiesWhere(RoleRecord.class, "account = ?", search);
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
