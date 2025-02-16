package wxdgaming.backends.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.EntityLongUID;
import wxdgaming.boot2.starter.batis.ann.DbColumn;

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
public class RecordBase extends EntityLongUID implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @JSONField(ordinal = 2)
    @DbColumn(index = true)
    private long logTime;
    /** 不写入数据库的用于验证的 */
    @JSONField(ordinal = -1)
    @DbColumn(ignore = true)
    private String token;
    /** 不写入数据库的用于验证的 */
    @JSONField(ordinal = 4)
    @DbColumn(ignore = true)
    private int gameId;

    public int intUid() {
        return getUid().intValue();
    }
}
