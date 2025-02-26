package wxdgaming.backends.mudole.slog.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.games.logs.SLog;
import wxdgaming.backends.entity.games.logs.SLog2Item;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.threading.ExecutorUtil;
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
@RequestMapping(path = "log/item")
public class SLogItemApi {

    final GameService gameService;
    final SLogService SLogService;

    @Inject
    public SLogItemApi(GameService gameService, SLogService SLogService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<SLog2Item> recordList) {
        ExecutorUtil.getInstance().getLogicExecutor().execute(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                for (SLog2Item record : recordList) {
                    push(gameContext, record);
                }
            }
        });
        return RunResult.ok();
    }

    /** 登录日志的批量提交 */
    @HttpRequest(authority = 2)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") SLog2Item record) {
        record.setLogType(record.tableName());
        if (record.getUid() == 0)
            record.setUid(gameContext.newId(record.tableName()));

        String logKey = record.tableName() + record.getUid();
        boolean haveLogKey = gameContext.getLogKeyCache().containsKey(logKey);
        if (haveLogKey) {
            gameContext.recordError("表结构 " + record.tableName() + " 重复日志记录 " + record.getUid(), record.toJsonString());
        } else {
            record.checkDataKey();
            AccountRecord accountRecord = gameContext.getAccountRecord(record.getAccount());
            if (accountRecord == null) {
                gameContext.recordError("道具记录 找不到账号 " + record.getAccount(), record.toJsonString());
                return RunResult.ok();
            }

            RoleRecord roleRecord = gameContext.getRoleRecord(record.getRoleId());
            if (roleRecord == null) {
                gameContext.recordError("道具记录 找不到角色 " + record.getRoleId(), record.toJsonString());
                return RunResult.ok();
            }

            gameContext.getLogKeyCache().put(logKey, true);
            gameContext.getDataHelper().getDataBatch().insert(record);
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    @ExecutorWith(useVirtualThread = true)
    public RunResult list(HttpContext httpSession,
                          @Param(path = "gameId") int gameId,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName,
                          @Param(path = "itemId", required = false) String itemId,
                          @Param(path = "itemName", required = false) String itemName,
                          @Param(path = "otherJson", required = false) String otherJson) {

        GameContext gameContext = gameService.gameContext(gameId);

        if (gameContext == null) {
            return RunResult.error("gameId error");
        }

        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
        SqlQueryBuilder sqlQueryBuilder = pgsqlDataHelper.queryBuilder();

        sqlQueryBuilder.sqlByEntity(SLog2Item.class)
                .pushWhereByValueNotNull("account=?", account)
                .pushWhereByValueNotNull("roleid=?", roleId)
                .pushWhereByValueNotNull("rolename=?", roleName);

        if (StringUtils.isNotBlank(itemId)) {
            sqlQueryBuilder.pushWhereByValueNotNull("itemid=?", NumberUtil.parseInt(itemId, 0));
        }
        sqlQueryBuilder.pushWhereByValueNotNull("itemname=?", itemName);
        if (StringUtils.isNotBlank(otherJson)) {
            String[] split = otherJson.split(",");
            for (String s : split) {
                String[] strings = s.split("=");
                sqlQueryBuilder.pushWhere("json_extract_path_text(other,'" + strings[0] + "') = ?", strings[1]);
            }
        }

        sqlQueryBuilder.setOrderBy("createtime desc");

        sqlQueryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);

        long rowCount = sqlQueryBuilder.findCount();

        List<SLog2Item> slogs = sqlQueryBuilder.findList2Entity(SLog2Item.class);

        List<JSONObject> list = slogs.stream()
                .map(SLog::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("other", jsonObject.getString("other"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }


}
