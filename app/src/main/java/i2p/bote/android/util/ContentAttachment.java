package i2p.bote.android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Locale;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Part;

import i2p.bote.Util;
import i2p.bote.android.Constants;
import i2p.bote.android.R;
import i2p.bote.email.Attachment;

public class ContentAttachment implements Attachment {
    private ParcelFileDescriptor mAttachmentPFD;
    private String mFileName;
    private long mSize;
    private DataHandler mDataHandler;

    public ContentAttachment(ContentResolver cr, Uri uri) throws FileNotFoundException {
        // Get the content resolver instance for this context, and use it
        // to get a ParcelFileDescriptor for the file.
        mAttachmentPFD = cr.openFileDescriptor(uri, "r");
        // If we get to here, the file exists

        Cursor returnCursor = cr.query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        mFileName = returnCursor.getString(nameIndex);
        mSize = returnCursor.getLong(sizeIndex);
        returnCursor.close();

        // Get a regular file descriptor for the file
        final FileDescriptor fd = mAttachmentPFD.getFileDescriptor();
        final String mimeType = cr.getType(uri);
        mDataHandler = new DataHandler(new DataSource() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(fd);
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                throw new IOException("Cannot write to attachments");
            }

            @Override
            public String getContentType() {
                return mimeType;
            }

            @Override
            public String getName() {
                return mFileName;
            }
        });
    }

    public ContentAttachment(final Part part) throws IOException, MessagingException {
        mFileName = part.getFileName();
        mSize = Util.getPartSize(part);
        mDataHandler = part.getDataHandler();
    }

    @Override
    public String getFileName() {
        return mFileName;
    }

    public long getSize() {
        return mSize;
    }

    public String getHumanReadableSize(Context context) {
        int unit = (63-Long.numberOfLeadingZeros(mSize)) / 10;   // 0 if totalBytes<1K, 1 if 1K<=totalBytes<1M, etc.
        double value = (double)mSize / (1<<(10*unit));
        int formatStr;
        switch (unit) {
            case 0: formatStr = R.string.n_bytes; break;
            case 1: formatStr = R.string.n_kilobytes; break;
            default: formatStr = R.string.n_megabytes;
        }
        NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
        if (value < 100)
            formatter.setMaximumFractionDigits(1);
        else
            formatter.setMaximumFractionDigits(0);
        return context.getString(formatStr, formatter.format(value));
    }

    @Override
    public DataHandler getDataHandler() {
        return mDataHandler;
    }

    @Override
    public boolean clean() {
        if (mAttachmentPFD == null)
            return true;

        try {
            mAttachmentPFD.close();
            return true;
        } catch (IOException e) {
            Log.e(Constants.ANDROID_LOG_TAG, "Can't close ParcelFileDescriptor: <" + mFileName + ">", e);
            return false;
        }
    }
}
