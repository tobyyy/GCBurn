package gcburn.common.distributions;

import gcburn.common.StdRandom;

public abstract class DistributionBase implements IDistribution {

	protected final StdRandom random;

	protected DistributionBase(StdRandom random) {
		this.random = random;
	}

	public StdRandom getRandom() {
		return random;
	}
}
