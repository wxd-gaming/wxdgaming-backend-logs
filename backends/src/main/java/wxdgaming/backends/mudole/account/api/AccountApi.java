package wxdgaming.backends.mudole.account.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.admin.game.GameExecutorEvent;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.mudole.srolelog.SLogService;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.format.TimeFormat;
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
 * 账号api
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 15:52
 **/
@Slf4j
@Singleton
@RequestMapping(path = "log/account")
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
        gameContext.submit(new GameExecutorEvent() {
            @Override public void onEvent() throws Exception {
                String account = data.getString("account");
                long createTime = data.getLongValue("createTime");
                AccountRecord entity = gameContext.accountGetOrCreate(account, createTime);
                JSONObject other = data.getJSONObject("other");
                entity.getOther().putAll(other);
            }
        });
        return RunResult.OK;
    }

    @HttpRequest(authority = 2)
    public RunResult pushList(@ThreadParam GameContext gameContext, @Param(path = "data") List<JSONObject> recordList) {

        for (JSONObject record : recordList) {
            push(gameContext, record);
        }
        return RunResult.OK;
    }

    /** 列表查询不要走缓存，否则会特别耗内存 */
    @HttpRequest(authority = 9)
    public RunResult list(HttpContext httpContext,
                          @Param(path = "gameId") Integer gameId,
                          @Param(path = "pageIndex") Integer pageIndex,
                          @Param(path = "pageSize") Integer pageSize,
                          @Param(path = "minDay", required = false) String minDay,
                          @Param(path = "maxDay", required = false) String maxDay,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "online", required = false) String online,
                          @Param(path = "rechargeAmount", required = false) String rechargeAmount,
                          @Param(path = "rechargeCount", required = false) String rechargeCount,
                          @Param(path = "other", required = false) String other
    ) {
        GameContext gameContext = gameService.gameContext(gameId);
        PgsqlDataHelper pgsqlDataHelper = gameContext.getDataHelper();

        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();
        queryBuilder
                .sqlByEntity(AccountRecord.class)
                .pushWhereByValueNotNull("account=?", account)
        ;

        if (StringUtils.isNotBlank(rechargeAmount)) {
            queryBuilder.pushWhereByValueNotNull("rechargeamount>=?", NumberUtil.parseLong(rechargeAmount, 0L));
        }

        if (StringUtils.isNotBlank(rechargeCount)) {
            queryBuilder.pushWhereByValueNotNull("rechargecount>=?", NumberUtil.parseInt(rechargeCount, 0));
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
        List<AccountRecord> records = queryBuilder.findList2Entity(AccountRecord.class);

        List<JSONObject> list = records.stream()
                .filter(accountRecord -> {
                    if (StringUtils.isNotBlank(online)) {
                        return online.equals("1") == accountRecord.online();
                    } else {
                        return true;
                    }
                })
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
                    jsonObject.put("online", accountRecord.online());
                    jsonObject.put("totalOnlineTime", new TimeFormat().addTime(jsonObject.getLong("totalOnlineTime") * 100).toString(TimeFormat.FormatInfo.All));
                    jsonObject.put("other", jsonObject.getString("other"));
                    return jsonObject;
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }
}
