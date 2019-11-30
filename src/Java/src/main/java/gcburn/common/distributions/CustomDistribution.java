package gcburn.common.distributions;

import java.util.function.ToDoubleFunction;

import gcburn.common.StdRandom;

public class CustomDistribution extends DistributionBase {

	private final ToDoubleFunction<StdRandom> sampler;

	protected CustomDistribution(StdRandom random, ToDoubleFunction<StdRandom> sampler) {
		super(random);
		this.sampler = sampler;
	}

	@Override
	public double sample() {
		return sampler.applyAsDouble(random);
	}

	public ToDoubleFunction<StdRandom> getSampler() {
		return sampler;
	}
}
