package i2p.bote.folder;

/** Interface for folders whose content expires */
public interface ExpirationListener {
    final static long EXPIRATION_TIME_MILLISECONDS = 100 * 24 * 3600 * 1000L;   // keep for up to 100 days

    /** Deletes any expired content */
    void deleteExpired();
}