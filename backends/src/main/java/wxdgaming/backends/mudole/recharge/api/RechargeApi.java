package wxdgaming.backends.mudole.recharge.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.RechargeRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.games.ServerRecord;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
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
 * 充值接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-20 21:00
 **/
@Slf4j
@Singleton
@RequestMapping(path = "log/recharge")
public class RechargeApi {

    final GameService gameService;

    @Inject
    public RechargeApi(GameService gameService) {
        this.gameService = gameService;
    }

    @HttpRequest(authority = 2)
    @ExecutorWith(useVirtualThread = true)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") RechargeRecord record) {
        gameContext.submit(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                if (record.getUid() == 0) {
                    record.setUid(gameContext.getRechargeHexId().newId());
                }
                record.checkDataKey();
                String logKey = "recharge" + record.getUid();
                boolean containsKey = gameContext.getLogKeyCache().has(logKey);
                if (!containsKey) {
                    gameContext.getLogKeyCache().put(logKey, true);
                    gameContext.getDataHelper().dataBatch().insert(record);
                    {
                        String account = record.getAccount();
                        AccountRecord accountRecord = gameContext.accountGetOrCreate(account);
                        accountRecord.getRechargeAmount().addAndGet(record.getAmount());
                        accountRecord.getRechargeCount().incrementAndGet();
                        if (accountRecord.getRechargeFirstTime() == 0) {
                            accountRecord.setRechargeFirstTime(record.getCreateTime());
                        }
                        accountRecord.setRechargeLastTime(record.getCreateTime());
                    }
                    RoleRecord roleRecord = gameContext.roleGetOrCreate(record.getAccount(), record.getRoleId());
                    roleRecord.getRechargeAmount().addAndGet(record.getAmount());
                    roleRecord.getRechargeCount().incrementAndGet();
                    if (roleRecord.getRechargeFirstTime() == 0) {
                        roleRecord.setRechargeFirstTime(record.getCreateTime());
                    }
                    roleRecord.setRechargeLastTime(record.getCreateTime());

                    ServerRecord serverRecord = gameContext.serverGetOrCreate(record.getSid());
                    serverRecord.getRechargeCount().incrementAndGet();
                    serverRecord.getRechargeAmount().addAndGet(record.getAmount())
                    ;
                    gameContext.getDataHelper().getDataBatch().save(serverRecord);

                } else {
                    gameContext.recordError("重复充值记录 " + record.getUid(), record.toJSONString());
                }
            }
        });
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<RechargeRecord> recordList) {

        for (RechargeRecord record : recordList) {
            push(gameContext, record);
        }
        return RunResult.OK;
    }

    @HttpRequest(authority = 9)
    public RunResult group(HttpContext httpContext,
                           @ThreadParam GameContext gameContext,
                           @Param(path = "minDay", required = false) String minDay,
                           @Param(path = "maxDay", required = false) String maxDay,
                           @Param(path = "other", required = false) String other) {

        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
        Object[] args = Objects.ZERO_ARRAY;
        String sql = "SELECT rr.amount, \"count\" (rr.amount) FROM record_recharge as rr";
        String sqlWhere = "";
        if (StringUtils.isNotBlank(minDay)) {
            sqlWhere += "daykey >= ?";
            String string = StringUtils.retainNumbers(minDay);
            int anInt = NumberUtil.parseInt(string, 0);
            args = Objects.merge(args, anInt);
        }

        if (StringUtils.isNotBlank(maxDay)) {
            if (StringUtils.isNotBlank(sqlWhere)) {
                sqlWhere += " AND ";
            }
            sqlWhere += "daykey <= ?";
            String string = StringUtils.retainNumbers(maxDay);
            int anInt = NumberUtil.parseInt(string, 0);
            args = Objects.merge(args, anInt);
        }

        if (StringUtils.isNotBlank(other)) {
            String[] split = other.split(",");
            for (String s : split) {
                String[] strings = s.split("=");
                if (StringUtils.isNotBlank(sqlWhere)) {
                    sqlWhere += " AND ";
                }
                sqlWhere += "json_extract_path_text(other,'" + strings[0] + "') = ?";
                args = Objects.merge(args, strings[1]);
            }
        }

        if (StringUtils.isNotBlank(sqlWhere)) {
            sql += " WHERE " + sqlWhere;
        }

        sql += " GROUP BY rr.amount ORDER BY rr.amount";

        List<JSONObject> jsonObjects = pgsqlDataHelper.queryList(sql, args);

        jsonObjects.sort((o1, o2) -> Long.compare(o2.getLongValue("count"), o1.getLongValue("count")));

        Object[] objectsTitle = new Object[jsonObjects.size()];
        Object[] objectsValue = new Object[jsonObjects.size()];

        for (int i = 0; i < jsonObjects.size(); i++) {
            JSONObject jsonObject = jsonObjects.get(i);
            objectsTitle[i] = jsonObject.getIntValue("amount") / 100;
            objectsValue[i] = jsonObject.getLongValue("count");
        }
        return RunResult.ok().data(new Object[]{objectsTitle, objectsValue});
    }

    @HttpRequest
    public RunResult list(HttpContext httpContext,
                          @ThreadParam GameContext gameContext,
                          @Param(path = "pageIndex") Integer pageIndex,
                          @Param(path = "pageSize") Integer pageSize,
                          @Param(path = "minDay", required = false) String minDay,
                          @Param(path = "maxDay", required = false) String maxDay,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName,
                          @Param(path = "curSid", required = false) String curSid,
                          @Param(path = "createSid", required = false) String createSid,
                          @Param(path = "spOrder", required = false) String spOrder,
                          @Param(path = "cpOrder", required = false) String cpOrder,
                          @Param(path = "other", required = false) String other) {

        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();
        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();
        queryBuilder.sqlByEntity(RechargeRecord.class);
        queryBuilder.pushWhereByValueNotNull("account = ?", account);
        queryBuilder.pushWhereByValueNotNull("roleid = ?", roleId);
        queryBuilder.pushWhereByValueNotNull("rolename = ?", roleName);
        queryBuilder.pushWhereByValueNotNull("sporder = ?", spOrder);
        queryBuilder.pushWhereByValueNotNull("cporder = ?", cpOrder);
        if (StringUtils.isNotBlank(curSid)) {
            queryBuilder.pushWhereByValueNotNull("cursid=?", NumberUtil.parseInt(curSid, 0));
        }
        if (StringUtils.isNotBlank(createSid)) {
            queryBuilder.pushWhereByValueNotNull("createsid=?", NumberUtil.parseInt(createSid, 0));
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
        List<JSONObject> list = queryBuilder.findList2Entity(RechargeRecord.class)
                .stream()
                .map(RechargeRecord::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("other", jsonObject.getString("other"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }


}
