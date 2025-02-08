package find;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import wxdgaming.backends.entity.logs.SLog;
import wxdgaming.boot.batis.DbConfig;
import wxdgaming.boot.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot.core.format.HexId;
import wxdgaming.boot.core.lang.RandomUtils;

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
        DbConfig dbConfig = new DbConfig()
                .setName("test")
                .setShow_sql(true)
                .setDbHost("192.168.137.10").setDbPort(5432)
                .setDbBase("game_db_1")
                .setDbUser("postgres")
                .setDbPwd("test")
                .setConnectionPool(true)
                .setBatchSizeThread(1)
                .setCreateDbBase(true);

        dataHelper = new PgsqlDataHelper(dbConfig);
        dataHelper.getBatchPool().setMaxCacheSize(100 * 10000);

    }

    @Test
    @RepeatedTest(5)
    public void selectCount() {
        long nanoTime = System.nanoTime();
        long count = dataHelper.rowCount("log_item", "");
        System.out.println((System.nanoTime() - nanoTime) / 10000 / 100f + " ms");
        System.out.println("select count=" + count);
    }

    @Test
    public void selectList() {
        long nanoTime = System.nanoTime();
        List<SLog> pgsqlLogTests = dataHelper.queryEntities(
                "select * from log_item where roleid = ? and json_extract_path_text(data,'itemId') = ?",
                SLog.class,
                String.valueOf(RandomUtils.random(1, 1000)),
                String.valueOf(RandomUtils.random(1, 100))
        );
        float v = (System.nanoTime() - nanoTime) / 10000 / 100f;
        pgsqlLogTests.forEach(System.out::println);
        System.out.println("查询耗时：" + v + " ms, select count=" + pgsqlLogTests.size());
    }

}
