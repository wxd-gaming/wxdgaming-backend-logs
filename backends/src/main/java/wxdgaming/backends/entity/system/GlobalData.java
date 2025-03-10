package wxdgaming.backends.entity.system;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.Entity;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全局配置
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-10 20:31
 **/
@Getter
@Setter
@DbTable()
public class GlobalData extends Entity {

    @DbColumn(key = true)
    private int uid = 1;

    private AtomicInteger newGameId = new AtomicInteger();

}
