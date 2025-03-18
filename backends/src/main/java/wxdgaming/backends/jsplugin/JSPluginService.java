package wxdgaming.backends.jsplugin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import wxdgaming.boot2.starter.js.JSContext;
import wxdgaming.boot2.starter.js.JsService;

import java.util.function.Consumer;

/**
 * 插件服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-18 10:32
 **/
@Singleton
public class JSPluginService {

    final JsService jsService;

    @Inject
    public JSPluginService(JsService jsService) {
        this.jsService = jsService;
        this.jsService.getOnInitListener().add(new Consumer<JSContext>() {
            @Override public void accept(JSContext jsContext) {
                jsContext.evalFile("script/util.js");
            }
        });
    }

    public void init() {

    }
}
