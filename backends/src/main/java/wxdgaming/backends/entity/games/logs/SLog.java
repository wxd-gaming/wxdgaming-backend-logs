package wxdgaming.backends.entity.games.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.EntityName;
import wxdgaming.boot2.starter.batis.ann.DbColumn;

/**
 * 服务日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 17:32
 **/
@Getter
@Setter
public class SLog extends RecordBase implements EntityName {

    @JSONField(ordinal = -1)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String logType;
    @JSONField(ordinal = 20)
    @DbColumn(index = true)
    private int sid;
    @JSONField(ordinal = 22)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;
    @JSONField(ordinal = 23)
    @DbColumn(index = true)
    private long roleId;
    @JSONField(ordinal = 24)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String roleName;
    @JSONField(ordinal = 25)
    @DbColumn(index = true)
    private int lv;
    @JSONField(ordinal = 99)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject other = MapOf.newJSONObject();


    @JSONField(serialize = false)
    @Override public String tableName() {
        return logType;
    }
}
