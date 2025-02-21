package wxdgaming.backends.entity.games.logs;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.EntityLongUID;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.sql.ann.Partition;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

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

    /** 不写入数据库的用于验证的 */
    @JSONField(ordinal = 10)
    @DbColumn(ignore = true)
    private String token;
    /** 不写入数据库的用于验证的 */
    @JSONField(ordinal = 11)
    @DbColumn(ignore = true)
    private int gameId;
    @Partition
    @JSONField(ordinal = 13)
    @DbColumn(key = true)
    private int dayKey;
    @JSONField(ordinal = 12)
    @DbColumn(index = true)
    private long createTime;

    public void checkDataKey() {
        if (getCreateTime() == 0) {
            setCreateTime(System.currentTimeMillis());
        }

        LocalDate localDate = MyClock.localDate(getCreateTime());
        setDayKey(localDate.getYear() * 10000 + localDate.getMonthValue() * 100 + localDate.getDayOfMonth());
    }

}
