package wxdgaming.backends.admin.login;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot.core.lang.RunResult;
import wxdgaming.boot.core.str.JwtUtils;
import wxdgaming.boot.core.threading.ThreadContext;
import wxdgaming.boot.net.http.HttpHeadNameType;
import wxdgaming.boot.net.web.hs.HttpSession;
import wxdgaming.boot.starter.pgsql.PgsqlService;

/**
 * 登录服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:46
 **/
@Slf4j
@Singleton
public class LoginService {

    final PgsqlService dataHelper;

    @Inject
    public LoginService(PgsqlService dataHelper) {
        this.dataHelper = dataHelper;
    }

    public RunResult checkLogin(HttpSession session) {
        Cookie cookie = session.getReqCookies().findCookie(HttpHeadNameType.AUTHORIZATION.getValue());
        if (cookie == null) {
            return RunResult.error("未登录");
        }
        try {
            Jws<Claims> claimsJws = JwtUtils.parseJWT(cookie.value());
            Long userId = claimsJws.getPayload().get("userId", Long.class);
            if (userId == null) {
                return RunResult.error("未登录");
            }
            User user = dataHelper.queryEntityByWhere(User.class, "uid = ?", userId);
            if (user == null) {
                return RunResult.error("账号异常");
            }
            if (user.isDisConnect()) {
                return RunResult.error("账号已被禁用");
            }
            ThreadContext.putContent("user", user);
            return RunResult.ok();
        } catch (Exception e) {
            return RunResult.error("登录已过期");
        }
    }

}
