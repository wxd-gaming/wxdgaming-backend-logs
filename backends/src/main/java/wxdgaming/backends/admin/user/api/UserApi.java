package wxdgaming.backends.admin.user.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.admin.user.UserService;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

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

    final UserService userService;
    final GameService gameService;
    final HexId hexId = new HexId(1);

    @Inject
    public UserApi(UserService userService, GameService gameService) {
        this.userService = userService;
        this.gameService = gameService;
    }

    @HttpRequest(authority = 9)
    public RunResult resetPwd(HttpContext httpContext,
                              @ThreadParam() User user,
                              @Param(path = "account") String account,
                              @Param(path = "pwd") String pwd) {

        if (StringUtils.isBlank(pwd) || pwd.length() < 6) {
            return RunResult.error("密码长度不能小于6位");
        }

        User findUser = userService.findByAccount(account);
        if (findUser == null) {
            return RunResult.error("用户不存在");
        }

        if (!user.isRoot() && Objects.equals(account, user.getAccount())) {
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
        findUser.setUpdateIndex(findUser.getUpdateIndex() + 1);

        userService.getPgsqlService().update(findUser);

        log.info("管理员 {} 修改用户 {} 密码", user.getAccount(), findUser.getAccount());
        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    public RunResult add(HttpContext httpContext,
                         @ThreadParam() User admin,
                         @Param(path = "account") String account,
                         @Param(path = "pwd") String pwd,
                         @Param(path = "phone") String phone) {

        if (StringUtils.isBlank(account) || account.length() < 4) {
            return RunResult.error("账号长度不能小于4位");
        }

        if (StringUtils.isBlank(pwd) || pwd.length() < 6) {
            return RunResult.error("密码长度不能小于6位");
        }

        if (StringUtils.isBlank(phone) || phone.length() < 11) {
            return RunResult.error("手机号码格式错误");
        }

        if (userService.findByAccount(account) != null) {
            return RunResult.error("账号已存在");
        }

        if (!admin.isRoot() && !admin.isAdmin()) {
            return RunResult.error("权限不足");
        }

        /*这里是添加默认管理员*/
        User newUser = new User();
        newUser.setCreatedTime(System.currentTimeMillis());
        newUser.setUid(hexId.newId());
        newUser.setAccount(account);
        newUser.setUpdateIndex(1);
        newUser.setPhone(phone);
        if (admin.isRoot()) {
            newUser.setAdmin(true);
        } else if (admin.isAdmin()) {
            /*设置父账号*/
            newUser.setParentUid(admin.getUid());
        }
        newUser.setPwd(userService.md5Pwd(newUser.getUid(), newUser.getAccount(), pwd));
        userService.getPgsqlService().insert(newUser);
        userService.getUserCache().put(newUser.getAccount(), newUser);

        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    public RunResult authorGames(HttpContext httpContext,
                                 @ThreadParam() User user,
                                 @Param(path = "account") String account,
                                 @Param(path = "authors", defaultValue = "[]") List<Integer> authors) {
        if (!(user.isAdmin() || user.isRoot())) {
            return RunResult.error("权限不足");
        }
        if (Objects.equals(account, user.getAccount())) {
            return RunResult.error("无法给自己授权");
        }
        User byAccount = userService.findByAccount(account);
        if (byAccount == null) {
            return RunResult.error("用户不存在");
        }

        if (byAccount.isAdmin()) {
            if (!user.isRoot()) {
                return RunResult.error("权限不足");
            }
        }

        HashSet<Integer> integers = new HashSet<>(authors);
        if (!user.isRoot()) {
            /*我要给别的账号授权，前提是我必须有权限*/
            HashSet<Integer> authorizationGames = user.getAuthorizationGames();
            integers.removeIf(v -> !authorizationGames.contains(v));
        }
        byAccount.setAuthorizationGames(integers);

        return RunResult.ok();
    }

    @HttpRequest(authority = 9)
    public RunResult authorGamesList(HttpContext session,
                                     @ThreadParam() User user,
                                     @Param(path = "account") String account) {

        if (Objects.equals(account, user.getAccount())) {
            return RunResult.error("无法给自己授权");
        }

        User byAccount = userService.findByAccount(account);
        if (byAccount == null) {
            return RunResult.error("用户不存在");
        }

        List<JSONObject> list = gameService.getGameContextHashMap().values()
                .stream()
                .sorted(Comparator.comparingInt(GameContext::getGameId))
                .map(GameContext::getGame)
                .filter(game -> user.isRoot() || user.getAuthorizationGames().contains(game.getUid()))
                .map(game -> {
                    JSONObject jsonObject = game.toJSONObject();
                    jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm", jsonObject.getLong("createTime")));
                    jsonObject.put("checked", byAccount.getAuthorizationGames().contains(game.getUid()) ? "checked" : "");
                    return jsonObject;
                })
                .toList();
        return RunResult.ok().fluentPut("data", list).fluentPut("rowCount", list.size());
    }

    @HttpRequest(authority = 9)
    public RunResult ban(HttpContext httpContext,
                         @ThreadParam() User user,
                         @Param(path = "account") String account) {

        if (Objects.equals(account, user.getAccount())) {
            return RunResult.error("不能禁用自己");
        }

        User findUser = userService.findByAccount(account);
        if (findUser.isAdmin()) {
            if (!user.isRoot()) {
                return RunResult.error("权限不足");
            }
        } else {
            if (findUser.getParentUid() != user.getUid()) {
                return RunResult.error("权限不足");
            }
        }

        findUser.setDisConnect(!findUser.isDisConnect());

        userService.getPgsqlService().update(findUser);
        log.info("管理员 {} 用户 {} 被禁用", user.getAccount(), findUser.getAccount());
        return RunResult.ok();
    }

    @HttpRequest()
    public RunResult list(HttpContext httpContext,
                          @ThreadParam() User user,
                          @Param(path = "pageIndex") int pageIndex,
                          @Param(path = "pageSize") int pageSize,
                          @Param(path = "account") String account) {

        if (!user.isRoot() && !user.isAdmin()) {
            return RunResult.error("权限不足");
        }

        SqlQueryBuilder queryBuilder = userService.getPgsqlService().queryBuilder();
        queryBuilder.sqlByEntity(User.class);

        queryBuilder.pushWhereByValueNotNull("account=?", account);

        if (!user.isRoot()) {
            queryBuilder.pushWhere("parentuid=?", user.getUid());
        }

        queryBuilder.pushWhere("uid<>?", 1);
        queryBuilder.setOrderBy("uid desc");
        queryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);

        long count = queryBuilder.findCount();

        List<User> list2Entity = queryBuilder.findList2Entity(User.class);

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
