# wxd-gaming-mariadb

#### 介绍

基于 wxdgaming.boot2 构建的 后台管理系统

#### 软件架构

| 模块       | 说明     |
|----------|--------|
| backends | 后台功能模块 |


#### push教程

```angular2html
在线调试：http://211.149.228.9:19000
本地调试：http://127.0.0.1:19000
logToken：3ugv7y8cP0Uk86gRYYm7I9AXLM36Rjz8
```

```angular2html
添加一个账号

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
```

``` push
    添加一个角色信息
    
    public void pushRole() {

        JSONObject record = new JSONObject();
        record.put("uid", 1);
        record.put("account", "xxxx");
        record.put("createTime", System.currentTimeMillis());
        record.put("createSid", RandomUtils.random(1, 100));
        record.put("curSid", RandomUtils.random(1, 100));
        record.put("roleName", StringUtils.randomString(8));
        record.put("Job", "魔剑士");
        record.put("sex", "男");
        record.put("lv", RandomUtils.random(1, 100));
        record.put("other", new JSONObject().fluentPut("channel", "huawei"));/*附加参数，本身也是一个json*/

        JSONObject push = new JSONObject()
                .fluentPut("gameId", gameId)
                .fluentPut("token", "logToken")
                .fluentPut("data", record);

        post("log/role/push", push.toJSONString());

    }
```

### 使用礼包码
```angular2html

        HashMap<String, Object> params = new HashMap<>();
        params.put("gameId", gameId);
        params.put("appToken", appToken);
        params.put("key", cdKey);
        params.put("account", account);
        params.put("rid", rid);

        PostText postText = HttpBuilder.postJson("http://www.backend.com/cdkey/use", FastJsonUtil.toJSONString(params));
        RunResult runResult = postText.request().bodyRunResult();

```

#### 预览

![image](/png/gamestat.png)

1. 日志落地功能
   ![image](/png/gamestat.png)
2. 日志查询功能
   ![image](/png/gamestat.png)
3. 日志系统采用数据库（PGSql）分表功能采用每天一张表来切割存储，提供存储和查询性能
4. 账号日志记录，账号的留存功能
   ![image](/png/account.png)
5. 充值订单记录，充值订单的留存功能
   ![image](/png/account.png)