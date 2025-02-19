package wxdgaming.backends.entity.logs;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.backends.entity.RecordBase;
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
@DbTable(tableName = "server_record")
public class ServerRecord extends RecordBase {

    /** 是否启用 */
    @DbColumn(index = true)
    private boolean enabled;
    /** 顺序 */
    @DbColumn(index = true)
    private int ordinal;
    /** 平台 */
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String platform;
    /** 服务器id */
    @DbColumn(index = true)
    private int sid;
    /** 服务器id */
    @DbColumn(index = true)
    private int mainSid;
    /** 服务器名字 */
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String name;
    /** 服务器显示名 */
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String showName;
    /** 开服时间 */
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String openTime;
    /** 维护时间 */
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String maintainTime;
    /** ip */
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String wlan;
    /** ip */
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String lan;
    /** 端口 */
    @DbColumn(index = true)
    private int port;
    /** web 端口 */
    @DbColumn(index = true)
    private int webPort;
    /** 状态 */
    @DbColumn(index = true)
    private String status;
    /** 版本 */
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String version;
    /** 注册账户 */
    @DbColumn()
    private int registerUserCount;
    /** 注册角色 */
    @DbColumn()
    private int registerRoleCount;
    /** 在线角色 */
    @DbColumn()
    private int onlineRoleCount;
    /** 活跃用户 */
    @DbColumn()
    private int activeRoleCount;
    /** 充值金额 */
    @DbColumn()
    private long rechargeCount;
    /** 最后更新时间 */
    @DbColumn()
    private long updateTime;
}
