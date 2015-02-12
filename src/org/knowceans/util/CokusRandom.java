/*
 * Copyright (c) 2006 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knowceans.util;

import java.util.Random;

/**
 * CokusRandom is a non-static version of the Cokus Mersenne Twister as a Random
 * subclass. (Based on the C code used in LDA-C).
 * <p>
 * Note: nextUnsignedInt(), next(), and randDouble() are tested for conformity
 * with Cokus, which means they provide sufficient randomness. The other methods
 * should yet be used with caution, esp. if long random sequences are to be
 * generated (Monte Carlo simulation etc.).
 * <p>
 * TODO: Esp. next(int) needs to be tested against randomness criteria;
 * suspected Markov property seen in nextBoolean(). As point of approach,
 * randDouble() and next() use the original Cokus algorithm and can be used as a
 * reference to test randomness against the nextDouble() and nextInt() method
 * that use the scaling mechanism of Random in connection with Cokus's next(int)
 * as a random number generator.
 * 
 * @author heinrich
 */
public class CokusRandom extends Random {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3256726169322797365L;

    public static final long DEFAULTSEED = 4357;

    /* Period parameters */
    /**
     * length of state vector
     */
    public static int N = 624;

    /**
     * a period parameter
     */
    public static int M = 397;

    /**
     * a magic constant
     */
    public static long K = 0x9908b0dfL;

    /**
     * state vector
     */
    long[] state;

    /**
     * next random value is computed from here (java: index into state[])
     */
    int next;

    /**
     * can *next++ this many times before reloading
     */
    int left = -1;

    public static void main(String[] args) {

        CokusRandom r = new CokusRandom(4357);

        for (int i = 0; i < 50; i++) {
            System.out.println(r.nextDouble());
        }
        System.out.println();
        for (int i = 0; i < 50; i++) {
            System.out.println(r.randDouble());
        }
    }

    /**
     * initialise with standard MT seed, 4357.
     */
    public CokusRandom() {
        this(4357);
    }

    /**
     * sets the seed (lower half of long taken)
     * 
     * @param i
     */
    public CokusRandom(long i) {
        // why not executed here?
        state = new long[N + 1];
        setSeed(i);
    }

    /**
     * mask all but highest bit of u (uint32)
     * 
     * @param u
     * @return
     */
    protected long hiBit(long u) {
        return (u) & 0x80000000l;
    }

    /**
     * mask all but lowest bit of u (uint32)
     * 
     * @param u
     * @return
     */
    protected long loBit(long u) {
        return (u) & 0x00000001l;
    }

    /**
     * mask the highest bit of u (uint32)
     * 
     * @param u
     * @return
     */
    protected static long loBits(long u) {
        return (u) & 0x7FFFFFFFl;
    }

    /**
     * move hi bit of u to hi bit of v (uint32)
     * 
     * @param u
     * @param v
     * @return
     */
    protected long mixBits(long u, long v) {
        return hiBit(u) | loBits(v);
    }

    /**
     * generate a random number with the number of bits in the argument
     * 
     * @param bits (max 32)
     */
    protected int next(int bits) {
        int a = next();
        return a >>> (32 - bits);
    }

    /**
     * returns the next integer
     * 
     * @return
     */
    public int next() {
        long y;

        if (--left < 0)
            return (reload());

        y = state[next++];
        y ^= (y >> 11);
        y ^= (y << 7) & 0x9D2C5680l;
        y ^= (y << 15) & 0xEFC60000l;
        return (int) (y ^ (y >> 18));
    }

    /**
     * returns a long in the numerical interval of an unsigned int.
     * 
     * @return
     */
    public long nextUnsignedInt() {
        return next() & 0xFFFFFFFFl;
    }

    /**
     * returns the next double between 0 and 1.
     * 
     * @return
     */
    public double randDouble() {
        return (next() & 0xffffffffl) / (double) 0x100000000l;
    }

    /**
     * Note: only lower half of seed is used.
     */
    public void setSeed(long seed) {
        // super.setSeed(seed);
        long x = (seed | 1) & 0xFFFFFFFFl;
        // why doesn't this work in the constructor?
        if (state == null) {
            state = new long[N + 1];
        }
        // long s = state;
        long[] s = state;
        int si = 0;
        int j;

        // s++ is done with an array
        left = 0;
        s[si++] = x;
        for (j = N; j > 0; --j) {
            s[si] = (x *= 69069);
            s[si] &= 0xFFFFFFFFl;
            si++;
        }
    }

    /**
     * reload the random number buffer
     * 
     * @return
     */
    public int reload() {
        int p0 = 0;
        int p2 = 2;
        int pM = M;
        long s0, s1;

        int j;

        if (left < -1)
            setSeed(4357);

        left = N - 1;
        next = 1;

        // for(s0=state[0], s1=state[1], j=N-M+1; --j; s0=s1, s1=*p2++)
        // *p0++ = *pM++ ^ (mixBits(s0, s1) >> 1) ^ (loBit(s1) ? K : 0U);

        for (s0 = state[0], s1 = state[1], j = N - M + 1; --j != 0; s0 = s1, s1 = state[p2++]) {
            state[p0++] = state[pM++] ^ (mixBits(s0, s1) >> 1)
                ^ (loBit(s1) != 0 ? K : 0);
        }

        // for(pM=state, j=M; --j; s0=s1, s1=*p2++)
        // *p0++ = *pM++ ^ (mixBits(s0, s1) >> 1) ^ (loBit(s1) ? K : 0U);

        for (pM = 0, j = M; --j != 0; s0 = s1, s1 = state[p2++]) {
            state[p0++] = state[pM++] ^ (mixBits(s0, s1) >> 1)
                ^ (loBit(s1) != 0 ? K : 0);
        }

        s1 = state[0];
        state[p0] = state[pM] ^ (mixBits(s0, s1) >> 1)
            ^ (loBit(s1) != 0 ? K : 0);

        s1 ^= (s1 >> 11);
        s1 ^= (s1 << 7) & 0x9D2C5680l;
        s1 ^= (s1 << 15) & 0xEFC60000l;

        return (int) (s1 ^ (s1 >> 18));
    }

    // reimplementation of the random interface
    @Override
    public double nextDouble() {
        return randDouble();
    }

    @Override
    public boolean nextBoolean() {
        return randDouble() >= 0.5;
    }

    @Override
    public float nextFloat() {
        return (float) randDouble();
    }

    protected boolean haveNextNextGaussian = false;

    protected double nextNextGaussian;

    public double lastRand;

    @Override
    public synchronized double nextGaussian() {
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        } else {
            double v1, v2, s;
            do {
                v1 = 2 * randDouble() - 1;
                v2 = 2 * randDouble() - 1;
                s = v1 * v1 + v2 * v2;
            } while (s >= 1 || s == 0);
            double multiplier = Math.sqrt(-2 * Math.log(s) / s);
            nextNextGaussian = v2 * multiplier;
            haveNextNextGaussian = true;
            return v1 * multiplier;
        }
    }

    @Override
    public int nextInt() {
        return next() - 0x80000000;
    }

    public long nextUnsignedLong() {
        return ((long) (next()) << 32) + next();
    }

    // TODO: check long type bounds
    @Override
    public long nextLong() {
        return nextUnsignedLong() - 0x8000000000000000l;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        int numRequested = bytes.length;

        int numGot = 0, rnd = 0;

        while (true) {
            for (int i = 0; i < numRequested; i++) {
                if (numGot == numRequested)
                    return;
                rnd = (i == 0 ? next() : rnd >> 8);
                bytes[numGot++] = (byte) rnd;
            }
        }
    }

    // TODO: check int type bounds
    @Override
    public int nextInt(int n) {
        return (int) (n * randDouble());
    }
}
