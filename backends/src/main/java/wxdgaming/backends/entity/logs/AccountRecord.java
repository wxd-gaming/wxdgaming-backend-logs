package wxdgaming.backends.entity.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.backends.entity.RecordBase;
import wxdgaming.boot.batis.enums.ColumnType;
import wxdgaming.boot.batis.struct.DbColumn;
import wxdgaming.boot.batis.struct.DbTable;
import wxdgaming.boot.core.collection.MapOf;

/**
 * 账户记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 11:35
 */
@Getter
@Setter
@DbTable(name = "record_account")
public class AccountRecord extends RecordBase {

    @JSONField(ordinal = 10)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 128)
    private String account;
    @JSONField(ordinal = 11)
    @DbColumn(index = true)
    private long createTime;
    @JSONField(ordinal = 12)
    @DbColumn(index = true)
    private int lastJoinSid;
    @JSONField(ordinal = 13)
    @DbColumn(index = true)
    private long lastJoinTime;
    @JSONField(ordinal = 14)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject data = MapOf.newJSONObject();

}
