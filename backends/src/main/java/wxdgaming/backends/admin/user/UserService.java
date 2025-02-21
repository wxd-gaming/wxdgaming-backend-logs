package wxdgaming.backends.admin.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.BootConfig;
import wxdgaming.boot2.core.ann.Sort;
import wxdgaming.boot2.core.ann.Start;
import wxdgaming.boot2.core.format.HexId;
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
@Singleton
public class UserService {

    /** 管理员账号 */
    public static String ROOT = null;
    /** 密钥key */
    public static String PWDKEY = null;
    final HexId hexId = new HexId(1);

    final PgsqlService pgsqlService;
    final JdbcCache<User, String> userCache;

    @Inject
    public UserService(PgsqlService pgsqlService) {
        this.pgsqlService = pgsqlService;
        ROOT = BootConfig.getIns().getNestedValue("other.root", String.class);
        AssertUtil.assertNull(ROOT, "json path=other.root root 账号配置异常");
        PWDKEY = BootConfig.getIns().getNestedValue("other.pwd-key", String.class);
        AssertUtil.assertNull(ROOT, "json path=other.pwd-key 密码 md5 私钥配置异常");
        userCache = new JdbcCache<User, String>(this.pgsqlService, 120/*120分钟*/) {
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
            user.setPwd(Md5Util.md5DigestEncode(String.valueOf(user.getUid()), user.getAccount(), PWDKEY, "123456"));
            user.setAdmin(true);
            userCache.put(user.getAccount(), user);
        }
    }

    public User findByAccount(String account) {
        return userCache.getIfPresent(account);
    }

}
