package wxdgaming.backends.admin.login.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.login.LoginService;
import wxdgaming.backends.admin.user.UserService;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.JwtUtils;
import wxdgaming.boot2.core.util.Md5Util;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;
import wxdgaming.boot2.starter.net.http.HttpHeadNameType;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.concurrent.TimeUnit;

/**
 * 登录，登出
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:49
 **/
@Slf4j
@Singleton
@RequestMapping(path = "/")
public class LoginApi {

    PgsqlService dataHelper;
    LoginService loginService;

    @Inject
    public LoginApi(LoginService loginService, PgsqlService dataHelper) {
        this.loginService = loginService;
        this.dataHelper = dataHelper;
    }

    @HttpRequest
    public RunResult login(HttpContext httpContext, @Param(path = "account") String account, @Param(path = "pwd") String pwd) {
        User user = dataHelper.findByWhere(User.class, "account = ?", account);
        if (user == null) {
            return RunResult.error("账号不存在");
        }

        String md5Sign = Md5Util.md5DigestEncode(String.valueOf(user.getUid()), user.getAccount(), UserService.PWDKEY, pwd);
        if (!Objects.equals(md5Sign, user.getPwd())) {
            return RunResult.error("密码错误");
        }

        if (user.isDisConnect()) {
            return RunResult.error("账号已被禁用");
        }

        /*设置6天的过期时间*/
        long daysMillis = TimeUnit.DAYS.toMillis(6);

        String outToken = JwtUtils.createJwtBuilder(daysMillis)
                .claim("account", user.getAccount())
                .compact();

        httpContext.getResponse().getResponseCookie().addCookie(HttpHeadNameType.AUTHORIZATION.getValue(), outToken, "/", null, daysMillis);

        return RunResult.ok();
    }

    @HttpRequest
    public RunResult checkLogin(HttpContext httpContext) {
        return loginService.checkLogin(httpContext);
    }


}
