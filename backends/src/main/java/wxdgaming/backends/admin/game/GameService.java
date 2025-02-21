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
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.reflect.ReflectContext;
import wxdgaming.boot2.core.timer.MyClock;
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

    private final ConcurrentHashMap<Integer, HexId> gameId2HexMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, PgsqlDataHelper> gameId2PgsqlDataHelperMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Game> gameId2GameRecordMap = new ConcurrentHashMap<>();

    final PgsqlService pgsqlService;
    final ReflectContext logReflectContext;

    @Inject
    public GameService(PgsqlService pgsqlService) {
        this.pgsqlService = pgsqlService;
        this.logReflectContext = ReflectContext.Builder.of(GameTableScan.class.getPackageName()).build();
    }

    @Start
    public void start() throws Exception {
        scheduled();
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
        initDataHelper(game);
        gameId2GameRecordMap.put(gameId, game);
        gameId2HexMap.putIfAbsent(gameId, new HexId(game.getUid()));
    }

    public long newId(int gameId) {
        HexId hexId = gameId2HexMap.computeIfAbsent(gameId, HexId::new);
        return hexId.newId();
    }

    public Game gameRecord(int gameId) {
        return gameId2GameRecordMap.get(gameId);
    }

    public PgsqlDataHelper pgsqlDataHelper(int gameId) {
        return gameId2PgsqlDataHelperMap.get(gameId);
    }

    public void initDataHelper(Game game) {
        PgsqlDataHelper dataHelper = gameId2PgsqlDataHelperMap.computeIfAbsent(game.getUid(), k -> {
            String dbName = "game_db_" + k;
            SqlConfig clone = pgsqlService.getSqlConfig().clone(dbName);
            clone.setScanPackage(GameTableScan.class.getPackageName());
            PgsqlDataHelper pgsqlDataHelper = new PgsqlDataHelper(clone);
            pgsqlDataHelper.start();
            return pgsqlDataHelper;
        });

        Map<String, String> dbTableMap = dataHelper.findTableMap();
        Map<String, LinkedHashMap<String, JSONObject>> tableStructMap = dataHelper.findTableStructMap();
        this.logReflectContext.withAnnotated(DbTable.class)
                .filter(content -> RecordBase.class.isAssignableFrom(content.getCls()))
                .forEach(content -> {
                    TableMapping tableMapping = dataHelper.tableMapping((Class) content.getCls());
                    checkSLogTable(
                            dataHelper,
                            dbTableMap, tableStructMap,
                            tableMapping, RecordBase.class.isAssignableFrom(content.getCls()),
                            tableMapping.getTableName(), tableMapping.getTableComment()
                    );
                });

        for (Map.Entry<String, String> entry : game.getTableMapping().entrySet()) {
            String tableName = entry.getKey();
            String tableComment = entry.getValue();
            TableMapping tableMapping = dataHelper.tableMapping(SLog.class);
            checkSLogTable(dataHelper, dbTableMap, tableStructMap, tableMapping, true, tableName, tableComment);
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
            for (int i = 0; i < 125; i++) {
                /*创建表分区*/
                String from = MyClock.formatDate("yyyyMMdd", localDate);
                localDate = localDate.plusDays(1);
                String to = MyClock.formatDate("yyyyMMdd", localDate);
                dataHelper.addPartition(dbTableMap, tableName, from, to);
            }
        }
    }

    public RunResult checkAppToken(Integer gameId, String token) {
        if (token == null)
            return RunResult.error("token is null");

        Game game = gameId2GameRecordMap.get(gameId);
        if (game == null) {
            return RunResult.error("gameId is not exist");
        }

        if (!game.getAppToken().equals(token))
            return RunResult.error("token is not match");

        return null;
    }

}
