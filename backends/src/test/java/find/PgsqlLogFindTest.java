package find;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.games.logs.SLog;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.starter.batis.sql.SqlConfig;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;

import java.util.List;

/**
 * cd
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-01-18 20:23
 **/
public class PgsqlLogFindTest {

    static HexId hexId = new HexId(1);
    static PgsqlDataHelper dataHelper;

    @Before
    @BeforeEach
    public void beforeClass() {
        if (dataHelper != null) return;
        SqlConfig sqlConfig = new SqlConfig();
        sqlConfig.setDebug(true);
        sqlConfig.setDriverClassName("org.postgresql.Driver");
        sqlConfig.setUrl("jdbc:postgresql://192.168.137.10:5432/test2");
        sqlConfig.setUsername("postgres");
        sqlConfig.setPassword("test");

        dataHelper = new PgsqlDataHelper(sqlConfig);

    }

    @Test
    @RepeatedTest(5)
    public void selectCount() {
        long nanoTime = System.nanoTime();
        long count = dataHelper.tableCount("log_item");
        System.out.println((System.nanoTime() - nanoTime) / 10000 / 100f + " ms");
        System.out.println("select count=" + count);
    }

    @Test
    public void selectList() {
        long nanoTime = System.nanoTime();
        List<SLog> pgsqlLogTests = dataHelper.findListBySql(
                SLog.class,
                "select * from log_item where roleid = ? and json_extract_path_text(data,'itemId') = ?",
                String.valueOf(RandomUtils.random(1, 1000)),
                String.valueOf(RandomUtils.random(1, 100))
        );
        float v = (System.nanoTime() - nanoTime) / 10000 / 100f;
        pgsqlLogTests.forEach(System.out::println);
        System.out.println("查询耗时：" + v + " ms, select count=" + pgsqlLogTests.size());
    }

}
