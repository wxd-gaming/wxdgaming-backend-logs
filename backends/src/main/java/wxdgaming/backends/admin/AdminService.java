package wxdgaming.backends.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.Sort;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.reflect.ReflectContext;
import wxdgaming.boot2.starter.batis.Entity;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;

/**
 * 管理服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:50
 **/
@Slf4j
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

}
