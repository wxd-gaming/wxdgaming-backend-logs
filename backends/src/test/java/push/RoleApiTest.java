package push;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.backends.entity.games.logs.RoleRecord;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 角色api操作
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 11:15
 **/
public class RoleApiTest extends AccountApiTest {

    protected String roleName = "无心道";

    @Test
    public void test() throws Exception {
        String logToken = findLogToken();
        HashMap<Integer, List<AccountRecord>> accountRecordMap = readAccount();
        for (List<AccountRecord> recordList : accountRecordMap.values()) {
            test(logToken, recordList);
        }
    }

    public void test(String logToken, List<AccountRecord> recordList) throws Exception {
        List<CompletableFuture<Response<PostText>>> futures = new ArrayList<>();
        for (AccountRecord accountRecord : recordList) {
            RoleRecord record = new RoleRecord();
            record.setUid(accountRecord.getUid());
            record.setAccount(accountRecord.getAccount());
            record.setCreateTime(accountRecord.getCreateTime());
            record.setCreateSid(RandomUtils.random(1, 100));
            record.setCurSid(RandomUtils.random(1, 100));
            record.setRoleName(StringUtils.randomString(8));
            record.setJob("魔剑士");
            record.setSex("男");
            record.setLv(RandomUtils.random(1, 100));
            record.getData().fluentPut("channel", "huawei");

            JSONObject push = new JSONObject()
                    .fluentPut("gameId", gameId)
                    .fluentPut("token", logToken)
                    .fluentPut("data", record);

            CompletableFuture<Response<PostText>> completableFuture = post("role/push", push.toJSONString());
            futures.add(completableFuture);
        }
        for (CompletableFuture<Response<PostText>> future : futures) {
            Response<PostText> join = future.join();
            RunResult runResult = join.bodyRunResult();
            if (join.responseCode() != 200 || runResult.code() != 1) {
                System.out.println(join.bodyString());
            }
        }
    }


}
