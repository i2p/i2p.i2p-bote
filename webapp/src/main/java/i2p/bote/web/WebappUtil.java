package i2p.bote.web;

import net.i2p.I2PAppContext;
import net.i2p.util.Translate;

public class WebappUtil {
    private static final String BUNDLE_NAME = "i2p.bote.locale.Messages";

    public static String _t(String messageKey) {
        return Translate.getString(messageKey, I2PAppContext.getGlobalContext(), BUNDLE_NAME);
    }

    public static String _t(String messageKey, Object parameter) {
        return Translate.getString(messageKey, parameter, I2PAppContext.getGlobalContext(), BUNDLE_NAME);
    }

    public static String _t(String messageKey, Object parameter1, Object parameter2) {
        return Translate.getString(messageKey, parameter1, parameter2, I2PAppContext.getGlobalContext(), BUNDLE_NAME);
    }
}
