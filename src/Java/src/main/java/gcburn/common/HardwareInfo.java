package gcburn.common;

import java.io.IOException;
import java.util.Arrays;

public class HardwareInfo {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private HardwareInfo() {
		//no instance
	}

	private enum Os {
		WINDOWS, LINUX, MACOSX;

		private static final Os CURRENT = getOs();

		private static Os getOs() {
			String osName = System.getProperty("os.name");
			String searchStr = osName.toLowerCase();

			if (searchStr.contains("win")) {
				return WINDOWS;
			}
			if (searchStr.contains("linux")) {
				return LINUX;
			}
			if (searchStr.contains("mac") || searchStr.contains("darwin")) {
				return MACOSX;
			}
			throw new UnsupportedOperationException("Unsupported OS: " + osName);
		}
	}

	//todo trim lines
	public static String getCpuModelName() {
		switch (Os.CURRENT) {
		case WINDOWS:
			return lastSensibleLine(runCmd("wmic", "cpu", "get", "name"));
		case LINUX:
			return runCmd("cat", "/proc/cpuinfo");
		case MACOSX:
			return runCmd("sysctl", "-n", "machdep.cpu.brand_string");
		}
		//java quirks ¯\_(ツ)_/¯
		throw new UnsupportedOperationException("Unsupported OS: " + Os.CURRENT);
	}

	public static int getRamSize() {
		switch (Os.CURRENT) {
		case WINDOWS:
			String res = lastSensibleLine(runCmd("wmic", "computersystem", "get", "TotalPhysicalMemory"));
			return (int)Math.round(Long.parseLong(res) / Sizes.GB);
		}
		//todo tweak on other platforms
		throw new UnsupportedOperationException("Unsupported OS: " + Os.CURRENT);
	}

	private static String runCmd(String... cmd) {
		try {
			CommandLineExecutor.CommandResult commandResult = CommandLineExecutor.execStatic(cmd);
			if (commandResult.getExitValue() == 0) {
				return commandResult.getStd();
			}
			throw new RuntimeException("Error running command " + Arrays.toString(cmd) + ". Exit code: " + commandResult.getExitValue());
		} catch (IOException e) {
			throw new RuntimeException("Error running command " + Arrays.toString(cmd), e);
		}
	}

	private static String lastSensibleLine(String str) {
		String[] split = str.split(LINE_SEPARATOR);

		String last = "";

		for (String s : split) {
			String trim = s.trim();
			if (!trim.isEmpty()) {
				last = trim;
			}
		}

		return last;
	}
}
