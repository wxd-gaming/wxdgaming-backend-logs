package wxdgaming.backends.entity.games.logs;

import wxdgaming.boot2.starter.batis.ann.DbTable;

/**
 * 登录日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-23 10:50
 **/
@DbTable(tableName = "record_role_login")
public class SLog2Login extends SLog {

    @Override public String tableName() {
        return "record_role_login";
    }

}
