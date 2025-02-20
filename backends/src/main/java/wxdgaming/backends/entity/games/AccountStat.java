package wxdgaming.backends.entity.games;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.Entity;
import wxdgaming.boot2.starter.batis.EntityUID;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

import java.util.HashMap;

/**
 * 账号留存
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-20 10:53
 **/
@Getter
@Setter
@DbTable
public class AccountStat extends Entity implements EntityUID<Integer> {

    @DbColumn(key = true)
    private Integer uid;
    @DbColumn(index = true)
    private long registerNum;
    @DbColumn(columnType = ColumnType.Json)
    private HashMap<String, String> dayStatNumMap = new HashMap<>();

}
