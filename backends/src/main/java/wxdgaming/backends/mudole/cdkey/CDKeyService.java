package wxdgaming.backends.mudole.cdkey;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.admin.game.GameService;
import wxdgaming.backends.entity.games.CDKeyEntity;
import wxdgaming.backends.entity.games.CDKeyRecord;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.CDKeyUtil;
import wxdgaming.boot2.core.util.ObjectLockUtil;
import wxdgaming.boot2.starter.batis.sql.SqlQueryBuilder;
import wxdgaming.boot2.starter.batis.sql.pgsql.PgsqlDataHelper;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * 服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-04-29 13:51
 **/
@Slf4j
@Singleton
@RequestMapping(path = "cdkey")
public class CDKeyService {

    final GameService gameService;

    @Inject
    public CDKeyService(GameService gameService) {
        this.gameService = gameService;
    }

    /** 生成cdkey */
    @HttpRequest(authority = 9)
    public RunResult add(@ThreadParam User user,
                         @Param(path = "gameId") int gameId,
                         @Param(path = "useType") int useType,
                         @Param(path = "useType") int useCount,
                         @Param(path = "rewards") String rewards) {

        List<CDKeyEntity.CDKeyReward> parseArray = FastJsonUtil.parseArray(rewards, CDKeyEntity.CDKeyReward.class);
        CDKeyEntity cdKeyEntity = new CDKeyEntity().setUseType(useType).setUseCount(useCount);
        cdKeyEntity.getRewards().addAll(parseArray);
        GameContext gameContext = gameService.gameContext(gameId);
        long l = gameContext.getDataHelper().tableCount(CDKeyEntity.class);
        cdKeyEntity.setUid((int) (l + 1));
        gameContext.getDataHelper().insert(cdKeyEntity);
        return RunResult.ok();
    }

    /** 获取所有的cdkey */
    @HttpRequest(authority = {1, 9})
    public RunResult list(HttpContext httpContext,
                          @ThreadParam GameContext gameContext,
                          @Param(path = "pageIndex") Integer pageIndex,
                          @Param(path = "pageSize") Integer pageSize) {
        PgsqlDataHelper dataHelper = gameContext.getDataHelper();
        SqlQueryBuilder sqlQueryBuilder = dataHelper.queryBuilder();
        sqlQueryBuilder.sqlByEntity(CDKeyEntity.class);
        sqlQueryBuilder.limit((pageIndex - 1) * pageSize, pageSize, 10, 1000);
        long rowCount = sqlQueryBuilder.findCount();
        List<CDKeyEntity> cdKeyEntities = sqlQueryBuilder.findList2Entity(CDKeyEntity.class);
        return RunResult.ok().fluentPut("rowCount", rowCount).data(cdKeyEntities);
    }

    /** 生成cdkey */
    @HttpRequest(authority = 9)
    public RunResult gain(HttpContext httpContext,
                          @ThreadParam GameContext gameContext,
                          @Param(path = "id") int id, @Param(path = "num") int num) {
        final String lockKey = "cdkey:" + id;
        ObjectLockUtil.lock(lockKey);
        try {
            /*TODO 需要记录数据库防止暴力破解*/
            PgsqlDataHelper dataHelper = gameContext.getDataHelper();
            List<String> strings = dataHelper.executeScalarList("select cdkey from cdkeyrecord where keyid=?", String.class, new Object[]{id});
            HashSet<String> hashSet = new HashSet<>(strings);
            List<String> resultList = new ArrayList<>(num);
            while (num > 0) {
                Collection<String> randoms = CDKeyUtil.cdKey(id, num);
                randoms.removeIf(v -> !hashSet.add(v));
                num -= randoms.size();
                for (String random : randoms) {
                    CDKeyRecord cdKeyRecord = new CDKeyRecord();
                    cdKeyRecord.setKeyId(id);
                    cdKeyRecord.setCdkey(random);
                    dataHelper.getDataBatch().insert(cdKeyRecord);
                    resultList.add(random);
                }
            }
            return RunResult.ok().data(resultList);
        } finally {
            ObjectLockUtil.unlock(lockKey);
        }
    }


    /** 检查 cdkey */
    @HttpRequest(authority = 1)
    public RunResult use(HttpContext httpContext,
                         @ThreadParam GameContext gameContext,
                         @Param(path = "key") String key,
                         @Param(path = "account") String account,
                         @Param(path = "rid") long rid) {
        if (StringUtils.isBlank(key)) {
            return RunResult.error("cdkey null");
        }
        final String lockKey = "cdkey:" + key;
        ObjectLockUtil.lock(lockKey);
        try {
            /*TODO 需要记录数据库防止暴力破解*/
            PgsqlDataHelper dataHelper = gameContext.getDataHelper();
            CDKeyEntity byWhere = dataHelper.findByWhere(CDKeyEntity.class, "cdkey = ?", key);
            if (byWhere != null) {
                /*通用激活码，比如6666 8888*/
                return RunResult.ok()
                        .fluentPut("cid", byWhere.getUid())
                        .fluentPut("comment", byWhere.getComment())
                        .fluentPut("rewards", byWhere.getRewards());
            }

            int cdKeyId = CDKeyUtil.getCdKeyId(key);
            CDKeyEntity cdKeyEntity = dataHelper.findByKey(CDKeyEntity.class, cdKeyId);
            if (cdKeyEntity == null) {
                return RunResult.error("cdkey 不存在");
            }
            CDKeyRecord cdKeyRecord = dataHelper.findByKey(CDKeyRecord.class, cdKeyId, key);
            if (cdKeyRecord == null) {
                return RunResult.error("cdkey 不存在");
            }
            if (cdKeyRecord.getUseCount() > cdKeyEntity.getUseCount()) {
                return RunResult.error("cdkey 不存在");
            }
            if (!cdKeyRecord.getAccountList().contains(account)) {
                cdKeyRecord.getAccountList().add(account);
            }
            if (!cdKeyRecord.getRidList().contains(rid)) {
                cdKeyRecord.getRidList().add(rid);
            }
            cdKeyRecord.setUseCount(cdKeyRecord.getUseCount() + 1);
            dataHelper.update(cdKeyRecord);
            return RunResult.ok()
                    .fluentPut("cid", cdKeyEntity.getUid())
                    .fluentPut("comment", cdKeyEntity.getComment())
                    .fluentPut("rewards", cdKeyEntity.getRewards());
        } finally {
            ObjectLockUtil.unlock(lockKey);
        }
    }

}
