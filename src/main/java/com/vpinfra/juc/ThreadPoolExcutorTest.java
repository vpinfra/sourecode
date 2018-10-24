package com.vpinfra.juc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

/**
 * ThreadPoolExcutor 线程池使用案例以及源码分析
 * 
 * @author <a href="mailto:yinjunfeng@vpgame.cn">Yinjf</a>
 * @date 2018年10月24日
 */
public class ThreadPoolExcutorTest {
	
	public static void main(String[] args) throws InterruptedException {
		validateSynchronousQueue();
		validateLinkedBlockingDeque();
		validateArrayBlockingQueue();
		validateAbortPolicy();
		validateCallerRunsPolicy();
		validateDiscardOldestPolicy();
		validateDiscardPolicy();
		validateThreadFactory();
    }
	
	/**
	 * SynchronousQueue 没有数量限制。并且不保持这些任务，而是直接交给线程池去执行。当任务数量超过最大线程数时会直接抛异常。
	 * 
	 * @throws InterruptedException
	 */
	private static void validateSynchronousQueue() throws InterruptedException {
		ThreadPoolExecutor executor = null;
		//线程数量>核心线程数，并且>最大线程数，会因为线程池拒绝添加任务而抛出异常。
		try{
			executor = new ThreadPoolExecutor(1,2,3,TimeUnit.SECONDS,new SynchronousQueue<>());
			validate(executor, "1个核心线程数，最大线程数为2，验证 SynchronousQueue 队列");
		}catch (RejectedExecutionException e) {
			System.out.println("线程池执行任务异常，并处于阻塞状态");
			executor.shutdown();
		}
		
		//线程数量>核心线程数，但<=最大线程数，线程池会创建新线程执行任务，这些任务也不会被放在任务队列中。这些线程属于非核心线程，在任务完成后，闲置时间达到了超时时间就会被清除。
		executor = new ThreadPoolExecutor(1,4,3,TimeUnit.SECONDS,new SynchronousQueue<>());
		validate(executor, "1个核心线程数，最大线程数为4，验证 SynchronousQueue 队列");
		executor.shutdown();
		
		//线程数量<=核心线程数量，那么直接启动一个核心线程来执行任务，不会放入队列中。
		executor = new ThreadPoolExecutor(5,6,3,TimeUnit.SECONDS,new SynchronousQueue<>());
		validate(executor, "5个核心线程数，最大线程数为6，验证 SynchronousQueue 队列");
		executor.shutdown();
	}
	
	/**
	 * <p>
	 * LinkedBlockingDeque 是一个无界缓存的等待队列，不指定容量则为Integer最大值，锁是分离的；
	 * </p>
	 * 
	 * @throws InterruptedException
	 */
	private static void validateLinkedBlockingDeque() throws InterruptedException {
		//线程数量>核心线程数，并且>最大线程数，超过核心线程的任务放在任务队列中排队。也就是当任务队列没有大小限制时，线程池的最大线程数设置是无效的，他的线程数最多不会超过核心线程数。
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1,2,3,TimeUnit.SECONDS,new LinkedBlockingDeque<>());
		validate(executor, "1个核心线程数，最大线程数为2，验证 LinkedBlockingDeque 队列");
		executor.shutdown();
		
		//线程数量>核心线程数，但<=最大线程数，超过核心线程数量的任务会放在任务队列中排队
		executor = new ThreadPoolExecutor(1,4,3,TimeUnit.SECONDS,new LinkedBlockingDeque<>(10));
		validate(executor, "1个核心线程数，最大线程数为4，验证 LinkedBlockingDeque 队列");
		executor.shutdown();
		
		//线程数量>核心线程数，但<=最大线程数 ,超过最大的队列数，新增的任务会直接创建新线程来执行，当创建的线程数量超过最大线程数量时会抛异常
		try{
			executor = new ThreadPoolExecutor(1,2,3,TimeUnit.SECONDS,new LinkedBlockingDeque<>(1));
			validate(executor, "1个核心线程数，最大线程数为4，验证 LinkedBlockingDeque 队列");
		}catch (RejectedExecutionException e) {
			System.out.println("线程池执行任务异常，创建的线程数大于最大线程数");
			executor.shutdown();
		}
	}
	
	/**
	 * <p>
	 * ArrayBlockingQueue 是一个有界缓存的等待队列，必须指定大小，锁是没有分离的；
	 * </p>
	 * 
	 * @throws InterruptedException
	 */
	private static void validateArrayBlockingQueue() throws InterruptedException {
		//线程数量>核心线程数，但<=最大线程数，超过核心线程数量的任务会放在任务队列中排队
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1,4,3,TimeUnit.SECONDS,new ArrayBlockingQueue<>(10));
		validate(executor, "1个核心线程数，最大线程数为4，验证 ArrayBlockingQueue 队列");
		executor.shutdown();
		
		//线程数量>核心线程数，但<=最大线程数 ,超过最大的队列数，新增的任务会直接创建新线程来执行，当创建的线程数量超过最大线程数量时会抛异常
		try{
			executor = new ThreadPoolExecutor(1,2,3,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1));
			validate(executor, "1个核心线程数，最大线程数为2，验证 ArrayBlockingQueue 队列");
		}catch (RejectedExecutionException e) {
			System.out.println("线程池执行任务异常，创建的线程数大于最大线程数");
			executor.shutdown();
		}
	}
	
	/**
	 * <p>
	 * AbortPolicy: 当任务添加到线程池中被拒绝时，它将抛出RejectedExecutionException 异常。
	 * </p>
	 * 
	 * @throws InterruptedException
	 */
	private static void validateAbortPolicy() throws InterruptedException {
		ThreadPoolExecutor executor = null;
		try{
			executor = new ThreadPoolExecutor(
				1, 
				2,
				3,
				TimeUnit.SECONDS,
				new LinkedBlockingDeque<>(1), 
				Executors.defaultThreadFactory(), 
				new ThreadPoolExecutor.AbortPolicy());
			validate(executor, "1个核心线程数，最大线程数为2，验证 AbortPolicy拒绝策略");
		}catch (RejectedExecutionException e) {
			System.out.println("线程池执行任务异常，创建的线程数大于最大线程数");
			executor.shutdown();
		}
	}
	
	/**
	 * <p>
	 * CallerRunsPolicy：当任务添加到线程池中被拒绝时，会在线程池当前正在运行的Thread线程池中处理被拒绝的任务。
	 * </p>
	 * 
	 * @throws InterruptedException
	 */
	private static void validateCallerRunsPolicy() throws InterruptedException {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				1,
				2,
				3,
				TimeUnit.SECONDS,
				new LinkedBlockingDeque<>(1), 
				Executors.defaultThreadFactory(), 
				new ThreadPoolExecutor.CallerRunsPolicy());
		validate(executor, "1个核心线程数，最大线程数为2，验证 CallerRunsPolicy拒绝策略");
		executor.shutdown();
	}
	
	/**
	 * <p>
	 * DiscardOldestPolicy：当任务添加到线程池中被拒绝时，线程池会放弃等待队列中最旧的未处理任务，然后将被拒绝的任务添加到等待队列中。
	 * </p>
	 * 
	 * @throws InterruptedException
	 */
	private static void validateDiscardOldestPolicy() throws InterruptedException {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				1,
				2,
				3,
				TimeUnit.SECONDS,new LinkedBlockingDeque<>(1), 
				Executors.defaultThreadFactory(), 
				new ThreadPoolExecutor.DiscardOldestPolicy());
		validate(executor, "1个核心线程数，最大线程数为2，验证 DiscardOldestPolicy拒绝策略");
		executor.shutdown();
	}
	
	/**
	 * <p>
	 * DiscardPolicy：当任务添加到线程池中被拒绝时，线程池将丢弃被拒绝的任务。
	 * </p>
	 * 
	 * @throws InterruptedException
	 */
	private static void validateDiscardPolicy() throws InterruptedException {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				1, 
				2, 
				3,
				TimeUnit.SECONDS,
				new LinkedBlockingDeque<>(1), 
				Executors.defaultThreadFactory(), 
				new ThreadPoolExecutor.DiscardPolicy());
		validate(executor, "1个核心线程数，最大线程数为2，验证 DiscardPolicy拒绝策略");
		executor.shutdown();
	}
	
	/**
	 * @throws InterruptedException
	 */
	private static void validateThreadFactory() throws InterruptedException {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				1,
				2,
				3,
				TimeUnit.SECONDS,
				new LinkedBlockingDeque<>(1), 
				new BasicThreadFactory.Builder().namingPattern("test-pool-%d").daemon(true).build(),
				new ThreadPoolExecutor.DiscardPolicy());
		validate(executor, "1个核心线程数，最大线程数为2，验证 自定义ThreadFactory");
		executor.shutdown();
	}
	
	/**
	 * 观察线程执行过程中的核心线程数、线程池数、队列任务数以及程序异常信息
	 * 
	 * @param executor 线程池执行器
	 * @param caseDesc 案例描述信息
	 * @throws InterruptedException
	 */
	private static void validate(ThreadPoolExecutor executor, String caseDesc) throws InterruptedException {
		System.out.println(String.format("==========准备开始验证案例：%s==========", caseDesc));
	    System.out.println("线程池数" + executor.getPoolSize());
	    
	    Task task = new Task();
	    System.out.println("---先执行两个任务---");
	    executor.execute(task);
	    executor.execute(task);
	    System.out.println("核心线程数" + executor.getCorePoolSize());
	    System.out.println("线程池数" + executor.getPoolSize());
	    System.out.println("队列任务数" + executor.getQueue().size());
	    
	    System.out.println("---再执行两个任务---");
	    executor.execute(task);
	    executor.execute(task);
	    System.out.println("核心线程数" + executor.getCorePoolSize());
	    System.out.println("线程池数" + executor.getPoolSize());
	    System.out.println("队列任务数" + executor.getQueue().size());
	    
	    Thread.sleep(5000);
	    System.out.println("----5秒之后----");
	    System.out.println("核心线程数" + executor.getCorePoolSize());
	    System.out.println("线程池数" + executor.getPoolSize());
	    System.out.println("队列任务数" + executor.getQueue().size());
	}

    static class Task implements Runnable{
    	@Override
		public void run() {
    		try {
				TimeUnit.MILLISECONDS.sleep(1000);
				System.out.println(Thread.currentThread().getName()+ " is running.");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
