package wxdgaming.backends.entity.games;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.Entity;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

import java.util.ArrayList;

/**
 * cd key
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-04-29 10:57
 */
@Getter
@Setter
@Accessors(chain = true)
@DbTable
public class CDKeyRecord extends Entity {

    /** 生成的key对应的id，用于获取数据奖励的 */
    @DbColumn(key = true)
    private int keyId;
    /** cdkey */
    @DbColumn(key = true, length = 32)
    private String cdkey;
    /** 使用次数 */
    private int useCount;
    /** 使用时间 */
    private long lastUseTime;
    @DbColumn(columnType = ColumnType.Json)
    private ArrayList<String> accountList = new ArrayList<>();
    @DbColumn(columnType = ColumnType.Json)
    private ArrayList<Long> ridList = new ArrayList<>();

}
