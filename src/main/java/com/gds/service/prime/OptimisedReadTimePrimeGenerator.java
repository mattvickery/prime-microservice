package com.gds.service.prime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.math.BigDecimal.valueOf;
import static org.springframework.util.Assert.state;

/**
 * @author Matt Vickery (matt.d.vickery@greendotsoftware.co.uk)
 * @since 13/10/2017
 * <p/>
 * Build a prime generator that enables a caller to request a set of prime numbers, either up to a maximum value
 * or between a user supplied range
 * <p/>
 * Maintains two potentially large collections during construction so memory size is a trade off against
 * fast access. At the end of construction, the initial array used for building the cache is discarded so half
 * the space overhead is on-going.
 * <p/>
 */
public class OptimisedReadTimePrimeGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(OptimisedReadTimePrimeGenerator.class);
    private final int sieveSize;
    private final int maxFactorSize;
    private final StopWatch stopWatch = new StopWatch();
    private boolean[] primes;
    private final List<Integer> primeValueCache = new ArrayList<>();
    private boolean initialised = false;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    public OptimisedReadTimePrimeGenerator() {
        this((int) Math.round(Math.pow(2, 24)));
    }

    public OptimisedReadTimePrimeGenerator(final int sieveSize) {
        this.sieveSize = sieveSize;
        maxFactorSize = (int) Math.round(Math.sqrt(sieveSize));
        init();
    }

    public static void main(final String[] args) {

        final OptimisedReadTimePrimeGenerator primeGenerator = new OptimisedReadTimePrimeGenerator();
        final String results = primeGenerator.harvestPrimesUpToValue(100).stream()
                .map(Object:: toString)
                .collect(Collectors.joining(","));
        LOG.debug(results);
    }

    public List<Integer> harvestPrimesUpToValue(final int value) {
        state(value >= 2, "Primes can only be harvested for values greater than 2.");
        return harvestPrimesForRange(0, value);
    }

    public List<Integer> harvestPrimesForRange(final int start, final int end) {

        state(start <= end, "");
        state(start >= 2, "Primes can only be harvested for values greater than 2.");
        state(end < sieveSize, "");

        if (LOG.isDebugEnabled())
            stopWatch.start();

        int startIndex = 0, endIndex = 0, index = 0;
        while (primeValueCache.get(index) <= end) {
            final int locatedValue = primeValueCache.get(index);
            if (locatedValue == start)
                startIndex = index;
            if (locatedValue < start)
                startIndex = index + 1;
            if (locatedValue <= end)
                endIndex = index;
            index++;
        }

        if (LOG.isDebugEnabled())
            stopWatch.stop();
        LOG.debug("Selection operation duration(ms): {}", stopWatch.getLastTaskTimeMillis());

        return primeValueCache.subList(startIndex, endIndex + 1);
    }

    private void init() {

        if (LOG.isDebugEnabled())
            stopWatch.start();
        primes = new boolean[sieveSize];
        Arrays.fill(primes, true);
        primes[0] = primes[1] = false;

        for (int index = 2; index <= maxFactorSize; index++)
            sieve(primes, sieveSize, index);
        for (int index = 0; index < primes.length; index++)
            if (primes[index])
                primeValueCache.add(index);

        if (LOG.isDebugEnabled())
            stopWatch.stop();
        initialised = true;
        LOG.debug("Harvest operation duration(ms): {}", formatter.format(stopWatch.getLastTaskTimeMillis()));
        LOG.debug("Prime cache initialised with {} values, calculated at {} primes p/s.",
                formatter.format(primes.length),
                formatter.format(rate(primes.length, stopWatch.getLastTaskTimeMillis())));
    }

    private void sieve(final boolean[] primes, final int sieveSize, final int rhsFactor) {
        for (int lhsFactor = 2; lhsFactor * rhsFactor < sieveSize; lhsFactor++)
            primes[lhsFactor * rhsFactor] = false;
    }

    private int rate(final int size, final long timeInMillis) {
        return new BigDecimal(size)
                .divide(valueOf(timeInMillis == 0 ? 1 : timeInMillis), 8, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(1000)).intValue();
    }
}