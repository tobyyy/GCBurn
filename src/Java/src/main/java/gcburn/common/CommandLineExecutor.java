package gcburn.common;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Safely executes commands, properly draining outputs and returning sensible result
 */
public class CommandLineExecutor implements Closeable {

	private static final int DEFAUTL_PROCESS_TIMEOUT_SECONDS = 60;

	private final ExecutorService pool = Executors.newCachedThreadPool();

	//for printing error messages
	private final PrintStream err;

	public CommandLineExecutor() {
		this(System.err);
	}

	public CommandLineExecutor(PrintStream errStream) {
		this.err = errStream;
	}

	@Override
	public void close() {
		pool.shutdown();
	}

	public static CommandResult execStatic(String... args) throws IOException {
		return execStatic(DEFAUTL_PROCESS_TIMEOUT_SECONDS, args);
	}

	public static CommandResult execStatic(int timeoutSec, String... args) throws IOException {
		try (CommandLineExecutor cle = new CommandLineExecutor()) {
			return cle.exec(timeoutSec, args);
		}
	}

	public CommandResult exec(String... commandArgs) throws IOException {
		return exec(DEFAUTL_PROCESS_TIMEOUT_SECONDS, commandArgs);
	}

	public CommandResult exec(int timeoutSec, String... commandArgs) throws IOException {
		Process process = Runtime.getRuntime().exec(commandArgs);
		ConsoleBuffer stdBuffer = new ConsoleBuffer(process.getInputStream());
		ConsoleBuffer errBuffer = new ConsoleBuffer(process.getErrorStream());
		try {
			pool.submit(stdBuffer);
			pool.submit(errBuffer);
			if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
				err.println("Process of command " + Arrays.toString(commandArgs) + " timed out. Destroying.");
				process.destroyForcibly().waitFor();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			stdBuffer.stop();
			errBuffer.stop();
		}
		return new CommandResult(process.exitValue(), stdBuffer.toString(), errBuffer.toString());
	}

	private class ConsoleBuffer implements Runnable {
		private final InputStream stream;
		private final StringWriter writer = new StringWriter();
		private volatile boolean run = true;

		ConsoleBuffer(InputStream stream) {
			this.stream = stream;
		}

		@Override
		public void run() {
			try {
				while (run) {
					int read;
					if ((read = stream.read()) == -1) {
						break;
					}
					writer.write(read);
				}
			} catch (Exception e) {
				err.println("Exception in process output buffer thread.");
				e.printStackTrace(err);
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
				}
				try {
					writer.close();
				} catch (IOException e) {
				}
			}
		}

		void stop() {
			run = false;
		}

		@Override
		public String toString() {
			return writer.toString();
		}
	}

	public static class CommandResult {

		private final int exitValue;
		private final String std;
		private final String err;

		public CommandResult(int exitValue, String std, String err) {
			this.exitValue = exitValue;
			this.std = std;
			this.err = err;
		}

		public int getExitValue() {
			return exitValue;
		}

		public String getStd() {
			return std;
		}

		public String getErr() {
			return err;
		}
	}

}
