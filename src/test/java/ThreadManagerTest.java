import lombok.extern.log4j.Log4j2;
import manager.ThreadManager;
import org.junit.Test;
import pojo.ThreadInfo;
import test.TestThread;

import java.util.List;

import static java.lang.Thread.sleep;

/**
 * @Time: 2025/4/15 9:52
 * @Author: guoxun
 * @File: ThreadTest
 * @Description:
 */


@Log4j2
public class ThreadManagerTest {

    private final ThreadManager threadManager = new ThreadManager();

    /**
     * 运行下述测试方法的前置方法
     */
    private void base(){
        for (int i=0; i<20; i++){
            threadManager.add(new TestThread("Thread " + i));
        }
        System.out.println("线程添加完毕");
    }


    /**
     * 添加测试
     * 测试状态: 通过
     * @throws InterruptedException
     */
    @Test
    public void addTest() throws InterruptedException {
        base();
        sleep(5000);
    }


    /**
     * 优先级测试
     * 测试状态: 通过
     * @throws InterruptedException
     */
    @Test
    public void rankTest() throws InterruptedException {
        for (int i=0; i<20; i++){
            threadManager.add(new TestThread("Thread " + i), i, "优先级测试", "");
        }
        sleep(1000);
        threadManager.add(new TestThread("高优先级测试"), 999, "", "");
        List<ThreadInfo> alreadyThreadList = threadManager.getAlreadyThreadList();
        String uuid = alreadyThreadList.get(threadManager.getAlreadyThreadList().size() - 1).getUuid();
        threadManager.changeThreadRank(uuid, 456);
        System.out.println("线程优先级修改完毕");
        System.out.println("线程添加完毕");
        sleep(5000);
    }


    /**
     * 获取线程信息测试 包含运行列表与就绪列表
     * 测试状态: 通过
     */
    @Test
    public void getThreadInfoTest() throws InterruptedException {
        base();
        int loop = 10;
        while(loop-- > 0){
            List<ThreadInfo> alreadyThreadList = threadManager.getAlreadyThreadList();
            System.out.println("size: " + alreadyThreadList.size() + " alreadyList: " + alreadyThreadList);
            List<ThreadInfo> runningThreadList = threadManager.getRunningThreadList();
            System.out.println("size: " + runningThreadList.size() + " runningList: " + runningThreadList);
            List<ThreadInfo> allThreadList = threadManager.getAllThreadList();
            System.out.println("size: " + allThreadList.size() + " allList: " + allThreadList);
            sleep(1000);
        }
    }


    /**
     * 删除/终止测试
     * 测试状态: 通过
     */
    @Test
    public void removeTest() throws InterruptedException {
        base();
        sleep(500);
        ThreadInfo alreadyThreadInfo = threadManager.getAlreadyThreadList().get(0);
        ThreadInfo runningThreadInfo = threadManager.getRunningThreadList().get(0);
        boolean remove = threadManager.remove(alreadyThreadInfo.getUuid());
        boolean remove1 = threadManager.remove(runningThreadInfo.getUuid());
        System.out.println("删除测试完成, 删除结果为: " + remove + "-" + remove1);
        sleep(1000);
    }


    /**
     * 启动/停止测试 此暂停功能仅是暂停调度器，不让新的线程加入到运行列表中，并无法对正在进行的线程进行控制
     * 测试状态: 通过
     * @throws InterruptedException
     */
    @Test
    public void startAndStopTest() throws InterruptedException {
        base();
        sleep(3000);
        threadManager.stopDispatcher();
        System.out.println("已暂停调度器");
        sleep(5000);
        threadManager.startDispatcher();
        System.out.println("已重启调度器");
        sleep(5000);
    }
}
