package gcburn.common.distributions;

import java.util.Optional;

import gcburn.common.StdRandom;

public class NormalDistribution extends DistributionBase {

	private final double mean;
	private final double stdDev;

	public NormalDistribution(StdRandom random, double mean, double stdDev) {
		super(random);
		this.mean = mean;
		this.stdDev = stdDev;
	}

	@Override
	public double sample() {
		return sample(random, mean, stdDev);
	}

	public static double sample(StdRandom random, double mean, double stdDev) {
		while (true) {
			Optional<PolarTransform> transform = PolarTransform.get(random.nextDouble(), random.nextDouble());
			if (transform.isPresent()) {
				return mean + stdDev * transform.get().getX();
			}
		}
	}

}
