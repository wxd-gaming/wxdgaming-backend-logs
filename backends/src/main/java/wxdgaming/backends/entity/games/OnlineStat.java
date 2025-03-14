package wxdgaming.backends.entity.games;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.EntityIntegerUID;
import wxdgaming.boot2.starter.batis.ann.DbTable;

import java.util.HashMap;

/**
 * 在线人数统计
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-13 20:47
 **/
@Getter
@Setter
@DbTable
public class OnlineStat extends EntityIntegerUID {

    /** key: hour, value: 在线数 */
    private final HashMap<Integer, Integer> onlineMap = new HashMap<>();

    public void update(int hour, int online) {
        Integer oldOnline = onlineMap.get(hour);
        if (oldOnline == null || oldOnline < online) {
            /*只保留每小时最大值*/
            onlineMap.put(hour, online);
        }
    }

}
