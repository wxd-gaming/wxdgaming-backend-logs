package wxdgaming.backends.entity.system;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.EntityIntegerUID;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

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
public class User extends EntityIntegerUID implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @JSONField(ordinal = 2)
    @DbColumn(index = true)
    private long createdTime;
    @JSONField(ordinal = 10)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;

    @JSONField(ordinal = 11)
    @DbColumn(columnType = ColumnType.String, length = 512)
    private String pwd;

    @JSONField(ordinal = 12)
    @DbColumn(index = true, columnType = ColumnType.String, length = 18)
    private String phone;
    /** 归属账号 */
    @JSONField(ordinal = 14)
    private long parentUid;
    /** 账号已被禁用 */
    @JSONField(ordinal = 15)
    private boolean disConnect;
    /** 是否是管理员 */
    @JSONField(ordinal = 16)
    private boolean root;
    /** 是否是管理员 */
    @JSONField(ordinal = 16)
    private boolean admin;
    /** 授权能查看的游戏 */
    @JSONField(ordinal = 17)
    private HashSet<Integer> authorizationGames = new HashSet<>();
    /** 授权路由 */
    @JSONField(ordinal = 18)
    private HashSet<String> authorizationRouting = new HashSet<>();

    /** 检查路由授权 */
    public boolean checkAuthorization(String path) {
        return admin || authorizationRouting.contains(path);
    }

}
