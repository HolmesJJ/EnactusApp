package com.example.enactusapp.Thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by ChineseBoy on 2017/9/29.
 */

/**
 * 线程池初始化方法
 *
 * corePoolSize 核心线程池大小----1
 * maximumPoolSize 最大线程池大小----1
 * keepAliveTime 线程池中超过corePoolSize数目的空闲线程最大存活时间----0+单位TimeUnit
 * TimeUnit keepAliveTime时间单位----TimeUnit.MINUTES
 * workQueue 阻塞队列----new ArrayBlockingQueue<Runnable>(3)====3容量的阻塞队列
 * threadFactory 新建线程工厂----new CustomThreadFactory()====定制的线程工厂
 * rejectedExecutionHandler 当提交任务数超过maxmumPoolSize+workQueue之和时,
 *                          即当提交第4个任务时(前面线程都没有执行完,此测试方法中用sleep(100)),
 *                                任务会交给RejectedExecutionHandler来处理
 */

public class CustomThreadPool {

    private ExecutorService mExecutorService;

    public CustomThreadPool(int poolSize){
        mExecutorService = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(poolSize),
                new CustomThreadFactory(),
                new CustomRejectedExecutionHandler());
    }

    public CustomThreadPool(int poolSize, int priority){
        mExecutorService = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(poolSize),
                new CustomThreadFactory(priority),
                new CustomRejectedExecutionHandler());
    }

    public void execute(Runnable runnable){
        if(mExecutorService == null || mExecutorService.isShutdown()) return;
        mExecutorService.execute(runnable);
    }

    public boolean isShutdown(){
        if(mExecutorService == null || mExecutorService.isShutdown()) return true;
        return false;
    }

    public void release(){
        if(mExecutorService == null || isShutdown()) return;
        mExecutorService.shutdown();
    }
}
