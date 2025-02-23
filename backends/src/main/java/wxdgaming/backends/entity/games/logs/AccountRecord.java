package wxdgaming.backends.entity.games.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 账户记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 11:35
 */
@Getter
@Setter
@DbTable(tableName = "record_account")
public class AccountRecord extends RecordBase {

    @JSONField(ordinal = 20)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;
    /** 玩家的角色列表 */
    @JSONField(ordinal = 21)
    @DbColumn(columnType = ColumnType.String, length = 15000)
    private final List<Long> roleList = new ArrayList<>();
    /** 充值金额 */
    @JSONField(ordinal = 22)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private AtomicLong rechargeAmount = new AtomicLong();
    /** 充值此时 */
    @JSONField(ordinal = 23)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private AtomicInteger rechargeCount = new AtomicInteger();
    /** 首次充值时间 */
    @JSONField(ordinal = 24)
    @DbColumn(index = true)
    private long rechargeFirstTime = 0;
    /** 最后充值时间 */
    @JSONField(ordinal = 25)
    @DbColumn(index = true)
    private long rechargeLastTime = 0;

    @JSONField(ordinal = 40)
    @DbColumn(index = true)
    private int lastJoinSid;
    /** 最后登录时间 */
    @JSONField(ordinal = 41)
    @DbColumn(index = true)
    private long lastJoinTime;
    /** 最后退出时间 */
    @JSONField(ordinal = 42)
    @DbColumn(index = true)
    private long lastExitTime;
    @JSONField(ordinal = 43)
    @DbColumn(index = true)
    private boolean online;

    @JSONField(ordinal = 99)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject data = MapOf.newJSONObject();

}
