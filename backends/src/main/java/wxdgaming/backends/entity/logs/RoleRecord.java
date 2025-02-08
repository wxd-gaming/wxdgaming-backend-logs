package wxdgaming.backends.entity.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.backends.entity.EntityBase;
import wxdgaming.boot.batis.enums.ColumnType;
import wxdgaming.boot.batis.struct.DbColumn;
import wxdgaming.boot.batis.struct.DbTable;
import wxdgaming.boot.core.collection.MapOf;

/**
 * 角色记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 17:48
 **/
@Getter
@Setter
@DbTable(name = "record_role")
public class RoleRecord extends EntityBase {

    @JSONField(ordinal = 3)
    @DbColumn(index = true)
    private long createTime;

    @JSONField(ordinal = 5)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 128)
    private String account;

    @JSONField(ordinal = 6)
    @DbColumn(index = true)
    private int createSid;

    @JSONField(ordinal = 7)
    @DbColumn(index = true)
    private int curSid;

    @JSONField(ordinal = 8)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 64)
    private String roleId;

    @JSONField(ordinal = 9)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 64)
    private String roleName;

    @JSONField(ordinal = 10)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 12)
    private String job;

    @JSONField(ordinal = 11)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 12)
    private String sex;

    @JSONField(ordinal = 12)
    @DbColumn(index = true)
    private int lv;

    @JSONField(ordinal = 13)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject data = MapOf.newJSONObject();
}
