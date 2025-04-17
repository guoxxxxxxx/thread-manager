/**
 * @Time: 2025/4/15 9:54
 * @Author: guoxun
 * @File: TestThread
 * @Description:
 */

package test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Data
public class TestThread extends Thread{

    private String threadName;

    public TestThread(String threadName){
        this.threadName = threadName;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                System.out.println("线程被中断退出！");
            }
            System.out.println(this.threadName + " is Running..., time is " + i);
        }
    }
}
