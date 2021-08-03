# 以两种异步模型应用案例，深度解析 Future 接口

本文以实际案例的形式分析了两种异步模型，并从源码角度深度解析 Future 接口和 FutureTask 类，希望大家踏下心来，打开你的 IDE，跟着文章看源码，相信你一定收获不小！

## 一、两种异步模型



在 Java 的并发编程中，大体上会分为两种异步编程模型，一类是直接以异步的形式来并行运行其他的任务，不需要返回任务的结果数据。一类是以异步的形式运行其他任务，需要返回结果。



**1.无返回结果的异步模型**



无返回结果的异步任务，可以直接将任务丢进线程或线程池中运行，此时，无法直接获得任务的执行结果数据，一种方式是可以使用回调方法来获取任务的运行结果。



具体的方案是：定义一个回调接口，并在接口中定义接收任务结果数据的方法，具体逻辑在回调接口的实现类中完成。将回调接口与任务参数一同放进线程或线程池中运行，任务运行后调用接口方法，执行回调接口实现类中的逻辑来处理结果数据。这里，给出一个简单的示例供参考。



- 定义回调接口



```
package io.binghe.concurrent.lab04;
/** * @author binghe * @version 1.0.0 * @description 定义回调接口 */public interface TaskCallable<T> {    T callable(T t);}
```

复制代码



便于接口的通用型，这里为回调接口定义了泛型。



- 定义任务结果数据的封装类



```
package io.binghe.concurrent.lab04;
import java.io.Serializable;
/** * @author binghe * @version 1.0.0 * @description 任务执行结果 */public class TaskResult implements Serializable {    private static final long serialVersionUID = 8678277072402730062L;    /**     * 任务状态     */    private Integer taskStatus;
    /**     * 任务消息     */    private String taskMessage;
    /**     * 任务结果数据     */    private String taskResult;		//省略getter和setter方法	@Override    public String toString() {        return "TaskResult{" +                "taskStatus=" + taskStatus +                ", taskMessage='" + taskMessage + '\'' +                ", taskResult='" + taskResult + '\'' +                '}';    }}
```

复制代码



- 创建回调接口的实现类



回调接口的实现类主要用来对任务的返回结果进行相应的业务处理，这里，为了方便演示，只是将结果数据返回。大家需要根据具体的业务场景来做相应的分析和处理。



```
package io.binghe.concurrent.lab04;
/** * @author binghe * @version 1.0.0 * @description 回调函数的实现类 */public class TaskHandler implements TaskCallable<TaskResult> {    @Overridepublic TaskResult callable(TaskResult taskResult) {//TODO 拿到结果数据后进一步处理    System.out.println(taskResult.toString());        return taskResult;    }}
```

复制代码



- 创建任务的执行类



任务的执行类是具体执行任务的类，实现 Runnable 接口，在此类中定义一个回调接口类型的成员变量和一个 String 类型的任务参数（模拟任务的参数），并在构造方法中注入回调接口和任务参数。在 run 方法中执行任务，任务完成后将任务的结果数据封装成 TaskResult 对象，调用回调接口的方法将 TaskResult 对象传递到回调方法中。



```
package io.binghe.concurrent.lab04;
/** * @author binghe * @version 1.0.0 * @description 任务执行类 */public class TaskExecutor implements Runnable{    private TaskCallable<TaskResult> taskCallable;    private String taskParameter;
    public TaskExecutor(TaskCallable<TaskResult> taskCallable, String taskParameter){        this.taskCallable = taskCallable;        this.taskParameter = taskParameter;    }
    @Override    public void run() {        //TODO 一系列业务逻辑,将结果数据封装成TaskResult对象并返回        TaskResult result = new TaskResult();        result.setTaskStatus(1);        result.setTaskMessage(this.taskParameter);        result.setTaskResult("异步回调成功");        taskCallable.callable(result);    }}
```

复制代码



到这里，整个大的框架算是完成了，接下来，就是测试看能否获取到异步任务的结果了。



- 异步任务测试类



```
package io.binghe.concurrent.lab04;
/** * @author binghe * @version 1.0.0 * @description 测试回调 */public class TaskCallableTest {    public static void main(String[] args){        TaskCallable<TaskResult> taskCallable = new TaskHandler();        TaskExecutor taskExecutor = new TaskExecutor(taskCallable, "测试回调任务");        new Thread(taskExecutor).start();    }}
```

复制代码



在测试类中，使用 Thread 类创建一个新的线程，并启动线程运行任务。运行程序最终的接口数据如下所示。



```
TaskResult{taskStatus=1, taskMessage='测试回调任务', taskResult='异步回调成功'}
```

复制代码



大家可以细细品味下这种获取异步结果的方式。这里，只是简单的使用了 Thread 类来创建并启动线程，也可以使用线程池的方式实现。大家可自行实现以线程池的方式通过回调接口获取异步结果。



**2.有返回结果的异步模型**



尽管使用回调接口能够获取异步任务的结果，但是这种方式使用起来略显复杂。在 JDK 中提供了可以直接返回异步结果的处理方案。最常用的就是使用 Future 接口或者其实现类 FutureTask 来接收任务的返回结果。



- 使用 Future 接口获取异步结果



使用 Future 接口往往配合线程池来获取异步执行结果，如下所示。



```
package io.binghe.concurrent.lab04;
import java.util.concurrent.*;
/** * @author binghe * @version 1.0.0 * @description 测试Future获取异步结果 */public class FutureTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {        ExecutorService executorService = Executors.newSingleThreadExecutor();        Future<String> future = executorService.submit(new Callable<String>() {            @Override            public String call() throws Exception {                return "测试Future获取异步结果";            }        });        System.out.println(future.get());        executorService.shutdown();    }}
```

复制代码



运行结果如下所示。



```
测试Future获取异步结果
```

复制代码



- 使用 FutureTask 类获取异步结果



FutureTask 类既可以结合 Thread 类使用也可以结合线程池使用，接下来，就看下这两种使用方式。

结合 Thread 类的使用示例如下所示。



```
package io.binghe.concurrent.lab04;
import java.util.concurrent.*;
/** * @author binghe * @version 1.0.0 * @description 测试FutureTask获取异步结果 */public class FutureTaskTest {
    public static void main(String[] args)throws ExecutionException, InterruptedException{        FutureTask<String> futureTask = new FutureTask<>(new Callable<String>() {            @Override            public String call() throws Exception {                return "测试FutureTask获取异步结果";            }        });        new Thread(futureTask).start();        System.out.println(futureTask.get());    }}
```

复制代码



运行结果如下所示。



```
测试FutureTask获取异步结果
```

复制代码



结合线程池的使用示例如下。



```
package io.binghe.concurrent.lab04;
import java.util.concurrent.*;
/** * @author binghe * @version 1.0.0 * @description 测试FutureTask获取异步结果 */public class FutureTaskTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {        ExecutorService executorService = Executors.newSingleThreadExecutor();        FutureTask<String> futureTask = new FutureTask<>(new Callable<String>() {            @Override            public String call() throws Exception {                return "测试FutureTask获取异步结果";            }        });        executorService.execute(futureTask);        System.out.println(futureTask.get());        executorService.shutdown();    }}
```

复制代码



运行结果如下所示。



```
测试FutureTask获取异步结果
```

复制代码



可以看到使用 Future 接口或者 FutureTask 类来获取异步结果比使用回调接口获取异步结果简单多了。注意：实现异步的方式很多，这里只是用多线程举例。

## 二、深度解析 Future 接口



**1.Future 接口**



Future 是 JDK1.5 新增的异步编程接口，其源代码如下所示。



```
package java.util.concurrent;
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit)        throws InterruptedException, ExecutionException, TimeoutException;}
```

复制代码



可以看到，在 Future 接口中，总共定义了 5 个抽象方法。接下来，就分别介绍下这 5 个方法的含义。



- cancel(boolean)



取消任务的执行，接收一个 boolean 类型的参数，成功取消任务，则返回 true，否则返回 false。当任务已经完成，已经结束或者因其他原因不能取消时，方法会返回 false，表示任务取消失败。当任务未启动调用了此方法，并且结果返回 true（取消成功），则当前任务不再运行。如果任务已经启动，会根据当前传递的 boolean 类型的参数来决定是否中断当前运行的线程来取消当前运行的任务。



- isCancelled()



判断任务在完成之前是否被取消，如果在任务完成之前被取消，则返回 true；否则，返回 false。



**这里需要注意一个细节：只有任务未启动，或者在完成之前被取消，才会返回 true，表示任务已经被成功取消。其他情况都会返回 false。**



- isDone()



判断任务是否已经完成，如果任务正常结束、抛出异常退出、被取消，都会返回 true，表示任务已经完成。



- get()



当任务完成时，直接返回任务的结果数据；当任务未完成时，等待任务完成并返回任务的结果数据。



- get(long, TimeUnit)



当任务完成时，直接返回任务的结果数据；当任务未完成时，等待任务完成，并设置了超时等待时间。在超时时间内任务完成，则返回结果；否则，抛出 TimeoutException 异常。



**2.RunnableFuture 接口**



Future 接口有一个重要的子接口，那就是 RunnableFuture 接口，RunnableFuture 接口不但继承了 Future 接口，而且继承了 java.lang.Runnable 接口，其源代码如下所示。



```
package java.util.concurrent;
public interface RunnableFuture<V> extends Runnable, Future<V> {    void run();}
```

复制代码



这里，问一下，RunnableFuture 接口中有几个抽象方法？想好了再说！哈哈哈。。。



这个接口比较简单 run()方法就是运行任务时调用的方法。



**3.FutureTask 类**



FutureTask 类是 RunnableFuture 接口的一个非常重要的实现类，它实现了 RunnableFuture 接口、Future 接口和 Runnable 接口的所有方法。FutureTask 类的源代码比较多，这个就不粘贴了，大家自行到 java.util.concurrent 下查看。



（1）FutureTask 类中的变量与常量



在 FutureTask 类中首先定义了一个状态变量 state，这个变量使用了 volatile 关键字修饰，这里，大家只需要知道 volatile 关键字通过内存屏障和禁止重排序优化来实现线程安全，后续会单独深度分析 volatile 关键字是如何保证线程安全的。紧接着，定义了几个任务运行时的状态常量，如下所示。



```
private volatile int state;private static final int NEW          = 0;private static final int COMPLETING   = 1;private static final int NORMAL       = 2;private static final int EXCEPTIONAL  = 3;private static final int CANCELLED    = 4;private static final int INTERRUPTING = 5;private static final int INTERRUPTED  = 6;
```

复制代码



其中，代码注释中给出了几个可能的状态变更流程，如下所示。



```
NEW -> COMPLETING -> NORMALNEW -> COMPLETING -> EXCEPTIONALNEW -> CANCELLEDNEW -> INTERRUPTING -> INTERRUPTED
```

复制代码



接下来，定义了其他几个成员变量，如下所示。



```
private Callable<V> callable;private Object outcome; private volatile Thread runner;private volatile WaitNode waiters;
```

复制代码



又看到我们所熟悉的 Callable 接口了，Callable 接口那肯定就是用来调用 call()方法执行具体任务了。



- outcome：Object 类型，表示通过 get()方法获取到的结果数据或者异常信息。
- runner：运行 Callable 的线程，运行期间会使用 CAS 保证线程安全，这里大家只需要知道 CAS 是 Java 保证线程安全的一种方式，后续文章中会深度分析 CAS 如何保证线程安全。
- waiters：WaitNode 类型的变量，表示等待线程的堆栈，在 FutureTask 的实现中，会通过 CAS 结合此堆栈交换任务的运行状态。



看一下 WaitNode 类的定义，如下所示。



```
static final class WaitNode {	volatile Thread thread;	volatile WaitNode next;	WaitNode() { thread = Thread.currentThread(); }}
```

复制代码



可以看到，WaitNode 类是 FutureTask 类的静态内部类，类中定义了一个 Thread 成员变量和指向下一个 WaitNode 节点的引用。其中通过构造方法将 thread 变量设置为当前线程。



（2）构造方法



接下来，是 FutureTask 的两个构造方法，比较简单，如下所示。



```
public FutureTask(Callable<V> callable) {	if (callable == null)		throw new NullPointerException();	this.callable = callable;	this.state = NEW;}
public FutureTask(Runnable runnable, V result) {	this.callable = Executors.callable(runnable, result);	this.state = NEW;}
```

复制代码



（3）是否取消与完成方法



继续向下看源码，看到一个任务是否取消的方法，和一个任务是否完成的方法，如下所示。



```
public boolean isCancelled() {	return state >= CANCELLED;}
public boolean isDone() {	return state != NEW;}
```

复制代码



这两方法中，都是通过判断任务的状态来判定任务是否已取消和已完成的。为啥会这样判断呢？再次查看 FutureTask 类中定义的状态常量发现，其常量的定义是有规律的，并不是随意定义的。其中，大于或者等于 CANCELLED 的常量为 CANCELLED、INTERRUPTING 和 INTERRUPTED，这三个状态均可以表示线程已经被取消。当状态不等于 NEW 时，可以表示任务已经完成。



**通过这里，大家可以学到一点：以后在编码过程中，要按照规律来定义自己使用的状态，尤其是涉及到业务中有频繁的状态变更的操作，有规律的状态可使业务处理变得事半功倍，这也是通过看别人的源码设计能够学到的，这里，建议大家还是多看别人写的优秀的开源框架的源码。**



（4）取消方法



我们继续向下看源码，接下来，看到的是 cancel(boolean)方法，如下所示。



```
public boolean cancel(boolean mayInterruptIfRunning) {	if (!(state == NEW &&		  UNSAFE.compareAndSwapInt(this, stateOffset, NEW,			  mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))		return false;	try {    // in case call to interrupt throws exception		if (mayInterruptIfRunning) {			try {				Thread t = runner;				if (t != null)					t.interrupt();			} finally { // final state				UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);			}		}	} finally {		finishCompletion();	}	return true;}
```

复制代码



接下来，拆解 cancel(boolean)方法。在 cancel(boolean)方法中，首先判断任务的状态和 CAS 的操作结果，如果任务的状态不等于 NEW 或者 CAS 的操作返回 false，则直接返回 false，表示任务取消失败。如下所示。



```
if (!(state == NEW &&	  UNSAFE.compareAndSwapInt(this, stateOffset, NEW,		  mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))	return false;
```

复制代码



接下来，在 try 代码块中，首先判断是否可以中断当前任务所在的线程来取消任务的运行。如果可以中断当前任务所在的线程，则以一个 Thread 临时变量来指向运行任务的线程，当指向的变量不为空时，调用线程对象的 interrupt()方法来中断线程的运行，最后将线程标记为被中断的状态。如下所示。



```
try {	if (mayInterruptIfRunning) {		try {			Thread t = runner;			if (t != null)				t.interrupt();		} finally { // final state			UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);		}	}}
```

复制代码



这里，发现变更任务状态使用的是 UNSAFE.putOrderedInt()方法，这个方法是个什么鬼呢？点进去看一下，如下所示。



```
public native void putOrderedInt(Object var1, long var2, int var4);
```

复制代码



可以看到，又是一个本地方法，嘿嘿，这里先不管它，后续文章会详解这些方法的作用。



接下来，cancel(boolean)方法会进入 finally 代码块，如下所示。



```
finally {	finishCompletion();}
```

复制代码



可以看到在 finallly 代码块中调用了 finishCompletion()方法，顾名思义，finishCompletion()方法表示结束任务的运行，接下来看看它是如何实现的。点到 finishCompletion()方法中看一下，如下所示。



```
private void finishCompletion() {	// assert state > COMPLETING;	for (WaitNode q; (q = waiters) != null;) {		if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {			for (;;) {				Thread t = q.thread;				if (t != null) {					q.thread = null;					LockSupport.unpark(t);				}				WaitNode next = q.next;				if (next == null)					break;				q.next = null; // unlink to help gc				q = next;			}			break;		}	}	done();	callable = null;        // to reduce footprint}
```

复制代码



在 finishCompletion()方法中，首先定义一个 for 循环，循环终止因子为 waiters 为 null，在循环中，判断 CAS 操作是否成功，如果成功进行 if 条件中的逻辑。首先，定义一个 for 自旋循环，在自旋循环体中，唤醒 WaitNode 堆栈中的线程，使其运行完成。当 WaitNode 堆栈中的线程运行完成后，通过 break 退出外层 for 循环。接下来调用 done()方法。done()方法又是个什么鬼呢？点进去看一下，如下所示。



```
protected void done() { }
```

复制代码



可以看到，done()方法是一个空的方法体，交由子类来实现具体的业务逻辑。



当我们的具体业务中，需要在取消任务时，执行一些额外的业务逻辑，可以在子类中覆写 done()方法的实现。



（5）get()方法



继续向下看 FutureTask 类的代码，FutureTask 类中实现了两个 get()方法，如下所示。



```
public V get() throws InterruptedException, ExecutionException {	int s = state;	if (s <= COMPLETING)		s = awaitDone(false, 0L);	return report(s);}
public V get(long timeout, TimeUnit unit)	throws InterruptedException, ExecutionException, TimeoutException {	if (unit == null)		throw new NullPointerException();	int s = state;	if (s <= COMPLETING &&		(s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)		throw new TimeoutException();	return report(s);}
```

复制代码



没参数的 get()方法为当任务未运行完成时，会阻塞，直到返回任务结果。有参数的 get()方法为当任务未运行完成，并且等待时间超出了超时时间，会 TimeoutException 异常。



两个 get()方法的主要逻辑差不多，一个没有超时设置，一个有超时设置，这里说一下主要逻辑。判断任务的当前状态是否小于或者等于 COMPLETING，也就是说，任务是 NEW 状态或者 COMPLETING，调用 awaitDone()方法，看下 awaitDone()方法的实现，如下所示。



```
private int awaitDone(boolean timed, long nanos)	throws InterruptedException {	final long deadline = timed ? System.nanoTime() + nanos : 0L;	WaitNode q = null;	boolean queued = false;	for (;;) {		if (Thread.interrupted()) {			removeWaiter(q);			throw new InterruptedException();		}
		int s = state;		if (s > COMPLETING) {			if (q != null)				q.thread = null;			return s;		}		else if (s == COMPLETING) // cannot time out yet			Thread.yield();		else if (q == null)			q = new WaitNode();		else if (!queued)			queued = UNSAFE.compareAndSwapObject(this, waitersOffset,												 q.next = waiters, q);		else if (timed) {			nanos = deadline - System.nanoTime();			if (nanos <= 0L) {				removeWaiter(q);				return state;			}			LockSupport.parkNanos(this, nanos);		}		else			LockSupport.park(this);	}}
```

复制代码



接下来，拆解 awaitDone()方法。在 awaitDone()方法中，最重要的就是 for 自旋循环，在循环中首先判断当前线程是否被中断，如果已经被中断，则调用 removeWaiter()将当前线程从堆栈中移除，并且抛出 InterruptedException 异常，如下所示。



```
if (Thread.interrupted()) {	removeWaiter(q);	throw new InterruptedException();}
```

复制代码



接下来，判断任务的当前状态是否完成，如果完成，并且堆栈句柄不为空，则将堆栈中的当前线程设置为空，返回当前任务的状态，如下所示。



```
int s = state;if (s > COMPLETING) {	if (q != null)		q.thread = null;	return s;}
```

复制代码



当任务的状态为 COMPLETING 时，使当前线程让出 CPU 资源，如下所示。



```
else if (s == COMPLETING)	Thread.yield();
```

复制代码



如果堆栈为空，则创建堆栈对象，如下所示。



```
else if (q == null)	q = new WaitNode();
```

复制代码



如果 queued 变量为 false，通过 CAS 操作为 queued 赋值，如果 awaitDone()方法传递的 timed 参数为 true，则计算超时时间，当时间已超时，则在堆栈中移除当前线程并返回任务状态，如下所示。如果未超时，则重置超时时间，如下所示。



```
else if (!queued)	queued = UNSAFE.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);else if (timed) {	nanos = deadline - System.nanoTime();	if (nanos <= 0L) {		removeWaiter(q);		return state;	}	LockSupport.parkNanos(this, nanos);}
```

复制代码



如果不满足上述的所有条件，则将当前线程设置为等待状态，如下所示。



```
else	LockSupport.park(this);
```

复制代码



接下来，回到 get()方法中，当 awaitDone()方法返回结果，或者任务的状态不满足条件时，都会调用 report()方法，并将当前任务的状态传递到 report()方法中，并返回结果，如下所示。



```
return report(s);
```

复制代码



看来，这里还要看下 report()方法啊，点进去看下 report()方法的实现，如下所示。



```
private V report(int s) throws ExecutionException {	Object x = outcome;	if (s == NORMAL)		return (V)x;	if (s >= CANCELLED)		throw new CancellationException();	throw new ExecutionException((Throwable)x);}
```

复制代码



可以看到，report()方法的实现比较简单，首先，将 outcome 数据赋值给 x 变量，接下来，主要是判断接收到的任务状态，如果状态为 NORMAL，则将 x 强转为泛型类型返回；当任务的状态大于或者等于 CANCELLED，也就是任务已经取消，则抛出 CancellationException 异常，其他情况则抛出 ExecutionException 异常。



至此，get()方法分析完成。注意：一定要理解 get()方法的实现，因为 get()方法是我们使用 Future 接口和 FutureTask 类时，使用的比较频繁的一个方法。



（6）set()方法与 setException()方法



继续看 FutureTask 类的代码，接下来看到的是 set()方法与 setException()方法，如下所示。



```
protected void set(V v) {	if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {		outcome = v;		UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state		finishCompletion();	}}
protected void setException(Throwable t) {	if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {		outcome = t;		UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state		finishCompletion();	}}
```

复制代码



通过源码可以看出，set()方法与 setException()方法整体逻辑几乎一样，只是在设置任务状态时一个将状态设置为 NORMAL，一个将状态设置为 EXCEPTIONAL。



至于 finishCompletion()方法，前面已经分析过。



（7）run()方法与 runAndReset()方法



接下来，就是 run()方法了，run()方法的源代码如下所示。



```
public void run() {	if (state != NEW ||		!UNSAFE.compareAndSwapObject(this, runnerOffset,									 null, Thread.currentThread()))		return;	try {		Callable<V> c = callable;		if (c != null && state == NEW) {			V result;			boolean ran;			try {				result = c.call();				ran = true;			} catch (Throwable ex) {				result = null;				ran = false;				setException(ex);			}			if (ran)				set(result);		}	} finally {		// runner must be non-null until state is settled to		// prevent concurrent calls to run()		runner = null;		// state must be re-read after nulling runner to prevent		// leaked interrupts		int s = state;		if (s >= INTERRUPTING)			handlePossibleCancellationInterrupt(s);	}}
```

复制代码



可以这么说，只要使用了 Future 和 FutureTask，就必然会调用 run()方法来运行任务，掌握 run()方法的流程是非常有必要的。在 run()方法中，如果当前状态不是 NEW，或者 CAS 操作返回的结果为 false，则直接返回，不再执行后续逻辑，如下所示。



```
if (state != NEW ||	!UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread()))	return;
```

复制代码



接下来，在 try 代码块中，将成员变量 callable 赋值给一个临时变量 c，判断临时变量不等于 null，并且任务状态为 NEW，则调用 Callable 接口的 call()方法，并接收结果数据。并将 ran 变量设置为 true。当程序抛出异常时，将接收结果的变量设置为 null，ran 变量设置为 false，并且调用 setException()方法将任务的状态设置为 EXCEPTIONA。接下来，如果 ran 变量为 true，则调用 set()方法，如下所示。



```
try {	Callable<V> c = callable;	if (c != null && state == NEW) {		V result;		boolean ran;		try {			result = c.call();			ran = true;		} catch (Throwable ex) {			result = null;			ran = false;			setException(ex);		}		if (ran)			set(result);	}}
```

复制代码



接下来，程序会进入 finally 代码块中，如下所示。



```
finally {	// runner must be non-null until state is settled to	// prevent concurrent calls to run()	runner = null;	// state must be re-read after nulling runner to prevent	// leaked interrupts	int s = state;	if (s >= INTERRUPTING)		handlePossibleCancellationInterrupt(s);}
```

复制代码



这里，将 runner 设置为 null，如果任务的当前状态大于或者等于 INTERRUPTING，也就是线程被中断了。则调用 handlePossibleCancellationInterrupt()方法，接下来，看下 handlePossibleCancellationInterrupt()方法的实现。



```
private void handlePossibleCancellationInterrupt(int s) {	if (s == INTERRUPTING)		while (state == INTERRUPTING)			Thread.yield();}
```

复制代码



可以看到，handlePossibleCancellationInterrupt()方法的实现比较简单，当任务的状态为 INTERRUPTING 时，使用 while()循环，条件为当前任务状态为 INTERRUPTING，将当前线程占用的 CPU 资源释放，也就是说，当任务运行完成后，释放线程所占用的资源。



runAndReset()方法的逻辑与 run()差不多，只是 runAndReset()方法会在 finally 代码块中将任务状态重置为 NEW。runAndReset()方法的源代码如下所示，就不重复说明了。



```
protected boolean runAndReset() {	if (state != NEW ||		!UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread()))		return false;	boolean ran = false;	int s = state;	try {		Callable<V> c = callable;		if (c != null && s == NEW) {			try {				c.call(); // don't set result				ran = true;			} catch (Throwable ex) {				setException(ex);			}		}	} finally {		// runner must be non-null until state is settled to		// prevent concurrent calls to run()		runner = null;		// state must be re-read after nulling runner to prevent		// leaked interrupts		s = state;		if (s >= INTERRUPTING)			handlePossibleCancellationInterrupt(s);	}	return ran && s == NEW;}
```

复制代码



（8）removeWaiter()方法



removeWaiter()方法中主要是使用自旋循环的方式来移除 WaitNode 中的线程，比较简单，如下所示。



```
private void removeWaiter(WaitNode node) {	if (node != null) {		node.thread = null;		retry:		for (;;) {          // restart on removeWaiter race			for (WaitNode pred = null, q = waiters, s; q != null; q = s) {				s = q.next;				if (q.thread != null)					pred = q;				else if (pred != null) {					pred.next = s;					if (pred.thread == null) // check for race						continue retry;				}				else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,													  q, s))					continue retry;			}			break;		}	}}
```

复制代码



最后，在 FutureTask 类的最后，有如下代码。



```
// Unsafe mechanicsprivate static final sun.misc.Unsafe UNSAFE;private static final long stateOffset;private static final long runnerOffset;private static final long waitersOffset;static {	try {		UNSAFE = sun.misc.Unsafe.getUnsafe();		Class<?> k = FutureTask.class;		stateOffset = UNSAFE.objectFieldOffset			(k.getDeclaredField("state"));		runnerOffset = UNSAFE.objectFieldOffset			(k.getDeclaredField("runner"));		waitersOffset = UNSAFE.objectFieldOffset			(k.getDeclaredField("waiters"));	} catch (Exception e) {		throw new Error(e);	}}
```

复制代码



关于这些代码的作用，会在后续深度解析 CAS 文章中详细说明，这里就不再探讨。



至此，关于 Future 接口和 FutureTask 类的源码就分析完了。