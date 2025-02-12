package wxdgaming.backends.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot.batis.EntityLong;
import wxdgaming.boot.batis.struct.DbColumn;
import wxdgaming.boot.batis.struct.DbTable;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实体类基类
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 13:19
 **/
@Getter
@Setter
@DbTable(mappedSuperclass = true)
public class RecordBase extends EntityLong implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @JSONField(ordinal = 2)
    @DbColumn(index = true)
    private long logTime;
    /** 不写入数据库的用于验证的 */
    @JSONField(ordinal = -1)
    @DbColumn(alligator = true)
    private String token;
    /** 不写入数据库的用于验证的 */
    @JSONField(ordinal = 4)
    @DbColumn(alligator = true)
    private int gameId;

    public int intUid() {
        return getUid().intValue();
    }
}
