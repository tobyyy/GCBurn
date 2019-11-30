package gcburn.common;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParallelRunner<T_ACTIVITY> {

	public static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

	private final List<T_ACTIVITY> activities;
	private final List<Thread> threads;

	public ParallelRunner(IntFunction<T_ACTIVITY> factory, Consumer<T_ACTIVITY> runner, int threadCount) {
		this.activities = IntStream.range(0, threadCount).mapToObj(factory).collect(Collectors.toList());
		this.threads = activities.stream().map(a -> new Thread(() -> runner.accept(a))).collect(Collectors.toList());
	}

	public List<T_ACTIVITY> run() throws InterruptedException {
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		return activities;
	}

	public static <T_ACTIVITY extends IActivity> ParallelRunner<T_ACTIVITY> create(IntFunction<T_ACTIVITY> factory) {
		return new ParallelRunner<>(factory, IActivity::run, THREAD_COUNT);
	}

	public static <T_ACTIVITY extends IActivity> ParallelRunner<T_ACTIVITY> create(IntFunction<T_ACTIVITY> factory, int threadCount) {
		return new ParallelRunner<>(factory, IActivity::run, threadCount);
	}

	public static <T_ACTIVITY> ParallelRunner<T_ACTIVITY> create(IntFunction<T_ACTIVITY> factory, Consumer<T_ACTIVITY> runner, int threadCount) {
		return new ParallelRunner<>(factory, runner, threadCount);
	}

	//FIXME what if we replaced this by Runnable?
	public interface IActivity {
		void run();
	}
}
