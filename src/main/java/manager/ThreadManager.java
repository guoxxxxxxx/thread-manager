/**
 * @Time: 2025/4/15 9:34
 * @Author: guoxun
 * @File: ThreadManager
 * @Description: 线程管理类
 */

package manager;

import lombok.extern.log4j.Log4j2;
import pojo.ThreadInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;


@Log4j2
public class ThreadManager{

    // 存储就绪态线程的信息
    private final List<ThreadInfo> alreadyThreadList = new ArrayList<>();
    // 运行态的线程信息
    private final List<ThreadInfo> runningThreadList = new ArrayList<>();
    // 定义锁, 确保上述列表在操作时线程安全
    private final ReentrantLock lock = new ReentrantLock();
    // 任务调度器
    private final Thread dispatcher;
    // 线程运行状态哨兵 用于监视运行线程状态信息, 并在线程运行结束后释放资源
    private final Thread watcher;
    // 任务调度器控制器
    private boolean isRunning = true;
    private final Lock runningLock = new ReentrantLock();
    private final Condition runningCondition = runningLock.newCondition();
    // 最大同时执行线程数 默认为5
    private final Semaphore availableThreadsSemaphore;


    /**
     * 默认构造函数
     * 支持最大并发线程数为 5
     * 哨兵轮询查询时间间隔为 500 毫秒
     */
    public ThreadManager(){
        this.availableThreadsSemaphore = new Semaphore(5);

        // 构造任务调度线程并启动
        this.dispatcher = constructDispatcher(5);
        this.dispatcher.start();

        // 构造线程运行状态哨兵并启动
        this.watcher = constructWatcher(500);
        this.watcher.start();
    }


    /**
     * 构造函数
     * @param maxConcurrentThreads 最大并发线程数
     * @param pollingTime 轮询时间
     */
    public ThreadManager(int maxConcurrentThreads, int pollingTime){
        this.availableThreadsSemaphore = new Semaphore(maxConcurrentThreads);

        // 构造任务调度线程并启动
        this.dispatcher = constructDispatcher(maxConcurrentThreads);
        this.dispatcher.start();

        // 构造线程运行状态哨兵并启动
        this.watcher = constructWatcher(pollingTime);
        this.watcher.start();
    }


    /**
     * 构造任务调度器线程
     * @param maxConcurrentThreads 最大并发线程数
     * @return 任务调度线程
     */
    private Thread constructDispatcher(int maxConcurrentThreads){
        // 构造任务调度器线程
        return new Thread(() -> {
            while (true){
                // 检查当前任务调度器是否启动, 若未启动则进入阻塞等待
                while (!isRunning){
                    runningLock.lock();
                    try {
                        runningCondition.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        runningLock.unlock();
                    }
                }

                // 获取空余槽位资源
                try {
                    availableThreadsSemaphore.acquire();
                    // 检查就绪队列是否有待执行的线程
                    lock.lock();
                    boolean needSleep = false;
                    try {
                        if (!alreadyThreadList.isEmpty()){
                            // 获取并删除在列表中的第一个位置的线程
                            ThreadInfo firstThreadInfo = alreadyThreadList.remove(0);
                            // 启动该线程并修改其运行状态、启动时间; 并将其添加至运行列表中
                            if (firstThreadInfo.getThread().getState() == Thread.State.NEW)
                                firstThreadInfo.getThread().start();
                            else {
                                log.debug("线程: " + firstThreadInfo.getUuid() + " 已经运行过!");
                            }
                            firstThreadInfo.setStatus("RUNNING");
                            firstThreadInfo.setStartTime(new Date());
                            runningThreadList.add(firstThreadInfo);
                        }
                        else {
                            needSleep = true;
                        }
                    } finally {
                        lock.unlock();
                    }
                    if (needSleep){
                        // 如果就绪队列为空, 则踏步等待3秒, 随后释放semaphore资源, 进入下一轮循环
                        sleep(3000);
                        availableThreadsSemaphore.release();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    /**
     * 构造线程运行状态哨兵线程
     * @param pollingTime 轮询时间
     * @return 线程运行状态哨兵线程
     */
    private Thread constructWatcher(int pollingTime){
        return new Thread(() -> {
            while (true){
                lock.lock();
                try {
                    List<ThreadInfo> removeList = new ArrayList<>();
                    for (ThreadInfo e : runningThreadList){
                        // 检查当前线程是否运行结束
                        if (e != null && !e.getThread().isAlive()){
                            log.debug(e.log());
                            removeList.add(e);
                        }
                    }
                    if (!removeList.isEmpty()) {
                        // 从运行列表中删除该线程
                        runningThreadList.removeAll(removeList);
                        // 信号量+1
                        availableThreadsSemaphore.release(removeList.size());
                    }
                } finally {
                    lock.unlock();
                }
                // 轮询一次之后等待一段时间再次进行下次轮询
                try {
                    sleep(pollingTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    /**
     * 添加线程
     * @param thread 线程实例
     * @param threadName 线程名
     * @param description 线程描述信息
     * @return 添加成功返回true，否则返回false
     */
    public String add(Thread thread, int rank, String threadName, String description){
        lock.lock();
        String uuid = UUID.randomUUID().toString();
        try {
            // 检测当前线程对象是否存在于线程队列中
            if (checkThreadIsExist(thread)) {
                log.info("当前线程已经存在于线程队列中, 无需再次添加!");
                return null;
            }
            // 创建线程详细信息对象
            ThreadInfo threadInfo = ThreadInfo.builder()
                    .uuid(uuid)
                    .name(threadName)
                    .status("ALREADY")
                    .appendTime(new Date())
                    .thread(thread)
                    .description(description)
                    .rank(rank)
                    .build();
            // 根据线程优先级将线程插入到就绪列表中
            insert(threadInfo);
        } finally {
            lock.unlock();
        }
        return uuid;
    }


    /**
     * 添加线程至队列中
     * @param thread 线程实体
     * @return 添加成功返回true，否则返回false
     */
    public String add(Thread thread){
        return add(thread, 5, "None", "None");
    }


    /**
     * 根据线程UUID获取就绪线程详细信息
     * @param threadUUID 线程UUID
     * @return 线程详细信息，若不存在则返回null
     */
    private ThreadInfo getThreadInfoInAlreadyList(String threadUUID){
        // 遍历就绪态线程列表取得uuid为threadUUID的线程对象
        for (ThreadInfo threadInfo : alreadyThreadList) {
            if (threadInfo.getUuid().equals(threadUUID)) {
                return threadInfo;
            }
        }
        return null;
    }


    private ThreadInfo getThreadInfoInRunningList(String threadUUID){
        // 遍历运行态线程列表取得uuid为threadUUID的线程对象
        for (ThreadInfo threadInfo : runningThreadList){
            if (threadInfo.getUuid().equals(threadUUID)){
                return threadInfo;
            }
        }
        return null;
    }


    /**
     * 根据线程对象实例检测当前线程是否存在在队列中
     * @param thread 线程对象实例
     * @return 存在返回true，否则返回false
     */
    private boolean checkThreadIsExist(Thread thread){
        // 检测当前线程对象是否在就绪态队列中存在
        for (ThreadInfo threadInfo : alreadyThreadList) {
            if (threadInfo.getThread() == thread) {
                return true;
            }
        }

        // 检查当前线程对象是否在运行态队列存在
        for (ThreadInfo threadInfo : runningThreadList) {
            if (threadInfo.getThread() == thread) {
                return true;
            }
        }
        return false;
    }


    /**
     * 根据线程UUID检测当前线程是否存在在队列中
     * @param threadUUID 线程UUID
     * @return 存在返回true，否则返回false
     */
    private boolean checkThreadIsExist(String threadUUID){
        // 检测当前线程对象是否在就绪态队列中存在
        for (ThreadInfo threadInfo : alreadyThreadList) {
            if (threadInfo.getUuid().equals(threadUUID)) {
                return true;
            }
        }

        // 检查当前线程对象是否在运行态队列存在
        for (ThreadInfo threadInfo : runningThreadList) {
            if (threadInfo.getUuid().equals(threadUUID)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 根据优先级降序原则向就绪队列中插入线程信息
     * @param threadInfo 线程信息
     * @return 插入成功返回true，否则返回false
     */
    private boolean insert(ThreadInfo threadInfo){
        int insertIndex = 0;
        for (int i=alreadyThreadList.size()-1; i>=0; i--){
            if (alreadyThreadList.get(i).getRank() >= threadInfo.getRank()){
                insertIndex = i + 1;
                break;
            }
        }
        alreadyThreadList.add(insertIndex, threadInfo);
        return true;
    }


    /**
     * 获取当前就绪队列中的线程信息 此方法不返回thread对象信息
     * @return 当前就绪队列中的线程信息
     */
    public List<ThreadInfo> getAlreadyThreadList(){
        lock.lock();
        List<ThreadInfo> result = new ArrayList<>();
        try {
            for(ThreadInfo e : alreadyThreadList){
                result.add(e.copy());
            }
        } finally {
            lock.unlock();
        }
        return result;
    }


    /**
     * 获取当前运行队列中的线程信息 此方法不返回thread对象信息
     * @return 当前运行的线程信息
     */
    public List<ThreadInfo> getRunningThreadList(){
        lock.lock();
        List<ThreadInfo> result = new ArrayList<>();
        try {
            for (ThreadInfo e : runningThreadList){
                result.add(e.copy());
            }
        } finally {
            lock.unlock();
        }
        return result;
    }


    /**
     * 获取当前所有线程信息 此方法不返回thread对象信息
     * @return 当前所有线程信息
     */
    public List<ThreadInfo> getAllThreadList(){
        List<ThreadInfo> result = new ArrayList<>();
        result.addAll(getAlreadyThreadList());
        result.addAll(getRunningThreadList());
        return result;
    }


    /**
     * 修改线程优先级
     * @param threadUUID 线程UUID
     * @param newRank 新的优先级
     * @return 修改成功返回true，否则返回false
     */
    public boolean changeThreadRank(String threadUUID, int newRank){
        lock.lock();
        try {
            ThreadInfo threadInfo = getThreadInfoInAlreadyList(threadUUID);
            if (threadInfo != null){
                alreadyThreadList.remove(threadInfo);
                threadInfo.setRank(newRank);
                insert(threadInfo);
                return true;
            }
            else {
                log.error("当前线程uuid在就绪线程中不存在, 无法完成线程的修改! ");
                return false;
            }
        } finally {
            lock.unlock();
        }
    }


    /**
     * 删除/终止线程
     * @param threadUUID 线程UUID
     * @return 删除成功返回true，否则返回false
     */
    public boolean remove(String threadUUID){
        lock.lock();
        try {
            ThreadInfo threadInfo = getThreadInfoInAlreadyList(threadUUID);
            if (threadInfo != null){
                alreadyThreadList.remove(threadInfo);
                return true;
            }
            else {
                // 线程不在就绪态列表中，则尝试从运行态列表中删除
                return interruptAndRemove(threadUUID);
            }
        } finally {
            lock.unlock();
        }
    }


    /**
     * 删除运行列表中的线程
     * @param threadUUID 线程UUID
     * @return 删除成功返回true，否则返回false
     */
    private boolean interruptAndRemove(String threadUUID){
        lock.lock();
        try {
            ThreadInfo threadInfo = getThreadInfoInRunningList(threadUUID);
            if (threadInfo != null){
                threadInfo.getThread().interrupt();
                runningThreadList.remove(threadInfo);
                availableThreadsSemaphore.release();
                log.debug("线程: " + threadUUID + " 已被移除!");
                return true;
            }
            else {
                log.warn("当uuid对应线程信息在列表中不存在!");
            }
        } finally {
            lock.unlock();
        }
        return false;
    }


    /**
     * 停止调度器
     */
    public void stopDispatcher(){
        runningLock.lock();
        try {
            this.isRunning = false;
        } finally {
            runningLock.unlock();
        }
    }


    /**
     * 启动调度器
     */
    public void startDispatcher(){
        runningLock.lock();
        try {
            this.isRunning = true;
            this.runningCondition.signal();
        } finally {
            runningLock.unlock();
        }
    }
}
