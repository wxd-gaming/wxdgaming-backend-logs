package push;

import org.junit.Test;
import wxdgaming.backends.entity.logs.RoleRecord;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        List<CompletableFuture> futures = new ArrayList<>();
        String logToken = findLogToken();
        for (int i = 1; i <= 1000; i++) {
            RoleRecord roleRecord = new RoleRecord();
            roleRecord.setGameId(gameId);
            roleRecord.setToken(logToken);
            roleRecord.setAccount(StringUtils.randomString(8));
            roleRecord.setCreateSid(RandomUtils.random(1, 100));
            roleRecord.setCurSid(RandomUtils.random(1, 100));
            roleRecord.setCreateTime(System.currentTimeMillis());
            roleRecord.setRoleId(String.valueOf(i));
            roleRecord.setRoleName(StringUtils.randomString(8));
            roleRecord.setJob("魔剑士");
            roleRecord.setSex("男");
            roleRecord.setLv(RandomUtils.random(1, 100));
            roleRecord.getData().fluentPut("channel", "huawei");
            CompletableFuture<Response<PostText>> completableFuture = post("role/push", roleRecord.toJsonString());
            futures.add(completableFuture);
        }
        for (CompletableFuture future : futures) {
            future.join();
        }
    }


}
