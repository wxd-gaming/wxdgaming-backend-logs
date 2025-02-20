package wxdgaming.backends.entity.games.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
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

    @JSONField(ordinal = 20)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;

    @JSONField(ordinal = 21)
    @DbColumn(index = true)
    private int createSid;

    @JSONField(ordinal = 22)
    @DbColumn(index = true)
    private int curSid;

    @JSONField(ordinal = 23)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String roleId;

    @JSONField(ordinal = 24)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String roleName;

    @JSONField(ordinal = 25)
    @DbColumn(index = true, columnType = ColumnType.String, length = 12)
    private String job;

    @JSONField(ordinal = 26)
    @DbColumn(index = true, columnType = ColumnType.String, length = 12)
    private String sex;

    @JSONField(ordinal = 27)
    @DbColumn(index = true)
    private int lv;

    @JSONField(ordinal = 28)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject data = MapOf.newJSONObject();

    @JSONField(ordinal = 29)
    @DbColumn(index = true)
    private int del;
}
