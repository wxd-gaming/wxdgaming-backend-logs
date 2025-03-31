package wxdgaming.backends.entity.games.logs;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

/**
 * 活跃用户日志记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-31 16:28
 **/
@Getter
@Setter
@DbTable(tableName = "record_active_account")
public class ActiveAccountRecord extends RecordBase {

    @JSONField(ordinal = 20)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;

}
