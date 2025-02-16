package wxdgaming.backends.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.BootConfig;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.reflect.GuiceReflectContext;
import wxdgaming.boot2.core.reflect.ReflectContext;
import wxdgaming.boot2.core.util.Md5Util;
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

    /** 管理员账号 */
    public static String ROOT = null;
    /** 密钥key */
    public static String PWDKEY = null;
    final HexId hexId = new HexId(1);

    final PgsqlService pgsqlService;

    @Inject
    public AdminService(PgsqlService pgsqlService) {
        this.pgsqlService = pgsqlService;
        ROOT = BootConfig.getIns().getNestedValue("other.root", String.class);
        PWDKEY = BootConfig.getIns().getNestedValue("other.pwd-key", String.class);
    }

    @Start
    public void start() throws Exception {
        ReflectContext reflectContext = ReflectContext.Builder.of(User.class.getPackageName()).build();
        reflectContext.classWithSuper(Entity.class)
                .forEach(cls -> {
                    pgsqlService.checkTable(cls);
                });

        /*这里是添加默认管理员*/
        User user = pgsqlService.findByWhere(User.class, "account = ?", ROOT);
        if (user == null) {
            user = new User();
            user.setCreatedTime(System.currentTimeMillis());
            user.setUid(hexId.newId());
            user.setAccount(ROOT);
            user.setPwd(Md5Util.md5DigestEncode(String.valueOf(user.getUid()), user.getAccount(), PWDKEY, "123456"));
            user.setAdmin(true);
            pgsqlService.insert(user);
        }
    }

}
