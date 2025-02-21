package wxdgaming.backends.entity.system;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.Entity;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

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
@DbTable
public class Game extends Entity {

    @DbColumn(key = true)
    @JSONField(ordinal = 1)
    private int uid = 0;
    @JSONField(ordinal = 3)
    @DbColumn(index = true)
    private long createTime;
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
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String appToken;
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String rechargeToken;
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String logToken;
    @JSONField(ordinal = 16)
    @DbColumn(columnType = ColumnType.String, length = 30000)
    private LinkedHashMap<String, String> tableMapping = new LinkedHashMap<>();

}
