package wxdgaming.backends.entity.games;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.lang.TimeValue;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.EntityIntegerUID;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 区服记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:16
 **/
@Getter
@Setter
@DbTable(tableName = "record_server")
public class ServerRecord extends EntityIntegerUID {
    /** 分组标签 */
    @DbColumn(index = true)
    @JSONField(ordinal = 2)
    private String group;
    /** 控制显示顺序 */
    @DbColumn(index = true)
    @JSONField(ordinal = 3)
    private int ordinal;
    /** 特殊标签 ，比如new-新服，recommend-推荐服 */
    @DbColumn(index = true)
    @JSONField(ordinal = 4)
    private String label;
    /** 服务器id */
    @JSONField(ordinal = 10)
    @DbColumn(index = true)
    private int mainSid;
    /** 服务器名字 */
    @JSONField(ordinal = 14)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String name;
    /** 服务器显示名 */
    @JSONField(ordinal = 15)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String showName;
    /** 最后更新时间 */
    @DbColumn()
    @JSONField(ordinal = 16)
    private TimeValue updateTime = new TimeValue();
    /** 是否启用 */
    @DbColumn(index = true)
    @JSONField(ordinal = 17)
    private boolean enabled = true;
    /** 开服时间 */
    @JSONField(ordinal = 19)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private TimeValue openTime = new TimeValue();
    /** 维护时间 */
    @JSONField(ordinal = 21)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private TimeValue maintainTime = new TimeValue();
    /** ip */
    @JSONField(ordinal = 22)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String wlan;
    /** ip */
    @JSONField(ordinal = 23)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String lan;
    /** 端口 */
    @JSONField(ordinal = 24)
    @DbColumn(index = true)
    private int port;
    /** web 端口 */
    @JSONField(ordinal = 25)
    @DbColumn(index = true)
    private int webPort;
    /** 状态 */
    @JSONField(ordinal = 26)
    @DbColumn(index = true)
    private String status;
    /** 注册角色 */
    @JSONField(ordinal = 29)
    @DbColumn()
    private int registerRoleCount;
    /** 充值金额 */
    @JSONField(ordinal = 32)
    @DbColumn()
    private AtomicLong rechargeCount = new AtomicLong();
    /** 充值金额 */
    @JSONField(ordinal = 32)
    @DbColumn()
    private AtomicLong rechargeAmount = new AtomicLong();
    @JSONField(ordinal = 99)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject other = MapOf.newJSONObject();

}
