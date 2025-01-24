package wxdgaming.backends.mudole.log.api;

import com.alibaba.fastjson.JSONObject;
import wxdgaming.boot.net.controller.ann.TextController;
import wxdgaming.boot.net.controller.ann.TextMapping;
import wxdgaming.boot.net.web.hs.HttpSession;

/**
 * 角色接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-23 20:55
 **/
@TextController
public class RoleApi {

    @TextMapping
    public String push(HttpSession session) {
        return "ok";
    }

}
