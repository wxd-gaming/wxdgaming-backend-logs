package code;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 累计测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-10 15:37
 **/
public class IncrementTest {

    @Test
    public void t0() {
        AtomicInteger total = new AtomicInteger(Integer.MAX_VALUE);
        System.out.println(total.get());
        total.incrementAndGet();
        System.out.println(total.get());
    }
}
