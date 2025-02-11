package wxdgaming.backends.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot.core.format.HexId;
import wxdgaming.boot.core.str.Md5Util;
import wxdgaming.boot.starter.BootConfig;
import wxdgaming.boot.starter.IocContext;
import wxdgaming.boot.starter.i.IStart;
import wxdgaming.boot.starter.pgsql.PgsqlService;

/**
 * 管理服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:50
 **/
@Slf4j
@Singleton
public class AdminService implements IStart {

    /** 管理员账号 */
    public static String ROOT = null;
    /** 密钥key */
    public static String PWDKEY = null;
    final HexId hexId = new HexId(1);

    final PgsqlService dataHelper;

    @Inject
    public AdminService(PgsqlService dataHelper) {
        this.dataHelper = dataHelper;
        ROOT = BootConfig.getInstance().other("root");
        PWDKEY = BootConfig.getInstance().other("pwd-key");
    }

    @Override public void start(IocContext iocInjector) throws Exception {
        /*这里是添加默认管理员*/
        User user = dataHelper.queryEntityByWhere(User.class, "account = ?", ROOT);
        if (user == null) {
            user = new User();
            user.setCreatedTime(System.currentTimeMillis());
            user.setUid(hexId.newId());
            user.setAccount(ROOT);
            user.setPwd(Md5Util.md5DigestEncode(String.valueOf(user.getUid()), user.getAccount(), PWDKEY, "123456"));
            user.setAdmin(true);
            dataHelper.insert(user);
        }
    }

}
