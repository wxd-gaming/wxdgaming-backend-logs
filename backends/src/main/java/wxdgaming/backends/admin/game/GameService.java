package wxdgaming.backends.admin.game;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.games.GameTableScan;
import wxdgaming.backends.entity.games.logs.RecordBase;
import wxdgaming.backends.entity.games.logs.SRoleLog;
import wxdgaming.backends.entity.games.logs.SServerLog;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.backends.entity.system.GlobalData;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.reflect.ReflectContext;
import wxdgaming.boot2.core.shutdown;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.Entity;
import wxdgaming.boot2.starter.batis.TableMapping;
import wxdgaming.boot2.starter.batis.ann.DbTable;
import wxdgaming.boot2.starter.batis.sql.SqlConfig;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 17:22
 **/
@Slf4j
@Getter
@Singleton
public class GameService {

    final ConcurrentHashMap<Integer, GameContext> gameContextHashMap = new ConcurrentHashMap<>();

    GlobalData globalData;
    final PgsqlService pgsqlService;
    final ReflectContext logReflectContext;

    @Inject
    public GameService(PgsqlService pgsqlService) {
        this.pgsqlService = pgsqlService;
        this.logReflectContext = ReflectContext.Builder.of(GameTableScan.class.getPackageName()).build();
    }

    @Start
    public void start() throws Exception {
        globalData = this.pgsqlService.findByKey(GlobalData.class, 1);
        if (globalData == null) {
            globalData = new GlobalData();
            globalData.setUid(1);
            globalData.setNewEntity(true);
            this.pgsqlService.insert(globalData);
        }
        scheduled();
    }

    @shutdown
    public void shutdown() {
        this.pgsqlService.update(globalData);
        gameContextHashMap.forEach((k, v) -> v.shutdown());
    }

    public int newGameId() {
        int incremented;
        do {
            incremented = globalData.getNewGameId().incrementAndGet();
        } while (gameContextHashMap.containsKey(incremented));
        return incremented;
    }

    /** 每日凌晨检查数据库，检查表分区信息 */
    @Scheduled("0 0 0")
    public void scheduled() {
        List<Game> games = pgsqlService.findList(Game.class);
        for (Game game : games) {
            addGameCache(game);
        }
    }

    public void addGameCache(Game game) {
        int gameId = game.getUid();
        GameContext gameContext = gameContextHashMap.computeIfAbsent(gameId, k -> {

            String dbName = "game_db_" + k;
            SqlConfig clone = pgsqlService.getSqlConfig().clone(dbName);
            clone.setScanPackage(GameTableScan.class.getPackageName());
            PgsqlDataHelper pgsqlDataHelper = new PgsqlDataHelper(clone);
            pgsqlDataHelper.start();
            return new GameContext(game, pgsqlDataHelper);
        });
        initDataHelper(gameContext);
    }

    public GameContext gameContext(int gameId) {
        return gameContextHashMap.get(gameId);
    }

    public void initDataHelper(GameContext gameContext) {

        Map<String, String> dbTableMap = gameContext.getDataHelper().findTableMap();
        Map<String, LinkedHashMap<String, JSONObject>> tableStructMap = gameContext.getDataHelper().findTableStructMap();
        this.logReflectContext.withAnnotated(DbTable.class)
                .filter(content -> RecordBase.class.isAssignableFrom(content.getCls()))
                .forEach(content -> {
                    TableMapping tableMapping = gameContext.getDataHelper().tableMapping((Class) content.getCls());
                    checkSLogTable(
                            gameContext,
                            gameContext.getDataHelper(),
                            dbTableMap, tableStructMap,
                            tableMapping, RecordBase.class.isAssignableFrom(content.getCls()),
                            tableMapping.getTableName(), tableMapping.getTableComment()
                    );
                });

        for (Map.Entry<String, String> entry : gameContext.getGame().getRoleTableMapping().entrySet()) {
            String tableName = entry.getKey();
            String tableComment = entry.getValue();
            TableMapping tableMapping = gameContext.getDataHelper().tableMapping(SRoleLog.class);
            checkSLogTable(gameContext, gameContext.getDataHelper(), dbTableMap, tableStructMap, tableMapping, true, tableName, tableComment);
        }

        for (Map.Entry<String, String> entry : gameContext.getGame().getServerTableMapping().entrySet()) {
            String tableName = entry.getKey();
            String tableComment = entry.getValue();
            TableMapping tableMapping = gameContext.getDataHelper().tableMapping(SServerLog.class);
            gameContext.getDataHelper().checkTable(tableStructMap, tableMapping, tableName, tableComment);
        }

    }

    public void checkSLogTable(GameContext gameContext, PgsqlDataHelper dataHelper, Class<? extends Entity> tableClass, String tableName, String tableComment) {
        Map<String, String> dbTableMap = dataHelper.findTableMap();
        Map<String, LinkedHashMap<String, JSONObject>> tableStructMap = dataHelper.findTableStructMap();
        TableMapping tableMapping = dataHelper.tableMapping(tableClass);
        checkSLogTable(gameContext, dataHelper, dbTableMap, tableStructMap, tableMapping, false, tableName, tableComment);
    }

    public void checkSLogTable(GameContext gameContext, PgsqlDataHelper dataHelper,
                               Map<String, String> dbTableMap,
                               Map<String, LinkedHashMap<String, JSONObject>> tableStructMap,
                               TableMapping tableMapping,
                               boolean checkPartition,
                               String tableName,
                               String tableComment) {

        dataHelper.checkTable(tableStructMap, tableMapping, tableName, tableComment);
        if (checkPartition) {
            /*TODO 处理分区表 */
            LocalDateTime localDate = LocalDateTime.now().plusDays(-2);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                /*创建表分区*/
                String from = MyClock.formatDate("yyyyMMdd", localDate.plusDays(i));
                String to = MyClock.formatDate("yyyyMMdd", localDate.plusDays(i + 1));

                String partition_table_name = tableName + "_" + from;
                if (dbTableMap.containsKey(partition_table_name)) {
                    continue;
                }
                sb.append(dataHelper.buildPartition(tableName, from, to)).append("\n");
            }
            dataHelper.executeUpdate(sb.toString());
        }
    }

    public RunResult checkAppToken(Integer gameId, String token) {
        if (token == null)
            return RunResult.error("token is null");

        GameContext gameContext = gameContextHashMap.get(gameId);
        if (gameContext == null) {
            return RunResult.error("gameId is not exist");
        }

        if (!gameContext.getGame().getAppToken().equals(token))
            return RunResult.error("token is not match");

        return null;
    }

}
