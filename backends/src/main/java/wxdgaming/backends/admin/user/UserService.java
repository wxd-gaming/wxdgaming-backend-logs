package wxdgaming.backends.admin.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.BootConfig;
import wxdgaming.boot2.core.ann.Sort;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.shutdown;
import wxdgaming.boot2.core.util.AssertUtil;
import wxdgaming.boot2.core.util.Md5Util;
import wxdgaming.boot2.starter.batis.sql.JdbcCache;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;

/**
 * 用户服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 20:50
 **/
@Slf4j
@Getter
@Singleton
public class UserService {

    /** 管理员账号 */
    public static String ROOT = null;
    /** 密钥key */
    public static String PWDKEY = null;

    final PgsqlService pgsqlService;
    final JdbcCache<User, String> userCache;

    @Inject
    public UserService(PgsqlService pgsqlService) {
        this.pgsqlService = pgsqlService;
        ROOT = BootConfig.getIns().getNestedValue("other.root", String.class);
        AssertUtil.assertNull(ROOT, "json path=other.root root 账号配置异常");
        PWDKEY = BootConfig.getIns().getNestedValue("other.pwd-key", String.class);
        AssertUtil.assertNull(ROOT, "json path=other.pwd-key 密码 md5 私钥配置异常");
        userCache = new JdbcCache<User, String>(this.pgsqlService, 1, 120/*120分钟*/) {
            @Override protected User loader(String account) {
                return pgsqlService.findByWhere(User.class, "account = ?", account);
            }
        };
    }

    @Start
    @Sort(10000000)
    public void start() {
        /*这里是添加默认管理员*/
        User user = userCache.getIfPresent(ROOT);
        if (user == null) {
            user = new User();
            user.setCreatedTime(System.currentTimeMillis());
            user.setUid(1);
            user.setAccount(ROOT);
            user.setUpdateIndex(1);
            user.setPhone("15388152619");
            user.setRoot(true);
            user.setAdmin(true);
            user.setPwd(md5Pwd(user.getUid(), user.getAccount(), "123456"));
            userCache.put(user.getAccount(), user);
        }
    }

    @shutdown
    public void shutdown() {
        userCache.shutdown();
    }

    public String md5Pwd(long uid, String account, String password) {
        return Md5Util.md5DigestEncode(String.valueOf(uid), account, PWDKEY, password);
    }

    public User findByAccount(String account) {
        return userCache.getIfPresent(account);
    }

}
