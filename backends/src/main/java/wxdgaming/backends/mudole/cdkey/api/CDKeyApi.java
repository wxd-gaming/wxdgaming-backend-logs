package wxdgaming.backends.mudole.cdkey.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.backends.admin.game.GameContext;
import wxdgaming.backends.entity.games.CDKeyEntity;
import wxdgaming.backends.entity.games.CDKeyRecord;
import wxdgaming.backends.entity.system.User;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.ann.ThreadParam;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.CDKeyUtil;
import wxdgaming.boot2.core.util.SingletonLockUtil;
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
 * html接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-05-18 10:16
 **/
@Slf4j
@Singleton
@RequestMapping(path = "cdkey")
public class CDKeyApi {

    /** 生成cdkey */
    @HttpRequest(authority = 9)
    public RunResult add(@ThreadParam User user,
                         @ThreadParam GameContext gameContext,
                         @Param(path = "comment") String comment,
                         @Param(path = "cdkey", defaultValue = "") String cdkey,
                         @Param(path = "rewards") String rewards) {

        List<CDKeyEntity.CDKeyReward> parseArray = new ArrayList<>();

        String[] split = rewards.split(",");
        for (String s : split) {
            String[] split1 = s.split(":");
            int itemId = 0;
            if ("金币".equals(split1[0])) {
                itemId = 1;
            }
            CDKeyEntity.CDKeyReward cdKeyReward = new CDKeyEntity.CDKeyReward().setItemId(itemId).setCount(Long.parseLong(split1[1]));
            parseArray.add(cdKeyReward);
        }

        CDKeyEntity cdKeyEntity = new CDKeyEntity().setUseType(1).setUseCount(1).setCdKey(cdkey).setComment(comment);
        cdKeyEntity.getRewards().addAll(parseArray);
        long l = gameContext.getDataHelper().tableCount(CDKeyEntity.class);
        cdKeyEntity.setUid((int) (l + 1));
        gameContext.getDataHelper().insert(cdKeyEntity);
        return RunResult.ok();
    }

    /** 获取所有的cdkey */
    @HttpRequest(authority = {9})
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
        List<JSONObject> list = new ArrayList<>();
        for (CDKeyEntity cdKeyEntity : cdKeyEntities) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("uid", cdKeyEntity.getUid());
            jsonObject.put("cdkey", cdKeyEntity.getCdKey());
            jsonObject.put("comment", cdKeyEntity.getComment());
            jsonObject.put("rewards", "金币*50");
            list.add(jsonObject);
        }
        return RunResult.ok().fluentPut("rowCount", rowCount).data(list);
    }

    /** 生成cdkey */
    @HttpRequest(authority = 9)
    public RunResult gain(HttpContext httpContext,
                          @ThreadParam GameContext gameContext,
                          @Param(path = "id") int id, @Param(path = "num") int num) {
        final String lockKey = "cdkey:" + id;
        SingletonLockUtil.lock(lockKey);
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
            SingletonLockUtil.unlock(lockKey);
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
            return RunResult.fail("cdkey null");
        }
        final String lockKey = "cdkey:" + key;
        SingletonLockUtil.lock(lockKey);
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
                return RunResult.fail("cdkey 不存在");
            }
            CDKeyRecord cdKeyRecord = dataHelper.findByKey(CDKeyRecord.class, cdKeyId, key);
            if (cdKeyRecord == null) {
                return RunResult.fail("cdkey 不存在");
            }
            if (cdKeyRecord.getUseCount() > cdKeyEntity.getUseCount()) {
                return RunResult.fail("cdkey 不存在");
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
            SingletonLockUtil.unlock(lockKey);
        }
    }

}
