package wxdgaming.backends.entity.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.backends.entity.RecordBase;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.EntityName;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.sql.ann.Partition;

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
    @Partition
    @JSONField(ordinal = 4)
    @DbColumn(key = true)
    private int dayKey;
    @JSONField(ordinal = 8)
    @DbColumn(index = true)
    private int sId;
    @JSONField(ordinal = 9)
    @DbColumn(index = true)
    private int mainId;
    @JSONField(ordinal = 10)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;
    @JSONField(ordinal = 11)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String roleId;
    @JSONField(ordinal = 12)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String roleName;
    @JSONField(ordinal = 13)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject data = MapOf.newJSONObject();


    @JSONField(serialize = false)
    @Override public String tableName() {
        long logTime = getLogTime();
        if (logTime == 0) {
            setLogTime(System.currentTimeMillis());
        }
        // String yyyy_mm_dd = MyClock.formatDate("yyyy_MM_dd", getLogTime());
        // return logType + "_" + yyyy_mm_dd;
        return logType;
    }
}
