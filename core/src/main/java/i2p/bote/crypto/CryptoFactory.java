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

import java.lang.reflect.Constructor;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.i2p.util.Log;

public class CryptoFactory {
    static {
        if (Security.getProvider("BC") == null) {
            try {
                Class<?> cls = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
                Constructor<?> con = cls.getConstructor(new Class[0]);
                Provider bc = (Provider)con.newInstance(new Object[0]);
                Security.addProvider(bc);
            } catch (Exception e) {
            }
        }
    }

    private static List<CryptoImplementation> instances;

    public synchronized static CryptoImplementation getInstance(int id) {
        if (instances == null)
            init();
        
        for (CryptoImplementation instance: instances)
            if (instance.getId() == id)
                return instance;
        return null;
    }
    
    public synchronized static List<CryptoImplementation> getInstances() {
        if (instances == null)
            init();
        
        return instances;
    }
    
    private static void init() {
        instances = Collections.synchronizedList(new ArrayList<CryptoImplementation>());
        Log log = new Log(CryptoFactory.class);
        try {
            instances.add(new ElGamal2048_DSA1024());
        } catch (GeneralSecurityException e) {
            log.error("Error creating ElGamal2048_DSA1024.", e);
        }
        try {
            instances.add(new ECDH256_ECDSA256());
            instances.add(new ECDH521_ECDSA521());
        }
        catch (GeneralSecurityException e) {
            log.error("Error creating ECDH256_ECDSA256 or ECDH521_ECDSA521.", e);
        }
        try {
            instances.add(new NTRUEncrypt1087_GMSS512());
        } catch (GeneralSecurityException e) {
            log.error("Error creating NTRUEncrypt1087_GMSS512.", e);
        }
    }
}