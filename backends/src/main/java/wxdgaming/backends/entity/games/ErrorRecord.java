package wxdgaming.backends.entity.games;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.EntityLongUID;
import wxdgaming.boot2.starter.batis.ann.DbColumn;

/**
 * 异常记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 13:24
 **/
@Getter
@Setter
public class ErrorRecord extends EntityLongUID {

    @JSONField(ordinal = 1)
    private int gameId;
    @JSONField(ordinal = 2)
    private long createTime;
    @JSONField(ordinal = 3)
    @DbColumn(columnType = ColumnType.String, length = 256)
    private String path;
    @JSONField(ordinal = 4)
    @DbColumn(columnType = ColumnType.String, length = 100000)
    private String errorMessage;
    @JSONField(ordinal = 5)
    @DbColumn(columnType = ColumnType.String, length = 100000)
    private String data;


}
