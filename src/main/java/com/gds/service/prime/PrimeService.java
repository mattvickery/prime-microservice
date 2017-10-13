package com.gds.service.prime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.util.Assert.state;

/**
 * @author Matt Vickery (matt.d.vickery@greendotsoftware.co.uk)
 * @since 13/10/2017
 * <p/>
 * Build a service that enables a caller to request a set of prime numbers, either up to a maximum value
 * or between a user supplied range
 *
 * Maintains two potentially large collections during construction so memory size is a trade off against
 * fast access. At the end of construction, the initial array used for building the cache is discarded so half
 * the space overhead is on-going.
 */
public class PrimeService {

    private static final Logger LOG = LoggerFactory.getLogger(PrimeService.class);
    private static final int SIEVE_SIZE = (int) Math.round(Math.pow(2, 26));
    private static final int MAX_FACTOR_SIZE = (int) Math.round(Math.sqrt(SIEVE_SIZE));
    private final StopWatch stopWatch = new StopWatch();
    private boolean[] primes;
    private final List<Integer> primeValueCache = new ArrayList<>();
    private boolean initialised = false;
    private DecimalFormat format = new DecimalFormat("###,###,###");

    public PrimeService() {
        init();
    }

    public static void main(final String[] args) {

        final PrimeService primeService = new PrimeService();
        final String results = primeService.harvestPrimesUpToValue(100).stream()
                .map(Object:: toString)
                .collect(Collectors.joining(","));
        LOG.debug(results);
    }

    public List<Integer> harvestPrimesUpToValue(final int value) {
        return harvestPrimesForRange(0, value);
    }

    public List<Integer> harvestPrimesForRange(final int start, final int end) {

        state(start <= end, "");
        state(start >= 0, "");
        state(end < SIEVE_SIZE, "");

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

    private PrimeService init() {

        if (LOG.isDebugEnabled())
            stopWatch.start();
        primes = new boolean[SIEVE_SIZE];
        Arrays.fill(primes, true);
        primes[0] = primes[1] = false;
        for (int index = 2; index <= MAX_FACTOR_SIZE; index++)
            sieve(primes, SIEVE_SIZE, index);
        for (int index = 0; index < primes.length; index++)
            if (primes[index])
                primeValueCache.add(index);
        if (LOG.isDebugEnabled())
            stopWatch.stop();
        initialised = true;
        LOG.debug("Harvest operation duration(ms): {}", format.format(stopWatch.getLastTaskTimeMillis()));
        LOG.debug("Prime cache initialised with {} values at {} p/s.",
                format.format(primes.length),
                format.format(Math.round(primes.length / (stopWatch.getLastTaskTimeMillis() / 1000))));

        return this;
    }

    private void sieve(final boolean[] primes, final int sieveSize, final int rhsFactor) {
        for (int lhsFactor = 2; lhsFactor * rhsFactor < sieveSize; lhsFactor++)
            primes[lhsFactor * rhsFactor] = false;
    }
}