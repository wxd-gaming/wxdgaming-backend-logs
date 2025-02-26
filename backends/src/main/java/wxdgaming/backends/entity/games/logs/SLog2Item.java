package wxdgaming.backends.entity.games.logs;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

/**
 * 登录日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-23 10:50
 **/
@Getter
@Setter
@DbTable(tableName = "record_role_item")
public class SLog2Item extends SLog {

    public enum ChangeTypeEnum {
        /** 获取 */
        Get,
        /** 消耗 */
        COST
    }

    /** 登录或者登出 */
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private ChangeTypeEnum changeType;
    @DbColumn(index = true)
    private int itemId;
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String itemName;
    @DbColumn(index = true)
    private boolean itemBind;
    @DbColumn(index = true)
    private int itemCount;
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String itemType;
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String itemSubType;
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String source;
    @DbColumn(columnType = ColumnType.String, length = 512)
    private String comment;

    @Override public String tableName() {
        return "record_role_item";
    }

}
