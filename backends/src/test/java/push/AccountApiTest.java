package push;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import reactor.core.publisher.Mono;
import wxdgaming.backends.entity.games.logs.AccountRecord;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.io.FileWriteUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * test
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 14:02
 **/
@Slf4j
public class AccountApiTest extends GameApiTest {


    @Test
    public void createAccount() throws Exception {
        HashMap<Integer, List<AccountRecord>> recordMap = new HashMap<>();
        LocalDateTime localDateTime = LocalDateTime.now().plusDays(-(days));
        for (int i = 1; i <= days; i++) {
            /*模拟每天注册的人数*/
            int random = RandomUtils.random(50, 200);
            LocalDateTime time = localDateTime.plusDays(i);
            long time2Milli = MyClock.time2Milli(time);
            System.out.println(time);
            List<AccountRecord> list = recordMap.computeIfAbsent(i, k -> new ArrayList<>());
            for (int j = 0; j < random; j++) {
                AccountRecord record = new AccountRecord();
                record.setUid(hexId.newId());
                record.setCreateTime(time2Milli);
                record.setAccount(i + "-" + j + "-" + randomAccount());
                record.checkDataKey();
                record.getOther().fluentPut("channel", "huawei").fluentPut("os", "huawei");
                list.add(record);
            }
        }
        FileWriteUtil.writeString("src/test/resources/account.json", FastJsonUtil.toJSONStringAsFmt(recordMap));
    }

    @Test
    public void pushAccount() throws Exception {
        JSONObject push = new JSONObject()
                .fluentPut("gameId", gameId)
                .fluentPut("token", "logToken")
                .fluentPut("data",
                        new JSONObject()
                                .fluentPut("uid", System.currentTimeMillis())
                                .fluentPut("account", String.valueOf(System.currentTimeMillis()))
                                .fluentPut("createTime", System.currentTimeMillis())
                                .fluentPut("other", new JSONObject().fluentPut("channel", "huawei").fluentPut("os", "huawei"))
                );
        post("log/account/push", push.toString());
    }

    @Test
    public void pushAccountList() throws Exception {
        String logToken = findLogToken();
        HashMap<Integer, List<JSONObject>> accountRecordMap = readAccount();
        List<Mono<Response<PostText>>> futures = new ArrayList<>();
        for (Map.Entry<Integer, List<JSONObject>> entry : accountRecordMap.entrySet()) {

            JSONObject push = new JSONObject()
                    .fluentPut("gameId", gameId)
                    .fluentPut("token", logToken)
                    .fluentPut("data", entry.getValue());

            Mono<Response<PostText>> post = post("log/account/pushList", push.toJSONString());
            futures.add(post);

            for (Mono<Response<PostText>> future : futures) {
                Response<PostText> join = future.block();
                RunResult runResult = join.bodyRunResult();
                if (join.responseCode() != 200 || runResult.code() != 1) {
                    System.out.println(join.bodyString());
                }
            }
        }
    }

}
