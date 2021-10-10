package players.mcts;

import java.util.Random;

/**
 * Get samples from a multinomial distribution.
 *
 * Based on: https://github.com/tedunderwood/LDA/blob/master/Multinomial.java
 *
 * NOTE: Do not use this code in production unless you have thoroughly tested
 * it and are comfortable that you know what it does!
 */
public class SimpleMultinomialSampler {

    private final Random _random;

    // NOTE: Ideally should pass in a ThreadLocalRandom.
    public SimpleMultinomialSampler(final Random random) {
        _random = random;
        // Normalize all the passed in "probabilities" so that they sum to 1.0.
    }

    /**
     * Returns the result of a multinomial sample of n trials.
     * The value of k (the number of possible outcomes) is determined by the
     * number of probabilities passed into the constructor.
     */
    public int[] multinomialSample(final int n, double[] _probabilities) {
        // The length of `_probabilities` is the number of possible outcomes.
        final int result[] = new int[_probabilities.length];

        // Get the result of each trial and increment the count for that outcome.
        for (int i = 0; i < n; i++) {
            result[multinomialTrial(_probabilities)]++;
        }

        return result;
    }

    /**
     * The `_probabilities` field is an array of "cumulative" probabilities.
     * The first element has the value p1, the second has p1 + p2, the third has p1 + p2 + p3, etc.
     * By definition, the last bin should have a value of 1.0.
     */
    public int multinomialTrial(double[] _probabilities) {
        double sample = _random.nextDouble(); // Between [0, 1)
        for (int i = 0; i < _probabilities.length; ++i) {
            // Find the first bucket whose upper bound is above the sampled value.
            if (sample < _probabilities[i]) {
                return i;
            }
        }
        // Catch-all return statement to ensure code compiles.
        return _probabilities.length - 1;
    }

    /**
     * Given an array of raw probabilities, this will transform the values in place in the the following manner:
     * 1. The sum of the values will be computed.
     * 2. Each value will be divided by the sum, normalizing them so that the sum is roughly 1.0.
     * 3. The values will be converted into cumulative values.
     *
     * Example: Input is: [0.5, 0.5, 1.0]
     * 1. Sum is 2.0
     * 2. After normalization: [0.25, 0.25, 0.5]
     * 3. After converting to cumulative values: [0.25, 0.5, 1.0]
     *
     * The form in (3) is useful for converting a uniformly-sampled value between [0, 1) into a multinomial sample,
     * because the values now represent the upper-bounds of the "range" between [0, 1) that the represent the probability
     * of that outcome. Thus, given a uniformly-sampled value between [0, 1), we just need to find the first/lowest bin
     * whose upper-bound is more than the sampled value.
     */
    public static double[] normalizeProbabilitiesToCumulative(final double[] probabilities) {
        if (probabilities == null || probabilities.length < 2) {
            throw new IllegalArgumentException("probabilities must have more than one value");
        }

        double sum = 0.0;
        for (double value: probabilities) {
            sum += value;
        }

        double cumulative = 0.0;
        final double[] distribution = new double[probabilities.length];
        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            distribution[i] = cumulative/sum;
        }

        // To ensure the right-most bin is always 1.0.
        distribution[distribution.length - 1] = 1.0;

        return distribution;
    }
}