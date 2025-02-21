package wxdgaming.backends.entity.games.logs;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.EntityLongUID;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

/**
 * 区服记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:16
 **/
@Getter
@Setter
@DbTable(tableName = "record_server")
public class ServerRecord extends EntityLongUID {

    /** 不写入数据库的用于验证的 */
    @JSONField(ordinal = 10)
    @DbColumn(ignore = true)
    private String token;
    /** 不写入数据库的用于验证的 */
    @JSONField(ordinal = 11)
    @DbColumn(ignore = true)
    private int gameId;
    /** 最后更新时间 */
    @DbColumn()
    @JSONField(ordinal = 12)
    private long updateTime;
    /** 是否启用 */
    @DbColumn(index = true)
    @JSONField(ordinal = 13)
    private boolean enabled;
    /** 顺序 */
    @DbColumn(index = true)
    @JSONField(ordinal = 14)
    private int ordinal;
    /** 平台 */
    @JSONField(ordinal = 15)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String platform;
    /** 服务器id */
    @JSONField(ordinal = 16)
    @DbColumn(index = true)
    private int sid;
    /** 服务器id */
    @JSONField(ordinal = 17)
    @DbColumn(index = true)
    private int mainSid;
    /** 服务器名字 */
    @JSONField(ordinal = 18)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String name;
    /** 服务器显示名 */
    @JSONField(ordinal = 19)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String showName;
    /** 开服时间 */
    @JSONField(ordinal = 20)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String openTime;
    /** 维护时间 */
    @JSONField(ordinal = 21)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String maintainTime;
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
    /** 版本 */
    @JSONField(ordinal = 27)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String version;
    /** 注册账户 */
    @JSONField(ordinal = 28)
    @DbColumn()
    private int registerUserCount;
    /** 注册角色 */
    @JSONField(ordinal = 29)
    @DbColumn()
    private int registerRoleCount;
    /** 在线角色 */
    @JSONField(ordinal = 30)
    @DbColumn()
    private int onlineRoleCount;
    /** 活跃用户 */
    @JSONField(ordinal = 31)
    @DbColumn()
    private int activeRoleCount;
    /** 充值金额 */
    @JSONField(ordinal = 32)
    @DbColumn()
    private long rechargeCount;
}
