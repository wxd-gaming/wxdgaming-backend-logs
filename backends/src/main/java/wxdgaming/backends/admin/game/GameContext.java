package wxdgaming.backends.admin.game;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.games.ErrorRecord;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.games.logs.ServerRecord;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.boot2.core.cache.Cache;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.threading.ThreadContext;
import wxdgaming.boot2.starter.batis.sql.JdbcCache;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 游戏上下文
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-22 21:33
 **/
@Slf4j
@Getter
public class GameContext {

    private final int gameId;
    private final Game game;
    private final HexId accountHexId;
    private final HexId rechargeHexId;
    private final HexId errorHexId;
    private final Map<String, HexId> logTypeHexIdMap = new ConcurrentHashMap<>();
    private final PgsqlDataHelper dataHelper;
    /**
     * 含义是用来判定日志是否已经入库
     * <p>key: logType+uid</p>
     * <p>value: true</p>
     */
    private final Cache<String, Boolean> logKeyCache;
    private final JdbcCache<AccountRecord, String> accountRecordJdbcCache;
    private final JdbcCache<RoleRecord, Long> roleRecordJdbcCache;
    private final Map<Integer, ServerRecord> serverRecordMap = new ConcurrentHashMap<>();
    private final ReentrantLock errorReentrantLock = new ReentrantLock();
    private final LinkedList<ErrorRecord> errorRecordList = new LinkedList<>();

    public GameContext(Game game, PgsqlDataHelper dataHelper) {
        this.game = game;
        this.gameId = game.getUid();
        this.accountHexId = new HexId(gameId);
        this.rechargeHexId = new HexId(gameId);
        this.errorHexId = new HexId(gameId);
        this.dataHelper = dataHelper;
        this.logKeyCache = Cache.<String, Boolean>builder()
                .cacheName("logKeyCache")
                .hashArea(100)
                .heartTime(2, TimeUnit.HOURS)
                .expireAfterWrite(2, TimeUnit.DAYS)
                .build();
        this.accountRecordJdbcCache = new JdbcCache<>(dataHelper, 60) {

            @Override protected AccountRecord loader(String account) {
                AccountRecord byWhere = dataHelper.findByWhere(AccountRecord.class, "account = ?", account);
                if (byWhere != null) {
                    byWhere.setNewEntity(false);
                    byWhere.checkHashCode();
                }
                return byWhere;
            }

            @Override protected void heart(String s, AccountRecord accountRecord) {
                super.heart(s, accountRecord);
            }

            @Override protected boolean removed(String s, AccountRecord accountRecord) {
                dataHelper.getDataBatch().save(accountRecord);
                return true;
            }

            @Override public void put(String key, AccountRecord value) {
                dataHelper.getDataBatch().insert(value);
                getCache().put(key, value);
            }

        };

        this.roleRecordJdbcCache = new JdbcCache<>(dataHelper, 60) {
            @Override protected RoleRecord loader(Long uid) {
                RoleRecord byWhere = dataHelper.findByWhere(RoleRecord.class, "uid = ?", uid);
                if (byWhere != null) {
                    byWhere.setNewEntity(false);
                    byWhere.checkHashCode();
                }
                return byWhere;
            }

            @Override public void put(Long key, RoleRecord value) {
                dataHelper.getDataBatch().insert(value);
                getCache().put(key, value);
            }

            @Override protected boolean removed(Long aLong, RoleRecord roleRecord) {
                dataHelper.getDataBatch().save(roleRecord);
                return true;
            }
        };

        List<ServerRecord> list = dataHelper.findList(ServerRecord.class);
        for (ServerRecord serverRecord : list) {
            serverRecordMap.put(serverRecord.getSid(), serverRecord);
        }

    }

    public void shutdown() {
        this.logKeyCache.shutdown();
        this.accountRecordJdbcCache.shutdown();
        this.roleRecordJdbcCache.shutdown();
        this.dataHelper.shutdown();
    }

    public long newId(String logType) {
        return logTypeHexIdMap.computeIfAbsent(logType, k -> new HexId(gameId)).newId();
    }

    public AccountRecord getAccountRecord(String account) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        return accountRecordJdbcCache.getIfPresent(account);
    }

    public RoleRecord getRoleRecord(long roleId) {
        if (roleId == 0) {
            return null;
        }
        return roleRecordJdbcCache.getIfPresent(roleId);
    }

    public void recordError(String errorMessage, String data) {
        ErrorRecord errorRecord = new ErrorRecord();
        errorRecord.setUid(getErrorHexId().newId());
        errorRecord.setCreateTime(System.currentTimeMillis());
        errorRecord.setGameId(getGameId());
        errorRecord.setPath(ThreadContext.context().getString("http-path"));
        errorRecord.setData(data);
        errorRecord.setErrorMessage(errorMessage);
        errorReentrantLock.lock();
        try {
            errorRecordList.addFirst(errorRecord);
            if (errorRecordList.size() > 200)
                errorRecordList.removeLast();
        } finally {
            errorReentrantLock.unlock();
        }
        log.error("记录错误信息 {}", errorRecord);
    }

}
