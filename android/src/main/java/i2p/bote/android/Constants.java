package i2p.bote.android;

public class Constants {
    public static final String ANDROID_LOG_TAG = "I2P-Bote";

    public static final String SHARED_PREFS = "i2p.bote";
    public static final String PREF_SELECTED_IDENTITY = "selectedIdentity";

    public static final String EMAILDEST_SCHEME = "bote";

    public static final String NDEF_DOMAIN = "i2p.bote";
    public static final String NDEF_TYPE_CONTACT = "contact";
    public static final String NDEF_TYPE_CONTACT_DESTINATION = "contactDestination";
    public static final String NDEF_LEGACY_TYPE_CONTACT = NDEF_DOMAIN + ":" + NDEF_TYPE_CONTACT;
    public static final String NDEF_LEGACY_TYPE_CONTACT_DESTINATION = NDEF_DOMAIN + ":" + NDEF_TYPE_CONTACT_DESTINATION;
}
