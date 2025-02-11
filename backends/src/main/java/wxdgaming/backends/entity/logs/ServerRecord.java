package wxdgaming.backends.entity.logs;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot.batis.EntityBase;
import wxdgaming.boot.batis.enums.ColumnType;
import wxdgaming.boot.batis.struct.DbColumn;
import wxdgaming.boot.batis.struct.DbTable;
import wxdgaming.boot.core.lang.ObjectBase;

/**
 * 区服记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:16
 **/
@Getter
@Setter
@DbTable
public class ServerRecord extends EntityBase<Long> {

    /** 是否启用 */
    @DbColumn(index = true)
    private boolean enabled;
    /** 顺序 */
    @DbColumn(index = true)
    private int ordinal;
    /** 平台 */
    @DbColumn(columnType = ColumnType.Varchar, length = 128)
    private String platform;
    /** 服务器id */
    @DbColumn(columnType = ColumnType.Varchar, length = 128)
    private int sid;
    /** 服务器id */
    @DbColumn(columnType = ColumnType.Varchar, length = 128)
    private int mainSid;
    /** 服务器名字 */
    @DbColumn(columnType = ColumnType.Varchar, length = 128)
    private String name;
    /** 服务器显示名 */
    @DbColumn(columnType = ColumnType.Varchar, length = 128)
    private String showName;
    /** 开服时间 */
    @DbColumn(columnType = ColumnType.Varchar, length = 128)
    private String openTime;
    /** 维护时间 */
    @DbColumn(columnType = ColumnType.Varchar, length = 128)
    private String maintainTime;
    /** ip */
    @DbColumn(columnType = ColumnType.Varchar, length = 128)
    private String wlan;
    /** ip */
    @DbColumn(columnType = ColumnType.Varchar, length = 128)
    private String lan;
    /** 端口 */
    @DbColumn()
    private int port;
    /** web 端口 */
    @DbColumn()
    private int webPort;
    /** 状态 */
    @DbColumn()
    private String status;
    /** 版本 */
    @DbColumn(columnType = ColumnType.Varchar, length = 128)
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
