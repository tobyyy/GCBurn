package gcburn.common.distributions;

import java.util.function.DoubleUnaryOperator;

public final class TransformedDistribution implements IDistribution {

	private final IDistribution source;
	private final DoubleUnaryOperator transformer;

	public TransformedDistribution(IDistribution source, DoubleUnaryOperator transformer) {
		this.source = source;
		this.transformer = transformer;
	}

	@Override
	public double sample() {
		return transformer.applyAsDouble(source.sample());
	}

	public IDistribution getSource() {
		return source;
	}

	public DoubleUnaryOperator getTransformer() {
		return transformer;
	}
}
