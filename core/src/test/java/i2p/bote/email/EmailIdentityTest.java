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

package i2p.bote.email;

import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class EmailIdentityTest {
    List<String> base64Strings;

    @Before
    public void setUp() throws Exception {
        base64Strings = new ArrayList<String>();
        String base64;
        
        // Add a ElGamal-2048 / DSA-1024 identity
        base64 = "rc5BsbTqxXuOviM266BxqknC0FBzcCJ~G55Y-4lf~u1AqDF2ciN-3Cc-msUDztzIvW~l4FENKz~k4t5kScRGQBxC-xQgBgrFpMmzec6LyATU65Dmn9XC2IKL1JNwl1f8YH-r1iQX2qYmXhZA9YaLxD0D4~DNCjWI4XGjo2iIhat3Lm8pKPwyZ~TFQlSFQR9V-IFf-ngmRCFsjb5Fjj5~VsY1jPJOFkQHIcJMMEg4Ivo07KKmrr-7GLY~cWz7-87fImGjGtqeCQ9M~EbyFZCtQ7ELAoJacTHnnXOZY0VdURqLnzp8vU9yopHwar5EqnOmGW9MG2WzcP74mUI~f9Fxjlcol8Qr~bJ3sdQ8RYxsJ~IKn83bakW26iMSLpvI-YT1sGyCBYVR0wBx70PzmQE9JfXGmXxJIf8YICwAZUOLp~VbXWVhKHdXkvNYKMRI1-B4fSpCNfHhjKOfDpoF6HbEHByudeYnBgDMK~Upme7-nVymUAU0SkN5wGzbIDqUqC5OAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADQSYVIEvrZrois~MiowIbkvEHS~oZYHa27oQvVwIkpjmpjIpu1irZ4f-o606qu1W-";
        base64Strings.add(base64);
        
        // Add an ECDH-521 / ECDSA-521 identity
        base64 = "-eLBNw5XZXh2qUxBn35vX9jBtdbAbUy9wM3D8v5ZAtLO3kmYOjoWdeh~4O-Lp6~ucsvCo0DBD9wqcOJgSDUrMl4IT3JgddfOYs7479NyC6ZpYlqx5Dan1G~0UgeWswd~Z0mkLXpuccNBgJJt0sbRadnCHlpxHSFbDkJ2Aohro-c8bHen9V~pEaj2hJWlo6ycp-be0hyM1nHdYG7bKUVUeV6A-dHLmRQW83HRhzO8-LeYNqXBzTFtxw47plcJbZRvhH8RcLhHp5f9grSHdIy2q7ptXHjXbRc8jdhc7lRxqf1WHcJKtBhqkBmS3EfGrMDBxETZRASXRXM8YE0QgAWVm4xU1XMc";
        base64Strings.add(base64);
    }

    @Test
    public void toBase64AndBack() throws GeneralSecurityException {
        for (int i=0; i<base64Strings.size(); i++) {
            String before = base64Strings.get(i);
            String after = new EmailIdentity(before).getFullKey();
            assertEquals(before, after);
        }
    }
}