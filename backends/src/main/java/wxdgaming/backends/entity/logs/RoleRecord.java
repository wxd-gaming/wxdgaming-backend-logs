package wxdgaming.backends.entity.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.backends.entity.RecordBase;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

/**
 * 角色记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 17:48
 **/
@Getter
@Setter
@DbTable(tableName = "record_role")
public class RoleRecord extends RecordBase {

    @JSONField(ordinal = 3)
    @DbColumn(index = true)
    private long createTime;

    @JSONField(ordinal = 5)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;

    @JSONField(ordinal = 6)
    @DbColumn(index = true)
    private int createSid;

    @JSONField(ordinal = 7)
    @DbColumn(index = true)
    private int curSid;

    @JSONField(ordinal = 8)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String roleId;

    @JSONField(ordinal = 9)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String roleName;

    @JSONField(ordinal = 10)
    @DbColumn(index = true, columnType = ColumnType.String, length = 12)
    private String job;

    @JSONField(ordinal = 11)
    @DbColumn(index = true, columnType = ColumnType.String, length = 12)
    private String sex;

    @JSONField(ordinal = 12)
    @DbColumn(index = true)
    private int lv;

    @JSONField(ordinal = 13)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject data = MapOf.newJSONObject();
}
