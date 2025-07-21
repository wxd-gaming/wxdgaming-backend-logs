package wxdgaming.backends.mudole.srolelog.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.admin.game.GameExecutorEvent;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.games.logs.SRoleLog;
import wxdgaming.backends.entity.games.logs.SRoleLog2Item;
import wxdgaming.backends.mudole.srolelog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.executor.ExecutorWith;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
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
@RequestMapping(path = "log/role/item")
public class SRoleLogItemApi {

    final GameService gameService;
    final SLogService SLogService;

    @Inject
    public SRoleLogItemApi(GameService gameService, SLogService SLogService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<SRoleLog2Item> recordList) {

        for (SRoleLog2Item record : recordList) {
            push(gameContext, record);
        }
        return RunResult.OK;
    }

    /** 登录日志的批量提交 */
    @HttpRequest(authority = 2)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") SRoleLog2Item record) {
        log.info("item log: {}", record.toJSONString());
        gameContext.submit(new GameExecutorEvent() {
            @Override public void onEvent() throws Exception {
                AccountRecord accountRecord = gameContext.accountGetOrCreate(record.getAccount());
                RoleRecord roleRecord = gameContext.roleGetOrCreate(record.getAccount(), record.getRoleId());
                record.setLogType(record.tableName());
                if (record.getUid() == 0)
                    record.setUid(gameContext.newId(record.tableName()));

                String logKey = record.tableName() + record.getUid();
                boolean haveLogKey = gameContext.getLogKeyCache().has(logKey);
                if (haveLogKey) {
                    gameContext.recordError("表结构 " + record.tableName() + " 重复日志记录 " + record.getUid(), record.toJSONString());
                } else {
                    record.checkDataKey();

                    gameContext.getLogKeyCache().put(logKey, true);
                    gameContext.getDataHelper().getDataBatch().insert(record);
                }
            }
        });
        return RunResult.OK;
    }

    @HttpRequest(authority = 9)
    public RunResult group(HttpContext httpContext,
                           @ThreadParam GameContext gameContext,
                           @Param(path = "changeType") String changeType,
                           @Param(path = "minDay", required = false) String minDay,
                           @Param(path = "maxDay", required = false) String maxDay,
                           @Param(path = "other", required = false) String other) {

        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
        Object[] args = Objects.ZERO_ARRAY;

        String sqlWhere = "";

        sqlWhere += "changeType = ?";
        args = Objects.merge(args, changeType);

        if (StringUtils.isNotBlank(minDay)) {
            sqlWhere += " AND ";
            sqlWhere += "daykey >= ?";
            String string = StringUtils.retainNumbers(minDay);
            int anInt = NumberUtil.parseInt(string, 0);
            args = Objects.merge(args, anInt);
        }

        if (StringUtils.isNotBlank(maxDay)) {
            sqlWhere += " AND ";
            sqlWhere += "daykey <= ?";
            String string = StringUtils.retainNumbers(maxDay);
            int anInt = NumberUtil.parseInt(string, 0);
            args = Objects.merge(args, anInt);
        }

        if (StringUtils.isNotBlank(other)) {
            String[] split = other.split(",");
            for (String s : split) {
                String[] strings = s.split("=");
                sqlWhere += " AND ";
                sqlWhere += "json_extract_path_text(other,'" + strings[0] + "') = ?";
                args = Objects.merge(args, strings[1]);
            }
        }

        String sql = """
                SELECT
                ri.itemid,
                "min"(ri.itemname),
                "sum"(ri.change)
                FROM
                record_role_item AS ri
                WHERE %s
                GROUP BY ri.itemid
                ORDER BY ri.itemid
                """.formatted(sqlWhere);

        List<JSONObject> jsonObjects = pgsqlDataHelper.queryList(sql, args);

        jsonObjects.sort((o1, o2) -> Long.compare(o2.getLongValue("sum"), o1.getLongValue("sum")));

        Object[] objectsTitle = new Object[jsonObjects.size()];
        Object[] objectsValue = new Object[jsonObjects.size()];

        for (int i = 0; i < jsonObjects.size(); i++) {
            JSONObject jsonObject = jsonObjects.get(i);
            objectsTitle[i] = "%s(%s)".formatted(jsonObject.getString("min"), jsonObject.getString("itemid"));
            objectsValue[i] = jsonObject.getLongValue("sum");
        }
        return RunResult.ok().data(new Object[]{objectsTitle, objectsValue});
    }

    @HttpRequest(authority = 9)
    @ExecutorWith(useVirtualThread = true)
    public RunResult list(HttpContext httpSession,
                          @Param(path = "gameId") Integer gameId,
                          @Param(path = "pageIndex") Integer pageIndex,
                          @Param(path = "pageSize") Integer pageSize,
                          @Param(path = "minDay", required = false) String minDay,
                          @Param(path = "maxDay", required = false) String maxDay,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName,
                          @Param(path = "itemId", required = false) String itemId,
                          @Param(path = "itemName", required = false) String itemName,
                          @Param(path = "other", required = false) String other) {

        GameContext gameContext = gameService.gameContext(gameId);

        if (gameContext == null) {
            return RunResult.fail("gameId error");
        }

        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();

        queryBuilder.sqlByEntity(SRoleLog2Item.class)
                .pushWhereByValueNotNull("account=?", account)
                .pushWhereByValueNotNull("roleid=?", roleId)
                .pushWhereByValueNotNull("rolename=?", roleName);

        if (StringUtils.isNotBlank(minDay)) {
            queryBuilder.pushWhereByValueNotNull("daykey>=?", NumberUtil.retainNumber(minDay));
        }

        if (StringUtils.isNotBlank(maxDay)) {
            queryBuilder.pushWhereByValueNotNull("daykey<=?", NumberUtil.retainNumber(maxDay));
        }

        if (StringUtils.isNotBlank(itemId)) {
            queryBuilder.pushWhereByValueNotNull("itemid=?", NumberUtil.parseInt(itemId, 0));
        }

        queryBuilder.pushWhereByValueNotNull("itemname=?", itemName);

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

        List<SRoleLog2Item> slogs = queryBuilder.findList2Entity(SRoleLog2Item.class);

        List<JSONObject> list = slogs.stream()
                .map(SRoleLog::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("other", jsonObject.getString("other"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }


}
