package wxdgaming.backends.mudole.log;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import wxdgaming.backends.entity.logs.SLog;
import wxdgaming.boot.starter.pgsql.PgsqlService;

import java.util.List;

/**
 * 日志服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-22 16:31
 **/
@Singleton
public class LogsService {



    PgsqlService psqlService;


    @Inject
    public LogsService(PgsqlService psqlService) {
        this.psqlService = psqlService;
    }



}
