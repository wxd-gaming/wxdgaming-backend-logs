package wxdgaming.backends.admin.game;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.games.ErrorRecord;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.backends.entity.games.logs.ServerRecord;
import wxdgaming.backends.entity.system.Game;
import wxdgaming.boot2.core.cache.Cache;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.threading.ThreadContext;
import wxdgaming.boot2.core.util.AssertUtil;
import wxdgaming.boot2.core.util.ObjectLockUtil;
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
                .delay(2, TimeUnit.DAYS)
                .heartTime(2, TimeUnit.DAYS)
                .expireAfterWrite(2, TimeUnit.DAYS)
                .build();
        this.accountRecordJdbcCache = new JdbcCache<>(dataHelper, 10, 60) {

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

        this.roleRecordJdbcCache = new JdbcCache<>(dataHelper, 10, 60) {
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
            serverRecordMap.put(serverRecord.getUid(), serverRecord);
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

    public ServerRecord serverGetOrCreate(int sid) {
        return serverRecordMap.computeIfAbsent(sid, k -> {
            ServerRecord serverRecord = new ServerRecord();
            serverRecord.setUid(sid);
            dataHelper.insert(serverRecord);
            return serverRecord;
        });
    }

    public AccountRecord accountGetOrCreate(String account) {
        return accountGetOrCreate(account, System.currentTimeMillis());
    }

    public AccountRecord accountGetOrCreate(String account, long createTime) {
        AssertUtil.assertNullEmpty(account, "account is blank");
        ObjectLockUtil.lock(account);
        try {
            AccountRecord ifPresent = accountRecordJdbcCache.getIfPresent(account);
            if (ifPresent == null) {
                ifPresent = new AccountRecord();
                ifPresent.setUid(accountHexId.newId());
                ifPresent.setAccount(account);
                ifPresent.setCreateTime(createTime);
                ifPresent.checkDataKey();
                accountRecordJdbcCache.put(account, ifPresent);
            }
            return ifPresent;
        } finally {
            ObjectLockUtil.unlock(account);
        }
    }

    public RoleRecord roleGetOrCreate(String account, long roleId) {
        return roleGetOrCreate(account, roleId, System.currentTimeMillis());
    }

    public RoleRecord roleGetOrCreate(String account, long roleId, long createTime) {
        AssertUtil.assertTrue(roleId > 0, "account is blank");
        AssertUtil.assertNullEmpty(account, "account is blank");
        ObjectLockUtil.lock(account);
        try {
            RoleRecord ifPresent = roleRecordJdbcCache.getIfPresent(roleId);
            if (ifPresent == null) {
                ifPresent = new RoleRecord();
                ifPresent.setUid(roleId);
                ifPresent.setAccount(account);
                ifPresent.setCreateTime(createTime);

                ifPresent.checkDataKey();
                roleRecordJdbcCache.put(roleId, ifPresent);
                AccountRecord accountRecord = accountGetOrCreate(account);
                if (!accountRecord.getRoleList().contains(roleId)) {
                    accountRecord.getRoleList().add(roleId);
                }
            }
            return ifPresent;
        } finally {
            ObjectLockUtil.unlock(account);
        }
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

    public void submit(Runnable runnable) {
        ExecutorUtil.getInstance().getVirtualExecutor().submit("queue-game-" + gameId, runnable);
    }

    public long registerAccountNum(int dayKey) {
        Long registerAccountNum = dataHelper.executeScalar("SELECT \"count\"(DISTINCT ll.account) FROM record_account as ll WHERE ll.daykey = ?;", Long.class, dayKey);
        return registerAccountNum == null ? 0L : registerAccountNum;
    }

    public long loginAccountNum(int dayKey) {
        Long loginAccountNum = dataHelper.executeScalar("SELECT \"count\"(DISTINCT account) FROM record_role_login WHERE daykey=?", Long.class, dayKey);
        return loginAccountNum == null ? 0L : loginAccountNum;
    }

    public long rechargeAmountNum(int dayKey) {
        Long rechargeAmountNum = dataHelper.executeScalar(
                """
                        SELECT
                            "sum" ( amount )\s
                        FROM
                            record_recharge rr
                            RIGHT JOIN ( SELECT "min" ( uid ) AS "uid" FROM record_recharge WHERE daykey = ? GROUP BY sporder ) rrt ON rr.uid = rrt.uid
                        """,
                Long.class,
                dayKey
        );
        return rechargeAmountNum == null ? 0L : rechargeAmountNum;
    }

    /** 充值账号数 */
    public long rechargeAccountNum(int dayKey) {
        Long rechargeAccountNum = dataHelper.executeScalar("SELECT \"count\"(DISTINCT account) FROM record_recharge WHERE daykey=?", Long.class, dayKey);
        return rechargeAccountNum == null ? 0L : rechargeAccountNum;
    }

    public long registerAccountRechargeNum(int dayKey) {
        /*今天注册就充值的账号数量*/
        Long registerAccountRechargeNum = dataHelper.executeScalar(
                """
                        SELECT
                            "count" ( DISTINCT rr.account )\s
                        FROM
                            record_recharge AS rr
                            RIGHT JOIN (SELECT account FROM record_account as ll WHERE ll.daykey = ? GROUP BY account) as ra ON ra.account = rr.account
                        """,
                Long.class,
                dayKey
        );
        return registerAccountRechargeNum == null ? 0L : registerAccountRechargeNum;
    }

    public long rechargeOrderNum(int dayKey) {
        Long rechargeOrderNum = dataHelper.executeScalar("SELECT \"count\"(DISTINCT sporder) FROM record_recharge WHERE daykey=?", Long.class, dayKey);
        return rechargeOrderNum == null ? 0L : rechargeOrderNum;
    }

    public long onlineAccount() {
        return getAccountRecordJdbcCache().values().stream()
                .filter(AccountRecord::online)
                .count();
    }

    public Object[] queryRechargeGroup(int dayKey) {

        String sql = "SELECT rr.amount,\"count\"(rr.amount) FROM record_recharge as rr WHERE daykey=? GROUP BY daykey,rr.amount ORDER BY rr.daykey DESC,rr.amount";

        List<JSONObject> jsonObjects = dataHelper.queryList(sql, dayKey);

        jsonObjects.sort((o1, o2) -> Long.compare(o2.getLongValue("count"), o1.getLongValue("count")));

        Object[] objectsTitle = new Object[jsonObjects.size()];
        Object[] objectsValue = new Object[jsonObjects.size()];

        for (int i = 0; i < jsonObjects.size(); i++) {
            JSONObject jsonObject = jsonObjects.get(i);
            objectsTitle[i] = jsonObject.getIntValue("amount") / 100;
            objectsValue[i] = jsonObject.getIntValue("count");
        }

        return new Object[]{
                objectsTitle, objectsValue
        };
    }

}
