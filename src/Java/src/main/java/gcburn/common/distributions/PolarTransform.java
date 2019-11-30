package gcburn.common.distributions;

import java.util.Optional;

public class PolarTransform {
	private final double x;
	private final double y;

	private PolarTransform(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public static Optional<PolarTransform> get(double x01, double y01) {
		double xn11 = 2.0 * x01 - 1.0;
		double yn11 = 2.0 * y01 - 1.0;
		double r2 = (xn11 * xn11) + (yn11 * yn11);
		if (r2 >= 1.0 || r2 == 0.0) {
			return Optional.empty();
		}
		double factor = Math.sqrt(-2.0 * Math.log(r2) / r2);
		return Optional.of(new PolarTransform(xn11 * factor, yn11 * factor));
	}
}
