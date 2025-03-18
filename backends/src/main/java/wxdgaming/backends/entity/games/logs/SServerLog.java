package wxdgaming.backends.entity.games.logs;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.ColumnType;
import wxdgaming.boot2.starter.batis.EntityLongUID;
import wxdgaming.boot2.starter.batis.EntityName;
import wxdgaming.boot2.starter.batis.ann.DbColumn;

import java.time.LocalDate;

/**
 * 服务日志
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 17:32
 **/
@Getter
@Setter
public class SServerLog extends EntityLongUID implements EntityName {

    @JSONField(ordinal = -1)
    @DbColumn(index = true, columnType = ColumnType.String, length = 128)
    private String logType;
    @JSONField(ordinal = 13)
    @DbColumn(key = true)
    private int dayKey;
    @JSONField(ordinal = 12)
    @DbColumn(index = true)
    private long createTime;
    @JSONField(ordinal = 20)
    @DbColumn(index = true)
    private int sid;
    @JSONField(ordinal = 99)
    @DbColumn(columnType = ColumnType.Json)
    private JSONObject other = MapOf.newJSONObject();


    @JSONField(serialize = false)
    @Override public String tableName() {
        return logType;
    }

    public void checkDataKey() {
        if (getCreateTime() == 0) {
            setCreateTime(System.currentTimeMillis());
        }

        LocalDate localDate = MyClock.localDate(getCreateTime());
        setDayKey(localDate.getYear() * 10000 + localDate.getMonthValue() * 100 + localDate.getDayOfMonth());
    }
}
