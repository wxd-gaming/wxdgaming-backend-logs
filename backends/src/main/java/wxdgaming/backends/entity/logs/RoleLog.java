package wxdgaming.backends.entity.logs;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot.batis.enums.ColumnType;
import wxdgaming.boot.batis.struct.DbBase;
import wxdgaming.boot.batis.struct.DbColumn;
import wxdgaming.boot.batis.struct.DbTable;

/**
 * 角色记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 17:48
 **/
@Getter
@Setter
@DbTable
public class RoleLog extends DbBase {

    @DbColumn(index = true)
    private long updateTime;
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 128)
    private String account;
    @DbColumn(index = true)
    private int sid;
    @DbColumn(index = true)
    private int mainSid;
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 64)
    private String roleId;
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 64)
    private String roleName;
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 12)
    private String job;
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 12)
    private String sex;
    @DbColumn(index = true)
    private int lv;

}
