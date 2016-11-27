package i2p.bote.web;

import net.i2p.util.Log;

import org.owasp.csrfguard.log.ILogger;
import org.owasp.csrfguard.log.LogLevel;

public class CSRFLogger implements ILogger {

    private static final long serialVersionUID = -4857601483759096198L;
    
    private static final Log LOGGER = new Log(CSRFLogger.class);

    @Override
    public void log(String msg) {
        LOGGER.info(msg.replaceAll("(\\r|\\n)", ""));
    }

    @Override
    public void log(LogLevel level, String msg) {
        // Remove CR and LF characters to prevent CRLF injection
        String sanitizedMsg = msg.replaceAll("(\\r|\\n)", "");
        
        switch(level) {
            case Trace:
                LOGGER.debug(sanitizedMsg);
                break;
            case Debug:
                LOGGER.debug(sanitizedMsg);
                break;
            case Info:
                LOGGER.info(sanitizedMsg);
                break;
            case Warning:
                LOGGER.warn(sanitizedMsg);
                break;
            case Error:
                LOGGER.error(sanitizedMsg);
                break;
            case Fatal:
                LOGGER.log(Log.CRIT, sanitizedMsg);
                break;
            default:
                throw new RuntimeException("unsupported log level " + level);
        }
    }

    @Override
    public void log(Exception exception) {
        LOGGER.warn(exception.getLocalizedMessage(), exception);
    }

    @Override
    public void log(LogLevel level, Exception exception) {
            switch(level) {
            case Trace:
                LOGGER.debug(exception.getLocalizedMessage(), exception);
                break;
            case Debug:
                LOGGER.debug(exception.getLocalizedMessage(), exception);
                break;
            case Info:
                LOGGER.info(exception.getLocalizedMessage(), exception);
                break;
            case Warning:
                LOGGER.warn(exception.getLocalizedMessage(), exception);
                break;
            case Error:
                LOGGER.error(exception.getLocalizedMessage(), exception);
                break;
            case Fatal:
                LOGGER.log(Log.CRIT, exception.getLocalizedMessage(), exception);
                break;
            default:
                throw new RuntimeException("unsupported log level " + level);
        }
    }
}
