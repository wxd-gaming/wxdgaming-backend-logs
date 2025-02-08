package wxdgaming.backends.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot.batis.struct.DbColumn;
import wxdgaming.boot.batis.struct.DbTable;
import wxdgaming.boot.core.lang.ObjectBase;

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
public class EntityBase extends ObjectBase implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @JSONField(ordinal = 1)
    @DbColumn(key = true)
    private long uid;
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
        return (int) uid;
    }
}
