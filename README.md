# wxd-gaming-mariadb

#### 介绍

基于 wxdgaming.boot2 构建的 后台管理系统

#### 软件架构

| 模块       | 说明     |
|----------|--------|
| backends | 后台功能模块 |

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