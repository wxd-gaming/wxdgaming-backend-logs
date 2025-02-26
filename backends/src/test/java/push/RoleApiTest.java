package push;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
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
        HashMap<Integer, List<AccountRecord>> accountRecordMap = readAccount();
        CountDownLatch countDownLatch = new CountDownLatch(accountRecordMap.size());
        for (List<AccountRecord> recordList : accountRecordMap.values()) {
            executorServices.execute(() -> {
                pushRoleList(logToken, recordList);
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
    }

    public void pushRoleList(String logToken, List<AccountRecord> recordList) {

        List<RoleRecord> roleRecordList = new ArrayList<>();
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
            record.getOther().fluentPut("channel", "huawei");

            roleRecordList.add(record);


        }
        JSONObject push = new JSONObject()
                .fluentPut("gameId", gameId)
                .fluentPut("token", logToken)
                .fluentPut("data", roleRecordList);

        CompletableFuture<Response<PostText>> future = post("role/pushList", push.toJSONString());
        Response<PostText> join = future.join();
        RunResult runResult = join.bodyRunResult();
        if (join.responseCode() != 200 || runResult.code() != 1) {
            System.out.println(join.bodyString());
        }
    }


}
