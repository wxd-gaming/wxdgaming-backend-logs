package push;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import reactor.core.publisher.Mono;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * 角色api操作
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 11:15
 **/
public class RoleApiTest extends AccountApiTest {

    protected String roleName = "无心道";

    @Test
    public void pushRoleList() throws Exception {
        String logToken = findLogToken();
        HashMap<Integer, List<JSONObject>> accountRecordMap = readAccount();
        CountDownLatch countDownLatch = new CountDownLatch(accountRecordMap.size());
        for (List<JSONObject> recordList : accountRecordMap.values()) {
            executorServices.execute(() -> {
                try {
                    pushRoleList(logToken, recordList);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
    }

    public void pushRoleList(String logToken, List<JSONObject> recordList) {

        List<JSONObject> roleRecordList = new ArrayList<>();
        for (JSONObject accountRecord : recordList) {
            JSONObject record = new JSONObject();
            record.put("uid", accountRecord.getLong("uid"));
            record.put("account", accountRecord.getString("account"));
            record.put("createTime", accountRecord.getLong("createTime"));
            record.put("createSid", RandomUtils.random(1, 100));
            record.put("curSid", RandomUtils.random(1, 100));
            record.put("roleName", StringUtils.randomString(8));
            record.put("Job", "魔剑士");
            record.put("sex", "男");
            record.put("lv", RandomUtils.random(1, 100));
            record.put("other", new JSONObject().fluentPut("channel", "huawei"));/*附加参数，本身也是一个json*/

            roleRecordList.add(record);
        }
        JSONObject push = new JSONObject()
                .fluentPut("gameId", gameId)
                .fluentPut("token", logToken)
                .fluentPut("data", roleRecordList);

        Mono<Response<PostText>> future = post("log/role/pushList", push.toJSONString());
        Response<PostText> join = future.block();
        RunResult runResult = join.bodyRunResult();
        if (join.responseCode() != 200 || runResult.code() != 1) {
            System.out.println(join.bodyString());
        }
    }


    public void pushRole() {

        JSONObject record = new JSONObject();
        record.put("uid", 1);
        record.put("account", "xxxx");
        record.put("createTime", System.currentTimeMillis());
        record.put("createSid", RandomUtils.random(1, 100));
        record.put("curSid", RandomUtils.random(1, 100));
        record.put("roleName", StringUtils.randomString(8));
        record.put("Job", "魔剑士");
        record.put("sex", "男");
        record.put("lv", RandomUtils.random(1, 100));
        record.put("other", new JSONObject().fluentPut("channel", "huawei"));/*附加参数，本身也是一个json*/

        JSONObject push = new JSONObject()
                .fluentPut("gameId", gameId)
                .fluentPut("token", "logToken")
                .fluentPut("data", record);

        post("log/role/push", push.toJSONString());

    }


}
