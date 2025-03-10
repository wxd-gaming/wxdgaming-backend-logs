package wxdgaming.backends.admin.game;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.games.GameTableScan;
import wxdgaming.backends.entity.games.logs.RecordBase;
import wxdgaming.backends.entity.games.logs.SLog;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.backends.entity.system.GlobalData;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.reflect.ReflectContext;
import wxdgaming.boot2.core.shutdown;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.TableMapping;
import wxdgaming.boot2.starter.batis.ann.DbTable;
import wxdgaming.boot2.starter.batis.sql.SqlConfig;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
                            gameContext.getDataHelper(),
                            dbTableMap, tableStructMap,
                            tableMapping, RecordBase.class.isAssignableFrom(content.getCls()),
                            tableMapping.getTableName(), tableMapping.getTableComment()
                    );
                });

        for (Map.Entry<String, String> entry : gameContext.getGame().getTableMapping().entrySet()) {
            String tableName = entry.getKey();
            String tableComment = entry.getValue();
            TableMapping tableMapping = gameContext.getDataHelper().tableMapping(SLog.class);
            checkSLogTable(gameContext.getDataHelper(), dbTableMap, tableStructMap, tableMapping, true, tableName, tableComment);
        }
    }

    public void checkSLogTable(PgsqlDataHelper dataHelper, String tableName, String tableComment) {
        Map<String, String> dbTableMap = dataHelper.findTableMap();
        Map<String, LinkedHashMap<String, JSONObject>> tableStructMap = dataHelper.findTableStructMap();
        TableMapping tableMapping = dataHelper.tableMapping(SLog.class);
        checkSLogTable(dataHelper, dbTableMap, tableStructMap, tableMapping, true, tableName, tableComment);
    }

    public void checkSLogTable(PgsqlDataHelper dataHelper,
                               Map<String, String> dbTableMap,
                               Map<String, LinkedHashMap<String, JSONObject>> tableStructMap,
                               TableMapping tableMapping,
                               boolean checkPartition,
                               String tableName,
                               String tableComment) {

        dataHelper.checkTable(tableStructMap, tableMapping, tableName, tableComment);
        if (checkPartition) {
            /*TODO 处理分区表 */
            LocalDateTime localDate = LocalDateTime.now().plusDays(-120);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < 125; i++) {
                final int fi = i;
                CompletableFuture<Void> voidCompletableFuture = ExecutorUtil.getInstance().getLogicExecutor().completableFuture(() -> {
                    /*创建表分区*/
                    String from = MyClock.formatDate("yyyyMMdd", localDate.plusDays(fi));
                    String to = MyClock.formatDate("yyyyMMdd", localDate.plusDays(fi + 1));
                    dataHelper.addPartition(dbTableMap, tableName, from, to);
                }, dataHelper.getDbName() + " 添加分区 " + tableName, 10000, 100000);
                futures.add(voidCompletableFuture);
            }
            futures.forEach(CompletableFuture::join);
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
