package wxdgaming.backends.entity.system;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot.batis.EntityLongUID;
import wxdgaming.boot.batis.enums.ColumnType;
import wxdgaming.boot.batis.struct.DbColumn;
import wxdgaming.boot.batis.struct.DbTable;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;

/**
 * 管理账号
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-10 20:21
 **/
@Getter
@Setter
@DbTable()
public class User extends EntityLongUID implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @JSONField(ordinal = 2)
    @DbColumn(index = true)
    private long createdTime;
    @JSONField(ordinal = 10)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 128)
    private String account;

    @JSONField(ordinal = 11)
    @DbColumn(columnType = ColumnType.Varchar, length = 512)
    private String pwd;

    @JSONField(ordinal = 12)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 11)
    private String phone;
    @JSONField(ordinal = 13)
    private boolean disConnect;
    /** 是否是管理员 */
    private boolean admin;
    /** 授权路由 */
    private HashSet<String> authorizationRouting = new HashSet<>();

    /** 检查路由授权 */
    public boolean checkAuthorization(String path) {
        return admin || authorizationRouting.contains(path);
    }

}
