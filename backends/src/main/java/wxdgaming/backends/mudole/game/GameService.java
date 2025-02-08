package wxdgaming.backends.mudole.game;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.LogScan;
import wxdgaming.backends.entity.logs.SLog;
import wxdgaming.backends.entity.system.GameRecord;
import wxdgaming.boot.batis.DbConfig;
import wxdgaming.boot.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot.core.format.HexId;
import wxdgaming.boot.core.lang.RunResult;
import wxdgaming.boot.core.timer.MyClock;
import wxdgaming.boot.core.timer.ann.Scheduled;
import wxdgaming.boot.starter.IocContext;
import wxdgaming.boot.starter.i.IStart;
import wxdgaming.boot.starter.pgsql.PgsqlService;

import java.time.LocalDateTime;
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
public class GameService implements IStart {

    private final ConcurrentHashMap<Integer, HexId> gameId2HexMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, PgsqlDataHelper> gameId2PgsqlDataHelperMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, GameRecord> gameId2GameRecordMap = new ConcurrentHashMap<>();

    final PgsqlService pgsqlService;

    @Inject
    public GameService(PgsqlService pgsqlService) {
        this.pgsqlService = pgsqlService;
    }

    @Override public void start(IocContext iocInjector) throws Exception {
        scheduled();
    }

    @Scheduled("0 0 0")
    public void scheduled() {
        List<GameRecord> gameRecords = pgsqlService.queryEntities(GameRecord.class);
        for (GameRecord gameRecord : gameRecords) {
            addGame(gameRecord);
        }
    }

    public void addGame(GameRecord gameRecord) {
        int gameId = (int) gameRecord.getUid();
        initDataHelper(gameRecord);
        gameId2GameRecordMap.put(gameId, gameRecord);
        gameId2HexMap.putIfAbsent(gameId, new HexId(gameRecord.getUid()));
    }

    public long newId(int gameId) {
        HexId hexId = gameId2HexMap.computeIfAbsent(gameId, HexId::new);
        return hexId.newId();
    }

    public PgsqlDataHelper pgsqlDataHelper(int gameId) {
        return gameId2PgsqlDataHelperMap.get(gameId);
    }

    public void initDataHelper(GameRecord gameRecord) {
        gameId2PgsqlDataHelperMap.computeIfAbsent((int) gameRecord.getUid(), k -> {
            String dbName = "game_db_" + k;
            DbConfig clone = pgsqlService.getDbConfig().clone(dbName);
            clone.setName(dbName);
            clone.setScanPackage(LogScan.class.getPackageName());
            PgsqlDataHelper pgsqlDataHelper = new PgsqlDataHelper(clone);
            pgsqlDataHelper.getBatchPool().setMaxCacheSize(300 * 10000);
            for (Map.Entry<String, String> entry : gameRecord.getTableMapping().entrySet()) {
                checkSLogTable(pgsqlDataHelper, entry.getKey());

                /*TODO 处理分区表 */
                LocalDateTime localDate = LocalDateTime.now();
                for (int i = 0; i < 5; i++) {
                    /*创建表分区*/
                    String form = MyClock.formatDate("yyyyMMdd", localDate);
                    localDate = localDate.plusDays(1);
                    String to = MyClock.formatDate("yyyyMMdd", localDate);
                    String partition_table_name = entry.getKey() + "_" + form;
                    if (pgsqlDataHelper.getDbTableMap().containsKey(partition_table_name))
                        continue;
                    String string = """
                            CREATE TABLE %s PARTITION OF %s
                                FOR VALUES FROM (%s) TO (%s);
                            """.formatted(partition_table_name, entry.getKey(), form, to);
                    pgsqlDataHelper.executeUpdate(string);
                    log.info("表 {} 创建分区 {}", entry.getKey(), partition_table_name);
                }

            }
            return pgsqlDataHelper;
        });
    }

    public void checkSLogTable(PgsqlDataHelper pgsqlDataHelper, String logTableName) {
        logTableName = logTableName.toLowerCase();
        if (pgsqlDataHelper.getDbTableMap().containsKey(logTableName)) return;

        pgsqlDataHelper.createTable(SLog.class, logTableName);

        pgsqlDataHelper.getDbTableMap().clear();
        pgsqlDataHelper.getDbTableStructMap().clear();
    }

    public RunResult checkAppToken(Integer gameId, String token) {
        if (token == null)
            return RunResult.error("token is null");

        GameRecord gameRecord = gameId2GameRecordMap.get(gameId);
        if (gameRecord == null) {
            return RunResult.error("gameId is not exist");
        }

        if (!gameRecord.getAppToken().equals(token))
            return RunResult.error("token is not match");

        return null;
    }

}
