package push;

import org.junit.Test;
import wxdgaming.backends.entity.logs.RoleRecord;
import wxdgaming.boot.core.lang.RandomUtils;

/**
 * 角色api操作
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-24 11:15
 **/
public class RoleApiTest extends AccountApiTest {

    protected String roleName = "无心道";

    @Test
    public void test() {
        for (int i = 1; i <= 1000; i++) {
            RoleRecord roleRecord = new RoleRecord();
            roleRecord.setGameId(gameId);
            roleRecord.setToken(appToken);
            roleRecord.setAccount(account);
            roleRecord.setCreateSid(RandomUtils.random(1, 100));
            roleRecord.setCurSid(RandomUtils.random(1, 100));
            roleRecord.setCreateTime(System.currentTimeMillis());
            roleRecord.setRoleId(String.valueOf(i));
            roleRecord.setRoleName(roleName);
            roleRecord.setJob("魔剑士");
            roleRecord.setSex("男");
            roleRecord.setLv(RandomUtils.random(1, 100));
            roleRecord.getData().fluentPut("channel", "huawei");
            push("role/push", roleRecord);
        }
    }


}
