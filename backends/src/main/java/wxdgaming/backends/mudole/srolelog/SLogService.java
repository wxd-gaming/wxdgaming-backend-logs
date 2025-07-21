package wxdgaming.backends.mudole.srolelog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.entity.games.logs.ActiveAccountRecord;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;

/**
 * 日志服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 16:31
 **/
@Singleton
public class SLogService {

    PgsqlDataHelper pgsqlDataHelper;

    @Inject
    public SLogService(PgsqlDataHelper pgsqlDataHelper) {
        this.pgsqlDataHelper = pgsqlDataHelper;
    }

    /** 刷新账号的活跃记录 */
    public void refreshActiveAccount(GameContext gameContext, String account, long oldTime, long refreshTime) {
        if (!MyClock.isSameDay(oldTime, refreshTime)) {
            /*跨天记录一条活跃数据*/
            ActiveAccountRecord activeAccountRecord = new ActiveAccountRecord();
            activeAccountRecord.setUid(gameContext.newId(ActiveAccountRecord.class.getName()));
            activeAccountRecord.setAccount(account);
            activeAccountRecord.setCreateTime(refreshTime);
            activeAccountRecord.checkDataKey();
            gameContext.getDataHelper().getDataBatch().insert(activeAccountRecord);
        }
    }

}
