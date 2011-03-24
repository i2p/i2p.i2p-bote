package de.flexiprovider.pki;

import de.flexiprovider.api.keys.KeySpec;

public interface EncodedKeySpec extends KeySpec {

    byte[] getEncoded();

    String getFormat();

}
