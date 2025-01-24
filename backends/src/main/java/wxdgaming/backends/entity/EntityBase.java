package wxdgaming.backends.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot.batis.struct.DbBase;
import wxdgaming.boot.batis.struct.DbColumn;

/**
 * 实体类基类
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 13:19
 **/
@Getter
@Setter
public class EntityBase extends DbBase {

    /** 不写入数据库的用于验证的 */
    @JSONField(ordinal = -1)
    @DbColumn(alligator = true)
    private String token;
    @JSONField(ordinal = 4)
    @DbColumn(alligator = true)
    private int gameId;
}
