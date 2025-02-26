package wxdgaming.backends.entity.games.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

/**
 * 充值记录
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 17:48
 **/
@Getter
@Setter
@DbTable(tableName = "record_recharge")
public class RechargeRecord extends RecordBase {

    @JSONField(ordinal = 20)
    @DbColumn(index = true)
    private int sid;
    @JSONField(ordinal = 21)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String account;
    @JSONField(ordinal = 22)
    @DbColumn(index = true)
    private long roleId;
    @JSONField(ordinal = 23)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String roleName;
    @JSONField(ordinal = 24)
    @DbColumn(index = true)
    private int lv;
    /** 充值金额单位分 */
    @JSONField(ordinal = 25)
    @DbColumn(index = true)
    private int amount;
    /** 充值渠道 */
    @JSONField(ordinal = 26)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String channel;
    /** 渠道订单号 */
    @JSONField(ordinal = 27)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String spOrder;
    /** 研发订单号 */
    @JSONField(ordinal = 28)
    @DbColumn(index = true, columnType = ColumnType.String, length = 64)
    private String cpOrder;

    @JSONField(ordinal = 99)
    @DbColumn(columnType = ColumnType.Json)
    private final JSONObject other = MapOf.newJSONObject();

}
