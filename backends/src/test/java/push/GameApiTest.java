package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import wxdgaming.backends.entity.RecordBase;
import wxdgaming.backends.entity.system.GameRecord;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.threading.ExecutorConfig;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.starter.net.httpclient.HttpBuilder;

import java.util.LinkedHashMap;

/**
 * test
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 14:02
 **/
@Slf4j
public class GameApiTest {

    protected int gameId = 2;
    protected String appToken = "34g3fy";
    protected String logToken = "42e8sxgm5FVF18b3iSNQVR0jof3FIUMgD6p922pUm36aubm70Tn5C7A5b3m8NlaE";

    static {
        ExecutorUtil.init(new ExecutorConfig());
    }

    public String push(String path, RecordBase base) {

        String json = base.toJsonString();
        return push(path, json);
    }

    public String push(String path, String json) {
        System.out.println(json);
        String string = HttpBuilder.postJson("http://127.0.0.1:19000/" + path, json)
                .retry(2)
                .request()
                .bodyString();
        System.out.println(string);
        return string;
    }

    public JSONObject listLogType() {
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject
                .fluentPut("gameId", gameId)
                .fluentPut("token", appToken);

        String push = push("game/listLogType", jsonObject.toJSONString());
        RunResult runResult = RunResult.parse(push);
        if (runResult.code() != 1)
            throw new RuntimeException(runResult.msg());
        return runResult.data(JSONObject.class);
    }

    @Test
    public void pushGame() {
        GameRecord gameRecord = new GameRecord();
        gameRecord.setUid(2L);
        gameRecord.setName("野火燎原");
        gameRecord.setIcon("icon");
        gameRecord.setDesc("desc");
        gameRecord.setUrl("url");

        LinkedHashMap<String, String> tableMapping = gameRecord.getTableMapping();
        tableMapping.put("log_item", "道具日志");
        tableMapping.put("log_pay", "支付日志");
        tableMapping.put("log_login", "登录日志");

        push("game/push", gameRecord);
    }

    @Test
    public void pushLogType() {
        JSONObject jsonObject = MapOf.newJSONObject();
        jsonObject
                .fluentPut("gameId", gameId)
                .fluentPut("token", appToken)
                .fluentPut("logType", "log_lv")
                .fluentPut("logComment", "等级日志");

        push("game/addLogType", jsonObject.toJSONString());
    }


}
