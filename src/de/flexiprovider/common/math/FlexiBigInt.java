package de.flexiprovider.common.math;

import java.math.BigInteger;

import de.flexiprovider.api.SecureRandom;

public final class FlexiBigInt {

    private class JavaSecureRandom extends java.security.SecureRandom {
	JavaSecureRandom(SecureRandom flexiRand) {
	    super(flexiRand, null);
	}
    }

    public BigInteger bigInt;

    public FlexiBigInt(byte[] val) {
	bigInt = new BigInteger(val);
    }

    public FlexiBigInt(String val) {
	bigInt = new BigInteger(val);
    }

    public FlexiBigInt(int signum, byte[] magnitude) {
	bigInt = new BigInteger(signum, magnitude);
    }

    public FlexiBigInt(String val, int radix) {
	bigInt = new BigInteger(val, radix);
    }

    public FlexiBigInt(int numBits, SecureRandom flexiRand) {
	JavaSecureRandom javaRand = new JavaSecureRandom(flexiRand);
	bigInt = new BigInteger(numBits, javaRand);
    }

    public FlexiBigInt(int bitLength, int certainty, SecureRandom flexiRand) {
	JavaSecureRandom javaRand = new JavaSecureRandom(flexiRand);
	bigInt = new BigInteger(bitLength, certainty, javaRand);
    }

    public FlexiBigInt(BigInteger bigInt) {
	this.bigInt = bigInt;
    }

    public static FlexiBigInt valueOf(long val) {
	return new FlexiBigInt(BigInteger.valueOf(val));
    }

    public static final FlexiBigInt ZERO = new FlexiBigInt(BigInteger.ZERO);

    public static final FlexiBigInt ONE = valueOf(1);

    public FlexiBigInt add(FlexiBigInt addend) {
	return new FlexiBigInt(bigInt.add(addend.bigInt));
    }

    public FlexiBigInt subtract(FlexiBigInt minuend) {
	return new FlexiBigInt(bigInt.subtract(minuend.bigInt));
    }

    public FlexiBigInt multiply(FlexiBigInt factor) {
	return new FlexiBigInt(bigInt.multiply(factor.bigInt));
    }

    public FlexiBigInt divide(FlexiBigInt divisor) {
	return new FlexiBigInt(bigInt.divide(divisor.bigInt));
    }

    public FlexiBigInt[] divideAndRemainder(FlexiBigInt divisor) {
	BigInteger[] dar = bigInt.divideAndRemainder(divisor.bigInt);
	return new FlexiBigInt[] { new FlexiBigInt(dar[0]),
		new FlexiBigInt(dar[1]) };
    }

    public FlexiBigInt remainder(FlexiBigInt divisor) {
	return new FlexiBigInt(bigInt.remainder(divisor.bigInt));
    }

    public FlexiBigInt pow(int exponent) {
	return new FlexiBigInt(bigInt.pow(exponent));
    }

    public FlexiBigInt gcd(FlexiBigInt val) {
	return new FlexiBigInt(bigInt.gcd(val.bigInt));
    }

    public FlexiBigInt abs() {
	return new FlexiBigInt(bigInt.abs());
    }

    public FlexiBigInt negate() {
	return new FlexiBigInt(bigInt.negate());
    }

    public int signum() {
	return bigInt.signum();
    }

    public FlexiBigInt mod(FlexiBigInt modulus) {
	return new FlexiBigInt(bigInt.mod(modulus.bigInt));
    }

    public FlexiBigInt modPow(FlexiBigInt exponent, FlexiBigInt modulus) {
	return new FlexiBigInt(bigInt.modPow(exponent.bigInt, modulus.bigInt));
    }

    public FlexiBigInt modInverse(FlexiBigInt modulus) {
	return new FlexiBigInt(bigInt.modInverse(modulus.bigInt));
    }

    public FlexiBigInt shiftLeft(int n) {
	return new FlexiBigInt(bigInt.shiftLeft(n));
    }

    public FlexiBigInt shiftRight(int n) {
	return new FlexiBigInt(bigInt.shiftRight(n));
    }

    public FlexiBigInt and(FlexiBigInt val) {
	return new FlexiBigInt(bigInt.and(val.bigInt));
    }

    public FlexiBigInt or(FlexiBigInt val) {
	return new FlexiBigInt(bigInt.or(val.bigInt));
    }

    public FlexiBigInt xor(FlexiBigInt val) {
	return new FlexiBigInt(bigInt.xor(val.bigInt));
    }

    public FlexiBigInt not() {
	return new FlexiBigInt(bigInt.not());
    }

    public FlexiBigInt andNot(FlexiBigInt val) {
	return new FlexiBigInt(bigInt.andNot(val.bigInt));
    }

    public boolean testBit(int n) {
	return bigInt.testBit(n);
    }

    public FlexiBigInt setBit(int n) {
	return new FlexiBigInt(bigInt.setBit(n));
    }

    public FlexiBigInt clearBit(int n) {
	return new FlexiBigInt(bigInt.clearBit(n));
    }

    public FlexiBigInt flipBit(int n) {
	return new FlexiBigInt(bigInt.flipBit(n));
    }

    public int getLowestSetBit() {
	return bigInt.getLowestSetBit();
    }

    public int bitLength() {
	return bigInt.bitLength();
    }

    public int bitCount() {
	return bigInt.bitCount();
    }

    public boolean isProbablePrime(int certainty) {
	return bigInt.isProbablePrime(certainty);
    }

    public int compareTo(FlexiBigInt other) {
	return bigInt.compareTo(other.bigInt);
    }

    public FlexiBigInt min(FlexiBigInt other) {
	return new FlexiBigInt(bigInt.min(other.bigInt));
    }

    public FlexiBigInt max(FlexiBigInt other) {
	return new FlexiBigInt(bigInt.max(other.bigInt));
    }

    public boolean equals(Object other) {
	if (!(other instanceof FlexiBigInt)) {
	    return false;
	}
	return bigInt.equals(((FlexiBigInt) other).bigInt);
    }

    public int hashCode() {
	return bigInt.hashCode();
    }

    public String toString(int radix) {
	return bigInt.toString(radix);
    }

    public String toString() {
	return bigInt.toString();
    }

    public byte[] toByteArray() {
	return bigInt.toByteArray();
    }

    public int intValue() {
	return bigInt.intValue();
    }

    public long longValue() {
	return bigInt.longValue();
    }

    public float floatValue() {
	return bigInt.floatValue();
    }

    public double doubleValue() {
	return bigInt.doubleValue();
    }

}
