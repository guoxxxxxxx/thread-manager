/**
 * @Time: 2025/4/15 9:43
 * @Author: guoxun
 * @File: ThreadInfo
 * @Description:
 */

package pojo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadInfo {

    /**
     * 线程本体
     */
    private Thread thread;

    /**
     * 线程唯一标识符
     */
    private String uuid;

    /**
     * 线程名称
     */
    private String name;

    /**
     * 线程描述
     */
    private String description;

    /**
     * 线程状态
     */
    private String status;

    /**
     * 线程加入队列时间
     */
    private Date appendTime;

    /**
     * 启动时间
     */
    private Date startTime;

    /**
     * 线程优先级 默认为5, 数字越大，优先级越高
     */
    private int rank;

    /**
     * 线程运行完毕的日志
     * @return 线程运行完毕的日志
     */
    public String log(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return "线程uuid: " + uuid + "结束状态: " + thread.getState() + " 线程名: " + name + " 优先级: " + rank + " 运行完毕, 创建时间: " + simpleDateFormat.format(appendTime) + " 开始时间: " +
                simpleDateFormat.format(startTime) + " 结束时间: " + simpleDateFormat.format(new Date()) + " 描述信息: " + description;
    }


    /**
     * 克隆一个线程信息 不含thread对象
     * @return 克隆后的线程信息
     */
    public ThreadInfo copy(){
        return ThreadInfo.builder()
                .name(name)
                .rank(rank)
                .uuid(uuid)
                .description(description)
                .appendTime(appendTime)
                .startTime(startTime)
                .status(status)
                .build();
    }
}
