package wxdgaming.backends.admin.filter;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.executor.ThreadContext;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.Md5Util;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.server.http.HttpContext;
import wxdgaming.boot2.starter.net.server.http.HttpFilter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限过滤器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-19 15:02
 **/
@Slf4j
@Singleton
public class Authority_1_AppToken_Filter implements HttpFilter {

    final GameService gameService;

    @Inject
    public Authority_1_AppToken_Filter(GameService gameService) {
        this.gameService = gameService;
    }


    @Override public Object doFilter(HttpRequest httpRequest, Method method, HttpContext httpContext) {
        if (httpRequest != null && Objects.checkHave(httpRequest.authority(), 1)) {

            JSONObject reqParams = httpContext.getRequest().getReqParams();

            Integer gameId = reqParams.getInteger("gameId");
            if (gameId == null) {
                return RunResult.fail("参数gameId不能为空");
            }
            GameContext gameContext = gameService.gameContext(gameId);
            if (gameContext == null) {
                return RunResult.fail("gameId is not exist");
            }

            String token = reqParams.getString("token");
            if (token == null) {
                return RunResult.fail("参数token不能为空");
            }

            String rawToken = reqParams.entrySet().stream()
                    .filter(v -> !v.getKey().equals("token"))
                    .sorted(Map.Entry.comparingByKey())
                    .map(v -> String.valueOf(v.getKey()) + "=" + FastJsonUtil.toJSONString(v.getValue()))
                    .collect(Collectors.joining());

            log.info("rawToken:{}", rawToken);

            String sign = Md5Util.md5DigestEncode(rawToken, gameContext.getGame().getAppToken());

            if (!Objects.equals(sign, token)) {
                return RunResult.fail("game log token error");
            }
            ThreadContext.putContent(gameContext);
        }
        return null;
    }

}
