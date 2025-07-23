package wxdgaming.backends.jsplugin.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.boot2.core.util.Md5Util;
import wxdgaming.boot2.starter.js.IJSPlugin;
import wxdgaming.boot2.starter.net.httpclient5.GetRequest;
import wxdgaming.boot2.starter.net.httpclient5.PostRequest;

/**
 * 通关java http client 发送请求
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-18 17:38
 **/
@Singleton
public class JHttp implements IJSPlugin {

    final GameService gameService;

    @Inject
    public JHttp(GameService gameService) {
        this.gameService = gameService;
    }

    @Override public String getName() {
        return "JHttp";
    }

    public String get(String url) {
        return GetRequest.of(url).execute().bodyString();
    }

    public String post(String url, String body) {
        return PostRequest.of(url, body).execute().bodyString();
    }

    public String postJson(String url, String json) {
        return PostRequest.ofJson(url, json).execute().bodyString();
    }

    public String postMail(String url, String json) {
        return PostRequest.ofJson(url, json).execute().bodyString();
    }

    public String md5(int gameId, String str) {
        GameContext gameContext = gameService.gameContext(gameId);
        return Md5Util.md5DigestEncode("&", str, gameContext.getGame().getAppToken());
    }

}
