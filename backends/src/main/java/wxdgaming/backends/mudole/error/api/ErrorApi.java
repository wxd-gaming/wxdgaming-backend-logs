package wxdgaming.backends.mudole.error.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.ErrorRecord;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;

import java.util.LinkedList;
import java.util.List;

/**
 * 错误接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-23 18:51
 **/
@Slf4j
@Singleton
@RequestMapping(path = "/log/error")
public class ErrorApi {

    final GameService gameService;

    @Inject
    public ErrorApi(GameService gameService) {
        this.gameService = gameService;
    }

    @HttpRequest()
    public RunResult list(
            @Param(path = "gameId") int gameId,
            @Param(path = "pageIndex") int pageIndex,
            @Param(path = "pageSize") int pageSize
    ) {
        GameContext gameContext = gameService.gameContext(gameId);

        gameContext.getErrorReentrantLock().lock();

        if (pageIndex < 1) pageIndex = 1;
        if (pageSize < 10) pageSize = 10;
        if (pageSize > 1000) pageSize = 1000;

        try {
            LinkedList<ErrorRecord> errorRecordList = gameContext.getErrorRecordList();
            int skip = (pageIndex - 1) * pageSize;
            if (skip > errorRecordList.size()) {
                skip = errorRecordList.size() - pageSize;
            }
            List<JSONObject> list = errorRecordList.stream()
                    .skip(skip)
                    .map(errorRecord -> {
                        JSONObject jsonObject = errorRecord.toJSONObject();
                        jsonObject.put("createTime", MyClock.formatDate("yyyy-MM-dd HH:mm:ss", errorRecord.getCreateTime()));
                        return jsonObject;
                    })
                    .limit(pageSize)
                    .toList();
            return RunResult.ok().data(list).fluentPut("rowCount", errorRecordList.size());
        } finally {
            gameContext.getErrorReentrantLock().unlock();
        }
    }

}
