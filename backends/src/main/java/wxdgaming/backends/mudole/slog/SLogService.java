package wxdgaming.backends.mudole.slog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;

/**
 * 日志服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 16:31
 **/
@Singleton
public class SLogService {

    PgsqlService psqlService;

    @Inject
    public SLogService(PgsqlService psqlService) {
        this.psqlService = psqlService;
    }

}
