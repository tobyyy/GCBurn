package gcburn.common;

public class StdRandom {
	public static final long MODULO = 2147483647; // 2^31 - 1, 8th Mersenne prime
	public static final long MULTIPLIER = 48271; // Prime, same as in C++11 minstd_rand

	private int seed;

	public StdRandom(int seed) {
		this.seed = seed;
	}

	public int getSeed() {
		return seed;
	}

	public void setSeed(int seed) {
		this.seed = seed;
	}

	public int next() {
		seed = (int) ((seed * MULTIPLIER) % MODULO);
		return seed;
	}

	public double nextDouble() {
		return ((double) next()) / MODULO;
	}
}
