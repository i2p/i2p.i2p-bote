package net.i2p.util;

/*
 * public domain
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Queue;

/**
 * bridge to android logging
 *
 * @author zzz
 */
class LogWriter implements Runnable {
    private final static long CONFIG_READ_ITERVAL = 10 * 1000;
    private long _lastReadConfig = 0;
    private long _numBytesInCurrentFile = 0;
    private OutputStream _currentOut; // = System.out
    private int _rotationNum = -1;
    private String _logFilenamePattern;
    private File _currentFile;
    private LogManager _manager;

    private boolean _write;

    private LogWriter() { // nop
    }

    public LogWriter(LogManager manager) {
        _manager = manager;
    }

    public void stopWriting() {
        _write = false;
    }

    public void run() {
        _write = true;
        try {
            while (_write) {
                flushRecords();
                if (_write)
                    rereadConfig();
            }
        } catch (Exception e) {
            System.err.println("Error writing the logs: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    public void flushRecords() { flushRecords(true); }
    public void flushRecords(boolean shouldWait) {
        try {
            // zero copy, drain the manager queue directly
            Queue<LogRecord> records = _manager.getQueue();
            if (records == null) return;
            if (!records.isEmpty()) {
                LogRecord rec;
                while ((rec = records.poll()) != null) {
                    writeRecord(rec);
                }
                try {
                    if (_currentOut != null)
                        _currentOut.flush();
                } catch (IOException ioe) {
                    //if (++_diskFullMessageCount < MAX_DISKFULL_MESSAGES)
                        System.err.println("Error writing the router log - disk full? " + ioe);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        } finally {
            if (shouldWait) {
                try {
                    synchronized (this) {
                        this.wait(10*1000);
                    }
                } catch (InterruptedException ie) { // nop
                }
            }
        }
    }

    public String currentFile() {
        return _currentFile != null ? _currentFile.getAbsolutePath() : "uninitialized";
    }

    private void rereadConfig() {
        long now = Clock.getInstance().now();
        if (now - _lastReadConfig > CONFIG_READ_ITERVAL) {
            _manager.rereadConfig();
            _lastReadConfig = now;
        }
    }

    private void writeRecord(LogRecord rec) {
        if (rec.getThrowable() == null)
            log(rec.getPriority(), rec.getSource(), rec.getSourceName(), rec.getThreadName(), rec.getMessage());
        else
            log(rec.getPriority(), rec.getSource(), rec.getSourceName(), rec.getThreadName(), rec.getMessage(), rec.getThrowable());

        // to be viewed on android screen
        String val = LogRecordFormatter.formatRecord(_manager, rec, true);
        _manager.getBuffer().add(val);
        if (rec.getPriority() >= Log.ERROR)
            _manager.getBuffer().addCritical(val);

        // we always add to the console buffer, but only sometimes write to stdout
        if (_manager.getDisplayOnScreenLevel() <= rec.getPriority()) {
            if (_manager.displayOnScreen()) {
                // android log already does time stamps, so reformat without the date
                System.out.print(LogRecordFormatter.formatRecord(_manager, rec, false));
            }
        }
    }

    private static final String ANDROID_LOG_TAG = "I2P";

    public void log(int priority, Class<?> src, String name, String threadName, String msg) {
            if (src != null) {
                String tag = src.getName();
                int dot = tag.lastIndexOf(".");
                if (dot >= 0)
                    tag = tag.substring(dot + 1);
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         tag +
                                         " [" + threadName + "] " + msg);
            } else if (name != null)
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         name +
                                         " ["  + threadName + "] " + msg);
            else
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         '[' + threadName + "] " + msg);
    }

    public void log(int priority, Class<?> src, String name, String threadName, String msg, Throwable t) {
            if (src != null) {
                String tag = src.getName();
                int dot = tag.lastIndexOf(".");
                if (dot >= 0)
                    tag = tag.substring(dot + 1);
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         tag +
                                         " [" + threadName + "] " + msg +
                                         ' ' + t.toString() + ' ' + android.util.Log.getStackTraceString(t));
            } else if (name != null)
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         name +
                                         " [" + threadName + "] " + msg +
                                         ' ' + t.toString() + ' ' + android.util.Log.getStackTraceString(t));
            else
                android.util.Log.println(toAndroidLevel(priority),
                                         ANDROID_LOG_TAG,
                                         '[' + threadName + "] " +
                                         msg + ' ' + t.toString() + ' ' + android.util.Log.getStackTraceString(t));
    }

    private static int toAndroidLevel(int level) {
        switch (level) {
        case Log.DEBUG:
            return android.util.Log.DEBUG;
        case Log.INFO:
            return android.util.Log.INFO;
        case Log.WARN:
            return android.util.Log.WARN;
        case Log.ERROR:
        case Log.CRIT:
        default:
            return android.util.Log.ERROR;
        }
    }
}
