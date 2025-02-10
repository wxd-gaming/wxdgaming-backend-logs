package wxdgaming.backends.mudole.filter;

import com.google.inject.Singleton;
import wxdgaming.boot.net.web.hs.HttpListenerAction;
import wxdgaming.boot.starter.net.filter.HttpFilter;

/**
 * 主过滤器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-10 16:46
 **/
@Singleton
public class MainFilter extends HttpFilter {

    @Override public boolean doFilter(HttpListenerAction httpListenerAction) {
        // httpListenerAction.getSession().responseJson(RunResult.error("授权失败"));
        return true;
    }

}
