package wxdgaming.backends.entity.games.logs;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

/**
 * 登录日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-23 10:50
 **/
@Getter
@Setter
@DbTable(tableName = "record_role_login")
public class SRoleLog2Login extends SRoleLog {

    public enum LogEnum {
        /** 登录 */
        LOGIN,
        /** 登出 */
        LOGOUT
    }

    /** 登录或者登出 */
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private LogEnum logEnum;

    @Override public String tableName() {
        return "record_role_login";
    }

}
