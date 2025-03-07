package wxdgaming.backends.jsplugin;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.js.IJSPlugin;

import java.util.List;

/**
 * sql 查询
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-06 21:23
 **/
@Singleton
public class JsSqlQuery implements IJSPlugin {

    final GameService gameService;

    @Inject
    public JsSqlQuery(GameService gameService) {
        this.gameService = gameService;
    }

    @Override public String getName() {
        return "SqlQuery";
    }

    public List<JSONObject> queryList(int gameId, String sql, Object... args) {
        GameContext gameContext = gameService.gameContext(gameId);
        PgsqlDataHelper dataHelper = gameContext.getDataHelper();
        return dataHelper.queryList(sql, args);
    }

    public JSONObject queryOne(int gameId, String sql, Object... args) {
        GameContext gameContext = gameService.gameContext(gameId);
        PgsqlDataHelper dataHelper = gameContext.getDataHelper();
        return dataHelper.queryTop(sql, args);
    }

    public Object executeScalar(int gameId, String sql, Object... args) {
        GameContext gameContext = gameService.gameContext(gameId);
        PgsqlDataHelper dataHelper = gameContext.getDataHelper();
        return dataHelper.executeScalar(sql, Object.class, args);
    }

}
