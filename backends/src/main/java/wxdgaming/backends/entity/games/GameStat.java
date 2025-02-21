package wxdgaming.backends.entity.games;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.starter.batis.EntityIntegerUID;
import wxdgaming.boot2.starter.batis.ann.DbColumn;
import wxdgaming.boot2.starter.batis.ann.DbTable;

/**
 * 游戏统计
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-21 20:30
 **/
@Getter
@Setter
@DbTable
public class GameStat extends EntityIntegerUID {

    @DbColumn(index = true)
    private long registerAccountNum;
    /** 充值金额 */
    @DbColumn(index = true)
    private long rechargeAmountNum;
    /** 统计充值的订单数 */
    @DbColumn(index = true)
    private long rechargeOrderNum;
    @DbColumn(index = true)
    private long loginAccountNum;
    @DbColumn(index = true)
    private long rechargeAccountNum;
    /** 今天注册就充值的账号数量 */
    @DbColumn(index = true)
    private long registerAccountRechargeNum;

    private String arpu;
    private String arppu;
    /** 今天注册的账号，今天就有充值的账号付费率 */
    private String fufeilv;

}
