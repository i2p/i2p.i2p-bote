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

package i2p.bote.fileencryption;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Contains parameters specific to the <code>scrypt</code> key derivation function.
 * @see FileEncryptionUtil
 */
public class SCryptParameters {
    public final int N;
    public final int r;
    public final int p;

    /**
     * @param N CPU cost parameter
     * @param r Memory cost parameter
     * @param p Parallelization parameter
     */
    public SCryptParameters(int N, int r, int p) {
        this.N = N;
        this.r = r;
        this.p = p;
    }

    public SCryptParameters(InputStream input) throws IOException {
        this(new DataInputStream(input));
    }
    
    public SCryptParameters(DataInputStream input) throws IOException {
        N = input.readInt();
        r = input.readInt();
        p = input.readInt();
    }
    
    public void writeTo(OutputStream output) throws IOException {
        DataOutputStream dataStream = new DataOutputStream(output);
        dataStream.writeInt(N);
        dataStream.writeInt(r);
        dataStream.writeInt(p);
    }
    
    @Override
    public boolean equals(Object anotherObject) {
        if (anotherObject == null)
            return false;
        if (!(anotherObject.getClass() == getClass()))
            return false;
        SCryptParameters otherParams = (SCryptParameters)anotherObject;
        
        return otherParams.N==N && otherParams.r==r && otherParams.p==p;
    }
    
    /** Overridden because <code>equals</code> is overridden */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + N;
        result = prime * result + p;
        result = prime * result + r;
        return result;
    }
}