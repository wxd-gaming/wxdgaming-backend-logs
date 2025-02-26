package wxdgaming.backends.entity.games.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 角色记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 17:48
 **/
@Getter
@Setter
@DbTable(tableName = "record_role")
public class RoleRecord extends RecordBase {

    @JSONField(ordinal = 10)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String roleName;

    @JSONField(ordinal = 20)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;

    @JSONField(ordinal = 21)
    @DbColumn(index = true)
    private int createSid;

    @JSONField(ordinal = 22)
    @DbColumn(index = true)
    private int curSid;

    @JSONField(ordinal = 25)
    @DbColumn(index = true, columnType = ColumnType.String, length = 12)
    private String job;

    @JSONField(ordinal = 26)
    @DbColumn(index = true, columnType = ColumnType.String, length = 12)
    private String sex;

    @JSONField(ordinal = 27)
    @DbColumn(index = true)
    private int lv;

    @JSONField(ordinal = 29)
    @DbColumn(index = true)
    private int del;


    /** 充值金额 */
    @JSONField(ordinal = 30)
    private AtomicLong rechargeAmount = new AtomicLong();
    /** 充值此时 */
    @JSONField(ordinal = 31)
    @DbColumn(index = true)
    private AtomicInteger rechargeCount = new AtomicInteger();
    /** 首次充值时间 */
    @JSONField(ordinal = 32)
    @DbColumn(index = true)
    private long rechargeFirstTime = 0;
    /** 最后充值时间 */
    @JSONField(ordinal = 33)
    @DbColumn(index = true)
    private long rechargeLastTime = 0;


    @JSONField(ordinal = 40)
    @DbColumn(index = true)
    private int lastJoinSid;
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
    /** 累计在线时长 */
    @JSONField(ordinal = 44)
    @DbColumn(index = true)
    private long totalOnlineTime;
    /** 最后一次在线时长 */
    @JSONField(ordinal = 44)
    @DbColumn(index = true)
    private long lastOnlineTime;

    @JSONField(ordinal = 99)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject other = MapOf.newJSONObject();
}
