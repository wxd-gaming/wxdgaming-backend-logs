package wxdgaming.backends.admin.login;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ThreadContext;
import wxdgaming.boot2.core.util.JwtUtils;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;
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

    final PgsqlService dataHelper;

    @Inject
    public LoginService(PgsqlService dataHelper) {
        this.dataHelper = dataHelper;
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
            Long userId = claimsJws.getPayload().get("userId", Long.class);
            if (userId == null) {
                return RunResult.error("未登录");
            }
            User user = dataHelper.findByWhere(User.class, "uid = ?", userId);
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
