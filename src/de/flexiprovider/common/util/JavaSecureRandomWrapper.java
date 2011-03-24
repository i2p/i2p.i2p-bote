package de.flexiprovider.common.util;

import de.flexiprovider.api.SecureRandom;

public class JavaSecureRandomWrapper extends SecureRandom {

    private java.security.SecureRandom javaRand;

    public JavaSecureRandomWrapper(java.security.SecureRandom javaRand) {
	this.javaRand = javaRand;
    }

    public byte[] generateSeed(int numBytes) {
	return javaRand.generateSeed(numBytes);
    }

    public void nextBytes(byte[] bytes) {
	javaRand.nextBytes(bytes);
    }

    public void setSeed(byte[] seed) {
	javaRand.setSeed(seed);
    }

}
