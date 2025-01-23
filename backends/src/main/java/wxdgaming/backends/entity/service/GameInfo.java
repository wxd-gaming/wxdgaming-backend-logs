package wxdgaming.backends.entity.service;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wxdgaming.boot.batis.struct.DbColumn;
import wxdgaming.boot.batis.struct.DbTable;
import wxdgaming.boot.core.lang.ObjectBase;

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
public class GameInfo extends ObjectBase {

    @DbColumn(key = true)
    private int uid;
    private String name;
    private String icon;
    private String desc;
    private String url;

}
