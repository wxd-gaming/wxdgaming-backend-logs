package wxdgaming.backends.admin.login.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.AdminService;
import wxdgaming.backends.admin.login.LoginService;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot.agent.io.Objects;
import wxdgaming.boot.core.lang.RunResult;
import wxdgaming.boot.core.str.JwtUtils;
import wxdgaming.boot.core.str.Md5Util;
import wxdgaming.boot.net.controller.ann.Param;
import wxdgaming.boot.net.controller.ann.TextController;
import wxdgaming.boot.net.controller.ann.TextMapping;
import wxdgaming.boot.net.http.HttpHeadNameType;
import wxdgaming.boot.net.web.hs.HttpSession;
import wxdgaming.boot.starter.pgsql.PgsqlService;

import java.util.concurrent.TimeUnit;

/**
 * 登录，登出
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-11 09:49
 **/
@Slf4j
@Singleton
@TextController(path = "/")
public class LoginApi {

    PgsqlService dataHelper;
    LoginService loginService;

    @Inject
    public LoginApi(LoginService loginService, PgsqlService dataHelper) {
        this.loginService = loginService;
        this.dataHelper = dataHelper;
    }

    @TextMapping
    public RunResult login(HttpSession session, @Param("account") String account, @Param("pwd") String pwd) {
        User user = dataHelper.queryEntityByWhere(User.class, "account = ?", account);
        if (user == null) {
            return RunResult.error("账号不存在");
        }

        String md5Sign = Md5Util.md5DigestEncode(String.valueOf(user.getUid()), user.getAccount(), AdminService.PWDKEY, pwd);
        if (!Objects.equals(md5Sign, user.getPwd())) {
            return RunResult.error("密码错误");
        }

        if (user.isDisConnect()) {
            return RunResult.error("账号已被禁用");
        }

        /*设置6天的过期时间*/
        long daysMillis = TimeUnit.DAYS.toMillis(6);

        String outToken = JwtUtils.createJwtBuilder(daysMillis)
                .claim("userId", user.getUid())
                .compact();

        session.getResCookie().addCookie(HttpHeadNameType.AUTHORIZATION.getValue(), outToken, "/", null, daysMillis);

        return RunResult.ok();
    }

    @TextMapping
    public RunResult checkLogin(HttpSession session) {
        return loginService.checkLogin(session);
    }


}
