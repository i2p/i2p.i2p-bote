package i2p.bote.io;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public class FileEncryptionTestUtil {
    
    public static DerivedKey deriveKey(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = new byte[FileEncryptionConstants.SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] keyBytes = FileEncryptionUtil.getEncryptionKey(password, salt, FileEncryptionConstants.NUM_ITERATIONS);
        return new DerivedKey(salt, FileEncryptionConstants.NUM_ITERATIONS, keyBytes);
    }
}