package wxdgaming.backends.entity.games;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.EntityLongUID;
import wxdgaming.boot2.starter.batis.ann.DbTable;

/**
 * 异常记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 13:24
 **/
@Getter
@Setter
@DbTable(tableName = "record_error")
public class ErrorRecord extends EntityLongUID {

}
