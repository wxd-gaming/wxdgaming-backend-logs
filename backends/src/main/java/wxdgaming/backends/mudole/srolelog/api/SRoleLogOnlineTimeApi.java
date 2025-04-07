package wxdgaming.backends.mudole.srolelog.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.logs.OnlineTimeRecord;
import wxdgaming.boot2.core.ann.Param;
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
 * 在线时长接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-24 20:48
 **/
@Slf4j
@Singleton
@RequestMapping(path = "log/role/online")
public class SRoleLogOnlineTimeApi {

    private final GameService gameService;

    @Inject
    public SRoleLogOnlineTimeApi(GameService gameService) {
        this.gameService = gameService;
    }

    @HttpRequest()
    public RunResult list(HttpContext httpSession,
                          @Param(path = "gameId") Integer gameId,
                          @Param(path = "pageIndex") Integer pageIndex,
                          @Param(path = "pageSize") Integer pageSize,
                          @Param(path = "minDay", required = false) String minDay,
                          @Param(path = "maxDay", required = false) String maxDay,
                          @Param(path = "account", required = false) String account,
                          @Param(path = "roleId", required = false) String roleId,
                          @Param(path = "roleName", required = false) String roleName) {

        GameContext gameContext = gameService.gameContext(gameId);
        PgsqlDataHelper dataHelper = gameContext.getDataHelper();
        SqlQueryBuilder queryBuilder = dataHelper.queryBuilder();

        queryBuilder.sqlByEntity(OnlineTimeRecord.class);

        queryBuilder.pushWhereByValueNotNull("account=?", account);
        if (StringUtils.isNotBlank(roleId)) {
            queryBuilder.pushWhereByValueNotNull("roleid=?", NumberUtil.parseLong(roleId, 0L));
        }
        queryBuilder.pushWhereByValueNotNull("rolename=?", roleName);

        if (StringUtils.isNotBlank(minDay)) {
            queryBuilder.pushWhereByValueNotNull("daykey>=?", NumberUtil.retainNumber(minDay));
        }

        if (StringUtils.isNotBlank(maxDay)) {
            queryBuilder.pushWhereByValueNotNull("daykey<=?", NumberUtil.retainNumber(maxDay));
        }
        queryBuilder.setOrderBy("createtime desc");

        queryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);


        long count = queryBuilder.findCount();
        List<OnlineTimeRecord> list2Entity = queryBuilder.findList2Entity(OnlineTimeRecord.class);
        List<JSONObject> list = list2Entity.stream()
                .map(onlineTimeRecord -> {
                    JSONObject jsonObject = onlineTimeRecord.toJSONObject();
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", onlineTimeRecord.getCreateTime()));
                    jsonObject.put("joinTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", onlineTimeRecord.getJoinTime()));
                    jsonObject.put("exitTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", onlineTimeRecord.getExitTime()));
                    jsonObject.put("onlineTime", new TimeFormat().addTime(onlineTimeRecord.getOnlineTime() * 100).toString(TimeFormat.FormatInfo.All));
                    jsonObject.put("totalOnlineTime", new TimeFormat().addTime(onlineTimeRecord.getTotalOnlineTime() * 100).toString(TimeFormat.FormatInfo.All));
                    return jsonObject;
                })
                .toList();
        return RunResult.ok().data(list).fluentPut("rowCount", count);
    }

}
