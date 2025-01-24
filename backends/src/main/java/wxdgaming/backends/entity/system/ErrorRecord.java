package wxdgaming.backends.entity.system;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.backends.entity.EntityBase;
import wxdgaming.boot.batis.struct.DbTable;

/**
 * 异常记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 13:24
 **/
@Getter
@Setter
@DbTable(name = "record_error")
public class ErrorRecord extends EntityBase {

}
