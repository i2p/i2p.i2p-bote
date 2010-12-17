package i2p.bote.io;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public interface PasswordHolder {
    
    char[] getPassword();
    
    DerivedKey getKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException;
}