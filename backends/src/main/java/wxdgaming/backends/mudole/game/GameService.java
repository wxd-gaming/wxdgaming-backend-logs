package wxdgaming.backends.mudole.game;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.logs.LogScan;
import wxdgaming.backends.entity.logs.SLog;
import wxdgaming.backends.entity.system.GameRecord;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.SqlConfig;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;

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
public class GameService {

    private final ConcurrentHashMap<Integer, HexId> gameId2HexMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, PgsqlDataHelper> gameId2PgsqlDataHelperMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, GameRecord> gameId2GameRecordMap = new ConcurrentHashMap<>();

    final PgsqlService pgsqlService;

    @Inject
    public GameService(PgsqlService pgsqlService) {
        this.pgsqlService = pgsqlService;
    }

    @Start
    public void start() throws Exception {
        scheduled();
    }

    // @Scheduled("0 0 0")
    public void scheduled() {
        List<GameRecord> gameRecords = pgsqlService.findAll(GameRecord.class);
        for (GameRecord gameRecord : gameRecords) {
            addGameCache(gameRecord);
        }
    }

    public void addGameCache(GameRecord gameRecord) {
        int gameId = gameRecord.getUid().intValue();
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
        PgsqlDataHelper dataHelper = gameId2PgsqlDataHelperMap.computeIfAbsent(gameRecord.getUid().intValue(), k -> {
            String dbName = "game_db_" + k;
            SqlConfig clone = pgsqlService.getSqlConfig().clone(dbName);
            clone.setScanPackage(LogScan.class .getPackageName());
            PgsqlDataHelper pgsqlDataHelper = new PgsqlDataHelper(clone);
            return pgsqlDataHelper;
        });

        for (Map.Entry<String, String> entry : gameRecord.getTableMapping().entrySet()) {
            checkSLogTable(dataHelper, entry.getKey());
            /*TODO 处理分区表 */
            LocalDateTime localDate = LocalDateTime.now();
            for (int i = 0; i < 50; i++) {
                /*创建表分区*/
                String form = MyClock.formatDate("yyyyMMdd", localDate);
                localDate = localDate.plusDays(1);
                String to = MyClock.formatDate("yyyyMMdd", localDate);
                String partition_table_name = entry.getKey() + "_" + form;
                if (dataHelper.getDbTableMap().containsKey(partition_table_name))
                    continue;
                String string = "CREATE TABLE %s PARTITION OF %s FOR VALUES FROM (%s) TO (%s)"
                        .formatted(partition_table_name, entry.getKey(), form, to);
                dataHelper.executeUpdate(string);
                log.info("数据库 {} 表 {} 创建分区 {}", dataHelper.getDbName(), entry.getKey(), partition_table_name);
            }
        }
    }

    public void checkSLogTable(PgsqlDataHelper pgsqlDataHelper, String logTableName) {
        logTableName = logTableName.toLowerCase();
        if (pgsqlDataHelper.getDbTableMap().containsKey(logTableName)) return;

        pgsqlDataHelper.checkTable(SLog.class, logTableName, "");

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
