/**
 * Copyright (C) 2009  HungryHobo@mail.i2p
 * 
 * The GPG fingerprint for HungryHobo@mail.i2p is:
 * 6DD3 EAA2 9990 29BC 4AD2 7486 1E2C 7B61 76DC DC12
 * 
 * This file is part of I2P-Bote.
 * I2P-Bote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * I2P-Bote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 */

package i2p.bote.crypto;

import net.i2p.data.Base64;
import net.i2p.util.Log;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * TODO document the 66-byte format
 */
public class ECDH521_ECDSA521 extends ECDH_ECDSA {
    private Log log = new Log(ECDH521_ECDSA521.class);

    public ECDH521_ECDSA521() throws GeneralSecurityException {
        super("P-521", "SHA512withECDSA", 66);   // Use the NIST P-521 curve, also known as secp521r1
    }
    
    @Override
    public String getName() {
        return "ECDH-521 / ECDSA-521";
    }
    
    @Override
    public byte getId() {
        return 3;
    }

    @Override
    public int getBase64PublicKeyPairLength() {
        return 174;
    }
    
    @Override
    public int getBase64CompleteKeySetLength() {
        return 348;
    }
    
    @Override
    protected byte[] toByteArray(PublicKey key) {
        ECPublicKey ecKey = castToEcKey(key);
        byte[] bouncyCompressedKey = ECUtils.encodePoint(ecKey.getParams(), ecKey.getW(), true);
        
        // shorten by one byte (bouncyCompressedKey[0] is either 2 or 3, bouncyCompressedKey[1] is either 0 or 1, so they can fit in two bits)
        if (bouncyCompressedKey[0]!=2 && bouncyCompressedKey[0]!=3)
            log.error("Illegal value in encoded EC key at byte 0: " + bouncyCompressedKey[0] + ", can only be 2 or 3.");
        if (bouncyCompressedKey[1]!=0 && bouncyCompressedKey[1]!=1)
            log.error("Illegal value in encoded EC key at byte 1: " + bouncyCompressedKey[1] + ", can only be 0 or 1.");
        byte[] compressedKey = Arrays.copyOfRange(bouncyCompressedKey, 1, keyLengthBytes+1);
        compressedKey[0] |= (bouncyCompressedKey[0]-2) << 1;
        
        return compressedKey;
    }
    
    @Override
    protected ECPublicKeySpec createPublicKeySpec(byte[] encodedKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        // convert the key to the format used by BouncyCastle, which adds one byte
        byte[] bouncyCompressedKey = new byte[keyLengthBytes+1];
        System.arraycopy(encodedKey, 0, bouncyCompressedKey, 1, keyLengthBytes);
        bouncyCompressedKey[0] = (byte)((bouncyCompressedKey[1] >> 1) + 2);
        bouncyCompressedKey[1] &= 1;
        // decompress into an EC point
        ECPoint w = ECUtils.decodePoint(ecParameterSpec.getCurve(), bouncyCompressedKey);
        
        // make a public key from the public point w
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(w, ecParameterSpec);
        
        return publicKeySpec;
    }

    @Override
    public PrivateKeyPair createPrivateKeyPair(String base64) throws GeneralSecurityException {
        int base64PrivateKeyLength = getBase64PrivateKeyPairLength() / 2;
        base64 = "A" + base64.substring(0, base64PrivateKeyLength) + "A" + base64.substring(base64PrivateKeyLength);
        byte[] bytes = Base64.decode(base64);
        return createPrivateKeyPair(bytes);
    }
}
