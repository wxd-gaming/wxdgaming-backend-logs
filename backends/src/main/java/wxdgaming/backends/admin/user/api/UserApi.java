package wxdgaming.backends.admin.user.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.user.UserService;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlService;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.List;

/**
 * 系统用户管理
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-08 19:39
 **/
@Slf4j
@Singleton
@RequestMapping(path = "user")
public class UserApi {

    final PgsqlService pgsqlService;
    final UserService userService;

    @Inject
    public UserApi(PgsqlService pgsqlService, UserService userService) {
        this.pgsqlService = pgsqlService;
        this.userService = userService;
    }

    @HttpRequest(authority = 9)
    public RunResult resetPwd(HttpContext httpContext,
                              @ThreadParam() User user,
                              @Param(path = "uid") int uid,
                              @Param(path = "pwd") String pwd) {
        User findUser = pgsqlService.findByKey(User.class, uid);
        if (findUser == null) {
            return RunResult.error("用户不存在");
        }

        if (!user.isRoot() && uid == user.getUid()) {
            return RunResult.error("权限不足");
        }

        if (findUser.isAdmin()) {
            if (!user.isRoot()) {
                return RunResult.error("权限不足");
            }
        } else {
            if (findUser.getParentUid() != user.getUid()) {
                return RunResult.error("权限不足");
            }
        }

        String string = userService.md5Pwd(findUser.getUid(), findUser.getAccount(), pwd);
        findUser.setPwd(string);

        pgsqlService.update(findUser);
        log.info("管理员 {} 修改用户 {} 密码", user.getAccount(), findUser.getAccount());
        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    public RunResult ban(HttpContext httpContext,
                         @ThreadParam() User user,
                         @Param(path = "uid") int uid) {
        if (uid == user.getUid()) {
            return RunResult.error("不能禁用自己");
        }
        User findUser = pgsqlService.findByKey(User.class, uid);
        if (findUser == null) {
            return RunResult.error("用户不存在");
        }

        if (findUser.isAdmin()) {
            if (!user.isRoot()) {
                return RunResult.error("权限不足");
            }
        } else {
            if (findUser.getParentUid() != user.getUid()) {
                return RunResult.error("权限不足");
            }
        }

        findUser.setDisConnect(true);
        pgsqlService.update(findUser);
        log.info("管理员 {} 用户 {} 被禁用", user.getAccount(), findUser.getAccount());
        return RunResult.ok();
    }

    @HttpRequest()
    public RunResult list(HttpContext httpContext,
                          @ThreadParam() User user,
                          @Param(path = "account") String account) {

        if (!user.isRoot() && !user.isAdmin()) {
            return RunResult.error("权限不足");
        }

        SqlQueryBuilder sqlQueryBuilder = pgsqlService.queryBuilder();
        sqlQueryBuilder.sqlByEntity(User.class);

        sqlQueryBuilder.pushWhereByValueNotNull("account=?", account);

        if (!user.isRoot()) {
            sqlQueryBuilder.pushWhere("parentuid=?", user.getUid());
        }

        long count = sqlQueryBuilder.findCount();

        List<User> list2Entity = sqlQueryBuilder.findList2Entity(User.class);

        List<JSONObject> list = list2Entity.stream()
                .map(u -> {
                    JSONObject jsonObject = u.toJSONObject();
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", u.getCreatedTime()));
                    jsonObject.remove("authorizationGames");
                    jsonObject.remove("authorizationRouting");
                    return jsonObject;
                })
                .toList();
        return RunResult.ok().data(list).fluentPut("rowCount", count);
    }

}
