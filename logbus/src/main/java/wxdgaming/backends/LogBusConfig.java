package wxdgaming.backends;

import lombok.Getter;
import lombok.Setter;

/**
 * 日志载体配置
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-11 20:20
 **/
@Getter
@Setter
public class LogBusConfig {

    private int appId;
    private String appToken;
    private String logToken;
    private String portUrl;
    private boolean batch = true;
    private int batchSize = 100;

}
