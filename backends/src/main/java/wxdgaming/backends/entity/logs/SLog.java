package wxdgaming.backends.entity.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot.batis.enums.ColumnType;
import wxdgaming.boot.batis.struct.DbColumn;
import wxdgaming.boot.batis.struct.TableName;
import wxdgaming.boot.core.lang.ObjectBase;

/**
 * 服务日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 17:32
 **/
@Getter
@Setter
public class SLog extends ObjectBase implements TableName {

    @JSONField(ordinal = 1)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 128)
    private String logType;
    @JSONField(ordinal = 2)
    @DbColumn(key = true)
    private long uid;
    @JSONField(ordinal = 3)
    @DbColumn(index = true)
    private long createTime;
    @JSONField(ordinal = 4)
    @DbColumn(index = true)
    private int year;
    @JSONField(ordinal = 5)
    @DbColumn(index = true)
    private int month;
    @JSONField(ordinal = 6)
    @DbColumn(index = true)
    private int day;
    @JSONField(ordinal = 7)
    @DbColumn(index = true)
    private long gameId;
    @JSONField(ordinal = 8)
    @DbColumn(index = true)
    private int sId;
    @JSONField(ordinal = 9)
    @DbColumn(index = true)
    private int mainId;
    @JSONField(ordinal = 10)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 128)
    private String account;
    @JSONField(ordinal = 11)
    @DbColumn(index = true)
    private long roleId;
    @JSONField(ordinal = 12)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 128)
    private String roleName;
    @JSONField(ordinal = 13)
    @DbColumn(columnType = ColumnType.Json)
    private JSONObject data;

    @JSONField(serialize = false)
    @Override public String getTableName() {
        return logType;
    }
}
