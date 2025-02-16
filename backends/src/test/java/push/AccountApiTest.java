package push;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.logs.AccountRecord;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.util.RandomUtils;

/**
 * test
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 14:02
 **/
@Slf4j
public class AccountApiTest extends GameApiTest {

    protected String account = "wxd-gaming";

    @Test
    public void pushAccount() {
        AccountRecord record = new AccountRecord();
        record.setGameId(gameId);
        record.setToken(appToken);
        record.setAccount(account);
        record.setLastJoinSid(RandomUtils.random(1, 100));
        record.setLastJoinTime(System.currentTimeMillis());
        record.getData().fluentPut("channel", "huawei").fluentPut("os", "huawei");
        push("account/push", record);
    }

    @Test
    @RepeatedTest(100)
    public void pushAccountList() {
        account = StringUtils.randomString(18);
        pushAccount();
    }

}
