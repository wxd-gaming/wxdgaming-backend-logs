package wxdgaming.backends.mudole.log.api;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.RoleRecord;
import wxdgaming.backends.mudole.game.GameService;
import wxdgaming.backends.mudole.log.LogsService;
import wxdgaming.boot.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot.core.str.StringUtil;
import wxdgaming.boot.core.threading.ThreadInfo;
import wxdgaming.boot.net.controller.ann.Body;
import wxdgaming.boot.net.controller.ann.TextController;
import wxdgaming.boot.net.controller.ann.TextMapping;
import wxdgaming.boot.net.web.hs.HttpSession;

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

}
