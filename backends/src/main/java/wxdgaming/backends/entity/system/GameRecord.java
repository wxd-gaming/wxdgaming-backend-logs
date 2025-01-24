package wxdgaming.backends.entity.system;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wxdgaming.boot.batis.enums.ColumnType;
import wxdgaming.boot.batis.struct.DbBase;
import wxdgaming.boot.batis.struct.DbColumn;
import wxdgaming.boot.batis.struct.DbTable;
import wxdgaming.boot.core.lang.ObjectBase;

import java.util.LinkedHashMap;

/**
 * 游戏
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 17:20
 **/
@Getter
@Setter
@Accessors(chain = true)
@DbTable(name = "record_game")
public class GameRecord extends DbBase {

    @JSONField(ordinal = 11)
    @DbColumn(index = true)
    private String name;
    @JSONField(ordinal = 12)
    private String icon;
    @JSONField(ordinal = 13)
    private String desc;
    @JSONField(ordinal = 14)
    private String url;
    @JSONField(ordinal = 15)
    @DbColumn(index = true, columnType = ColumnType.Varchar, length = 128)
    private String token;
    @JSONField(ordinal = 16)
    @DbColumn(index = true, columnType = ColumnType.Text)
    private LinkedHashMap<String, String> tableMapping = new LinkedHashMap<>();

}
