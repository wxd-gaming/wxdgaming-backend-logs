package wxdgaming.backends.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.ann.Sort;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.shutdown;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;

/**
 * 管理服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:50
 **/
@Slf4j
@Getter
@Singleton
public class AdminService {

    final PgsqlService pgsqlService;

    @Inject
    public AdminService(PgsqlService pgsqlService) {
        this.pgsqlService = pgsqlService;
    }

    @Start
    @Sort(100)
    public void start() throws Exception {

    }

    @shutdown
    public void shutdown() {

    }

}
