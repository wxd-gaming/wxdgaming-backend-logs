package wxdgaming.backends.mudole.recharge.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.ErrorRecord;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.RechargeRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.boot2.core.ann.Body;
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
 * 充值接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-20 21:00
 **/
@Slf4j
@Singleton
@RequestMapping(path = "recharge")
public class RechargeApi {

    final GameService gameService;

    @Inject
    public RechargeApi(GameService gameService) {
        this.gameService = gameService;
    }

    @HttpRequest(authority = 2)
    @ExecutorWith(useVirtualThread = true)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") RechargeRecord record) {
        if (record.getUid() == 0) {
            record.setUid(gameContext.getRechargeHexId().newId());
        }
        record.checkDataKey();
        String logKey = "recharge" + record.getUid();
        boolean containsKey = gameContext.getLogKeyCache().containsKey(logKey);
        if (!containsKey) {
            gameContext.getLogKeyCache().put(logKey, true);
            gameContext.getDataHelper().dataBatch().insert(record);
            String account = record.getAccount();
            AccountRecord accountRecord = gameContext.getAccountRecord(account);
            if (accountRecord != null) {
                accountRecord.getRechargeAmount().addAndGet(record.getAmount());
                accountRecord.getRechargeCount().incrementAndGet();
                if (accountRecord.getRechargeFirstTime() == 0) {
                    accountRecord.setRechargeFirstTime(record.getCreateTime());
                }
                accountRecord.setRechargeLastTime(record.getCreateTime());
            } else {
                gameContext.recordError(record.toJsonString(), "重复充值记录 找不到账号 " + record.getAccount());
            }
            long roleId = record.getRoleId();
            RoleRecord roleRecord = gameContext.getRoleRecord(roleId);
            if (roleRecord != null) {
                roleRecord.getRechargeAmount().addAndGet(record.getAmount());
                roleRecord.getRechargeCount().incrementAndGet();
                if (roleRecord.getRechargeFirstTime() == 0) {
                    roleRecord.setRechargeFirstTime(record.getCreateTime());
                }
                roleRecord.setRechargeLastTime(record.getCreateTime());
            } else {
                gameContext.recordError(record.toJsonString(), "重复充值记录 找不到对应的角色 " + record.getRoleId());
            }
        } else {
            gameContext.recordError(record.toJsonString(), "重复充值记录 " + record.getUid());
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<RechargeRecord> recordList) {
        ExecutorUtil.getInstance().getVirtualExecutor().execute(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                for (RechargeRecord record : recordList) {
                    push(gameContext, record);
                }
            }
        });
        return RunResult.ok();
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
                          @Param(path = "createSid", required = false) String createSid,
                          @Param(path = "spOrder", required = false) String spOrder,
                          @Param(path = "cpOrder", required = false) String cpOrder) {

        PgsqlDataHelper pgsqlDataHelper = gameService.gameContext(gameId).getDataHelper();
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

        queryBuilder.setOrderBy("createtime desc");

        queryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);

        long rowCount = queryBuilder.findCount();
        List<JSONObject> list = queryBuilder.findList2Entity(RechargeRecord.class)
                .stream()
                .map(RechargeRecord::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", jsonObject.getLong("createTime")));
                    jsonObject.put("data", jsonObject.getString("data"));
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }


}
