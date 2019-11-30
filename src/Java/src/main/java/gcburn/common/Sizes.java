package gcburn.common;

public final class Sizes {

	private Sizes() {
		//		no instance
	}

	public static final double KB = 1024;
	public static final double MB = KB * KB;
	public static final double GB = MB * KB;
	public static final double TB = GB * KB;

	public static final double KILO = 1000;
	public static final double MEGA = KILO * KILO;
	public static final double GIGA = MEGA * KILO;
	public static final double TERA = GIGA * KILO;

	public static final double MILLI = 0.001;
	public static final double MICRO = MILLI * MILLI;
	public static final double NANO = MICRO * MILLI;
	public static final double PICO = NANO * MILLI;

}
