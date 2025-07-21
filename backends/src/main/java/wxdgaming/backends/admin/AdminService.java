package wxdgaming.backends.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.ann.Order;
import wxdgaming.boot2.core.ann.Shutdown;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;

/**
 * 管理服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:50
 **/
@Slf4j
@Singleton
public class AdminService {

    final PgsqlDataHelper pgsqlDataHelper;

    @Inject
    public AdminService(PgsqlDataHelper pgsqlDataHelper) {
        this.pgsqlDataHelper = pgsqlDataHelper;
    }

    @Start
    @Order(100)
    public void start() throws Exception {

    }

    @Shutdown
    public void shutdown() {

    }

}
