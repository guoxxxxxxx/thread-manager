# 线程任务调度SDK


## 1. 整体实现图解
![](https://cdn.nlark.com/yuque/0/2025/png/29410528/1744869277203-4cde5509-e3d6-496a-b253-f3beae9bdbe5.png)



## 2. 方法目录
### 2.1 构造方法
**构造方法1**

方法名：`ThreadManager()`

说明：无参构造方法

参数：无

返回值：无

注：调用该方法将会采用默认配置，默认配置如下：

1. 最大支持并发线程数为5
2. 哨兵线程轮询查询时间为5
3. 线程最大运行时间3600秒（1小时）
4. 就绪线程列表最大容量100.



**构造方法2**

方法名：`ThreadManager(int maxConcurrentThreads, int pollingTime, long maxRunningTime, int maxAlreadyThreadCounts)`

说明：有参构造方法

参数：

| **参数名** | **类型** | **说明** |
| --- | --- | --- |
| maxConcurrentThreads | int | 支持最大并发线程数 |
| pollingTime | int | 哨兵线程轮询查询时间 |
| maxRunningTime | int | 线程最大运行时间 |
| maxAlreadyThreadCounts | int | 就绪列表最大线程数量 |


返回值：无



### 2.2 添加线程至线程管理器中
**方法1**

方法名：`String add(Thread thread)`

说明：添加线程至就绪列表中

参数：

| **参数名** | **类型** | **说明** |
| --- | --- | --- |
| thread | Thread | 线程对象 |


返回值：线程的唯一标识符

注：采用上述方法，优先级将被设置为默认值5，线程名称与描述都将设置为None



**方法2**

方法名：`String add(Thread thread, int rank, String threadName, String description)`

说明：添加线程至就绪列表中

参数：

| **参数名** | **类型** | **说明** |
| --- | --- | --- |
| thread | Thread | 线程对象 |
| rank | int | 线程优先级（等级越高的越先被调度） |
| threadName | String | 线程名称 |
| description | String | 线程描述 |


返回值：线程的唯一标识符



### 2.3 获取所有待执行线程的详细信息
方法名：`List<ThreadInfo> getAlreadyThreadList()`

说明：获取所有待执行线程的详细信息

参数：无

返回值：List<ThreadInfo> 待执行线程详细信息, 不含Thread对象！



### 2.4 获取当前正在运行的线程的详细信息
方法名：`List<ThreadInfo> getRunningThreadList()`

说明：获取所有正在运行线程的详细信息

参数：无

返回值：List<ThreadInfo> 正在运行线程详细信息, 不含Thread对象！



### 2.5 获取所有线程详细信息
方法名：`List<ThreadInfo> getAllThreadList()`

说明：获取所有线程的详细信息

参数：无

返回值：List<ThreadInfo> 所有线程详细信息, 不含Thread对象！



### 2.6 修改线程优先级
方法名：`boolean changeThreadRank(String threadUUID, int newRank)`

说明：修改线程优先级

参数：

| **参数名** | **类型** | **说明** |
| --- | --- | --- |
| threadUUID | String | 线程的唯一标识符，自动生成，可以通过获取线程详细信息方法进行查询 |
| newRank | int | 新的优先级 |


返回值：true 修改成功， false修改失败



### 2.7 删除/终止线程
方法名：`boolean remove(String threadUUID)`

说明：终止线程并删除线程

参数：

| **参数名** | **类型** | **说明** |
| --- | --- | --- |
| threadUUID | String | 线程的唯一标识符，自动生成，可以通过获取线程详细信息方法进行查询 |


返回值：true 成功， false 失败



### 2.8 停止任务调度器
方法名：`void stopDispatcher()`

说明：停止任务调度器

参数：无

返回值：true 成功， false 失败



### 2.9 启动任务调度器
方法名：`void startDispatcher()`

说明：启动任务调度器

参数：无

返回值：true 成功， false 失败

