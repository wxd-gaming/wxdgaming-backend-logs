package wxdgaming.backends.mudole.account.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.mudole.slog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.format.TimeFormat;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.Event;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.NumberUtil;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.List;

/**
 * 账号api
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 15:52
 **/
@Slf4j
@Singleton
@RequestMapping(path = "account")
public class AccountApi {

    final GameService gameService;
    final SLogService SLogService;

    @Inject
    public AccountApi(GameService gameService, SLogService SLogService) {
        this.gameService = gameService;
        this.SLogService = SLogService;
    }

    @HttpRequest(authority = 2)
    public RunResult push(@ThreadParam GameContext gameContext, @Param(path = "data") JSONObject data) {
        String account = data.getString("account");
        AccountRecord entity = gameContext.getAccountRecord(account);
        if (entity == null) {
            entity = new AccountRecord();
            Long uid = data.getLong("uid");
            entity.setUid(uid);
            if (entity.getUid() == 0) {
                entity.setUid(gameContext.getAccountHexId().newId());
            }
            long createTime = data.getLong("createTime");
            entity.setCreateTime(createTime);
            JSONObject other = data.getJSONObject("other");
            entity.getOther().putAll(other);
            entity.checkDataKey();
            gameContext.getAccountRecordJdbcCache().put(entity.getAccount(), entity);
        }
        return RunResult.ok();
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<JSONObject> recordList) {
        ExecutorUtil.getInstance().getLogicExecutor().execute(new Event(5000, 10000) {
            @Override public void onEvent() throws Exception {
                for (JSONObject record : recordList) {
                    push(gameContext, record);
                }
            }
        });
        return RunResult.ok();
    }

    /** 列表查询不要走缓存，否则会特别耗内存 */
    @HttpRequest(authority = 9)
    public RunResult list(HttpContext httpContext,
                          @Param(path = "gameId") int gameId,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "online", required = false) String online,
                          @Param(path = "rechargeAmount", required = false) String rechargeAmount,
                          @Param(path = "rechargeCount", required = false) String rechargeCount
    ) {
        GameContext gameContext = gameService.gameContext(gameId);
        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();

        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();
        queryBuilder
                .sqlByEntity(AccountRecord.class)
                .pushWhereByValueNotNull("account=?", account)
        ;

        if (StringUtils.isNotBlank(online)) {
            queryBuilder.pushWhereByValueNotNull("online=?", "1".equals(online));
        }

        if (StringUtils.isNotBlank(rechargeAmount)) {
            queryBuilder.pushWhereByValueNotNull("rechargeamount>=?", NumberUtil.parseLong(rechargeAmount, 0L));
        }

        if (StringUtils.isNotBlank(rechargeCount)) {
            queryBuilder.pushWhereByValueNotNull("rechargecount>=?", NumberUtil.parseInt(rechargeCount, 0));
        }

        queryBuilder.setOrderBy("createtime desc");

        if (pageIndex > 0) {
            queryBuilder.setSkip((pageIndex - 1) * pageSize);
        }

        if (pageSize <= 10) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;
        queryBuilder.setLimit(pageSize);

        long rowCount = queryBuilder.findCount();
        List<AccountRecord> records = queryBuilder.findList2Entity(AccountRecord.class);

        List<JSONObject> list = records.stream()
                .map(accountRecord -> {
                    AccountRecord accountRecordFind = gameContext.getAccountRecordJdbcCache().find(accountRecord.getAccount());
                    if (accountRecordFind != null) {
                        accountRecord = accountRecordFind;
                    }
                    JSONObject jsonObject = accountRecord.toJSONObject();
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", accountRecord.getCreateTime()));
                    jsonObject.put("roleCount", String.valueOf(accountRecord.getRoleList().size()));
                    jsonObject.put("rechargeFirstTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", accountRecord.getRechargeFirstTime()));
                    jsonObject.put("rechargeLastTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", accountRecord.getRechargeLastTime()));
                    jsonObject.put("totalOnlineTime", new TimeFormat().addTime(jsonObject.getLong("totalOnlineTime") * 100).toString(TimeFormat.FormatInfo.All));
                    jsonObject.put("other", jsonObject.getString("other"));
                    return jsonObject;
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }
}
