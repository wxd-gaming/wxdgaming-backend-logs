package wxdgaming.backends.admin.login;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.user.UserService;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ThreadContext;
import wxdgaming.boot2.core.util.JwtUtils;
import wxdgaming.boot2.starter.net.http.HttpHeadNameType;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

/**
 * 登录服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:46
 **/
@Slf4j
@Singleton
public class LoginService {

    private final UserService userService;

    @Inject
    public LoginService(UserService userService) {
        this.userService = userService;
    }

    public RunResult checkLogin(HttpContext httpContext) {
        String token = null;
        Cookie cookie = httpContext.getRequest().getReqCookies().findCookie(HttpHeadNameType.AUTHORIZATION.getValue());
        if (cookie != null) token = cookie.value();
        else token = httpContext.getRequest().header(HttpHeadNameType.AUTHORIZATION.getValue());

        if (StringUtils.isBlank(token)) {
            return RunResult.error("未登录");
        }

        try {
            Jws<Claims> claimsJws = JwtUtils.parseJWT(token);
            String account = claimsJws.getPayload().get("account", String.class);
            if (account == null) {
                return RunResult.error("未登录");
            }
            User user = userService.findByAccount(account);
            if (user == null) {
                return RunResult.error("账号异常");
            }
            if (user.isDisConnect()) {
                return RunResult.error("账号已被禁用");
            }
            ThreadContext.putContent("user", user);
            return RunResult.ok();
        } catch (Exception e) {
            log.error("登录校验失败", e);
            return RunResult.error("登录已过期");
        }
    }

}
