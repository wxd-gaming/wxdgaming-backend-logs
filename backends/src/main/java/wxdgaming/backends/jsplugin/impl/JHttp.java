package wxdgaming.backends.jsplugin.impl;

import com.google.inject.Singleton;
import wxdgaming.boot2.starter.js.IJSPlugin;
import wxdgaming.boot2.starter.net.httpclient.HttpBuilder;

/**
 * 通关java http client 发送请求
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-18 17:38
 **/
@Singleton
public class JHttp implements IJSPlugin {


    @Override public String getName() {
        return "JHttp";
    }

    public String get(String url) {
        return HttpBuilder.get(url).request().bodyString();
    }

    public String post(String url, String body) {
        return HttpBuilder.postText(url, body).request().bodyString();
    }

    public String postJson(String url, String json) {
        return HttpBuilder.postJson(url, json).request().bodyString();
    }

}
