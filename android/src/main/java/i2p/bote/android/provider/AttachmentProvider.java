package i2p.bote.android.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.mail.MessagingException;
import javax.mail.Part;

import i2p.bote.Util;
import i2p.bote.android.BuildConfig;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;

public class AttachmentProvider extends ContentProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".attachmentprovider";

    private static final int RAW_ATTACHMENT = 1;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, "*/*/#/RAW", RAW_ATTACHMENT);
    }

    private final static String[] OPENABLE_PROJECTION = {
            OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};

    public static Uri getUriForAttachment(String folderName, String messageId, int partNum) {
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(folderName)
                .appendPath(messageId)
                .appendPath(Integer.toString(partNum))
                .appendPath("RAW")
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (sUriMatcher.match(uri) == UriMatcher.NO_MATCH)
            throw new IllegalArgumentException("Invalid URI: " + uri);
        if (projection == null) {
            projection = OPENABLE_PROJECTION;
        }

        final MatrixCursor cursor = new MatrixCursor(projection, 1);
        MatrixCursor.RowBuilder b = cursor.newRow();

        try {
            Part attachment = getAttachment(uri);
            if (attachment == null)
                return null;

            for (String col : projection) {
                switch (col) {
                    case OpenableColumns.DISPLAY_NAME:
                        b.add(attachment.getFileName());
                        break;
                    case OpenableColumns.SIZE:
                        b.add(Util.getPartSize(attachment));
                        break;
                }
            }
        } catch (PasswordException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        if (sUriMatcher.match(uri) != UriMatcher.NO_MATCH) {
            try {
                Part attachment = getAttachment(uri);
                if (attachment != null) {
                    String contentType = attachment.getContentType();
                    // Remove any "; name=fileName" suffix
                    int delim = contentType.indexOf(';');
                    if (delim >= 0) {
                        String params = contentType.substring(delim + 1);
                        contentType = contentType.substring(0, delim);

                        // Double-check in case the attachment was created by an old
                        // I2P-Bote version that didn't detect MIME types correctly.
                        if ("application/octet-stream".equals(contentType)) {
                            // Find the filename
                            String filename = "";
                            delim = params.indexOf("name=");
                            if (delim >= 0) {
                                filename = params.substring(delim + 5);
                                delim = filename.indexOf(' ');
                                if (delim >= 0)
                                    filename = params.substring(0, delim);
                            }

                            if (!filename.isEmpty()) {
                                MimetypesFileTypeMap mimeTypeMap = new MimetypesFileTypeMap();
                                contentType = mimeTypeMap.getContentType(filename);
                            }
                        }
                    }
                    return contentType;
                }
            } catch (PasswordException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (sUriMatcher.match(uri) == UriMatcher.NO_MATCH)
            throw new FileNotFoundException("Invalid URI: " + uri);
        if (!"r".equals(mode))
            throw new FileNotFoundException("Attachments can only be read");

        ParcelFileDescriptor[] pipe;
        try {
            pipe = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Exception opening pipe", e);
            throw new FileNotFoundException("Could not open pipe for: "
                    + uri.toString());
        }

        try {
            Part attachment = getAttachment(uri);
            if (attachment == null)
                throw new FileNotFoundException("Unknown email or attachment for URI " + uri);

            new TransferThread(attachment.getInputStream(),
                    new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])).start();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Exception accessing attachment", e);
            throw new FileNotFoundException("Exception accessing attachment: " + e.getLocalizedMessage());
        }

        return pipe[0];
    }

    static class TransferThread extends Thread {
        InputStream in;
        OutputStream out;

        TransferThread(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buf = new byte[8192];
            int len;

            try {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "Exception transferring file", e);
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    private Part getAttachment(Uri uri) throws PasswordException, IOException, MessagingException {
        List<String> segments = uri.getPathSegments();
        String folderName = segments.get(0);
        String messageId = segments.get(1);
        int partNum = Integer.valueOf(segments.get(2));

        Email email = BoteHelper.getEmail(folderName, messageId);
        if (email != null) {
            if (partNum >= 0 && partNum < email.getParts().size())
                return email.getParts().get(partNum);
        }
        return null;
    }
}
