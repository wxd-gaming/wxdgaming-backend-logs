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
 * 账户记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 11:35
 */
@Getter
@Setter
@DbTable(tableName = "record_account")
public class AccountRecord extends RecordBase {

    @JSONField(ordinal = 20)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;
    @JSONField(ordinal = 21)
    @DbColumn(index = true)
    private int lastJoinSid;
    @JSONField(ordinal = 22)
    @DbColumn(index = true)
    private long lastJoinTime;
    @JSONField(ordinal = 23)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject data = MapOf.newJSONObject();

}
