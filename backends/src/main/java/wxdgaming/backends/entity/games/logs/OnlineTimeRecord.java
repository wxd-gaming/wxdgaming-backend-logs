package wxdgaming.backends.entity.games.logs;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

/**
 * 在线时长记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-24 20:18
 **/
@Getter
@Setter
@DbTable(tableName = "record_role_online_time")
public class OnlineTimeRecord extends RecordBase {

    @JSONField(ordinal = 20)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;
    @JSONField(ordinal = 21)
    @DbColumn(index = true)
    private long roleId;
    @JSONField(ordinal = 22)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String roleName;
    @JSONField(ordinal = 23)
    @DbColumn(index = true)
    private int lv;
    @JSONField(ordinal = 24)
    @DbColumn(index = true)
    private int sid;
    @JSONField(ordinal = 25)
    @DbColumn(index = true)
    private long joinTime;
    /** 最后退出时间 */
    @JSONField(ordinal = 26)
    @DbColumn(index = true)
    private long exitTime;
    /** 最后一次在线时长 */
    @JSONField(ordinal = 27)
    @DbColumn(index = true)
    private long onlineTime;
    /** 累计在线时长 */
    @JSONField(ordinal = 28)
    @DbColumn(index = true)
    private long totalOnlineTime;

}
