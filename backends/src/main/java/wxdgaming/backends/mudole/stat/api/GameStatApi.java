package wxdgaming.backends.mudole.stat.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.GameStat;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
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
@RequestMapping(path = "stat/game")
public class GameStatApi {

    final GameService gameService;

    @Inject
    public GameStatApi(GameService gameService) {
        this.gameService = gameService;
    }


    @HttpRequest(authority = 9)
    public RunResult list(HttpContext httpContext, @Param(path = "gameId") Integer gameId,
                          @Param(path = "pageIndex") Integer pageIndex,
                          @Param(path = "pageSize") Integer pageSize,
                          @Param(path = "minDay", required = false) String minDay,
                          @Param(path = "maxDay", required = false) String maxDay) {

        // if ((StringUtils.isNotBlank(day) && StringUtils.isNotBlank(minDay))
        //     || (StringUtils.isNotBlank(day) && StringUtils.isNotBlank(maxDay))
        //     || (StringUtils.isNotBlank(minDay) && StringUtils.isNotBlank(maxDay))) {
        //     return RunResult.error("日期参数只能有一个选项");
        // }

        PgsqlDataHelper pgsqlDataHelper = gameService.gameContext(gameId).getDataHelper();
        SqlQueryBuilder queryBuilder = pgsqlDataHelper.queryBuilder();
        queryBuilder.sqlByEntity(GameStat.class);

        if (StringUtils.isNotBlank(minDay)) {
            queryBuilder.pushWhereByValueNotNull("uid>=?", NumberUtil.retainNumber(minDay));
        }

        if (StringUtils.isNotBlank(maxDay)) {
            queryBuilder.pushWhereByValueNotNull("uid<=?", NumberUtil.retainNumber(maxDay));
        }

        queryBuilder.setOrderBy("uid desc");

        long rowCount = queryBuilder.findCount();


        queryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);

        List<JSONObject> list = queryBuilder.findList2Entity(GameStat.class)
                .stream()
                .map(GameStat::toJSONObject)
                .peek(jsonObject -> {
                    jsonObject.put("rechargeAmountNum", jsonObject.getLongValue("rechargeAmountNum") / 100f);
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", rowCount);
    }
}
