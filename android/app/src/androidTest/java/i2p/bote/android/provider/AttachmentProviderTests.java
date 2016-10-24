package i2p.bote.android.provider;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;

import i2p.bote.I2PBote;
import i2p.bote.android.InitActivities;
import i2p.bote.android.R;
import i2p.bote.android.util.BoteHelper;
import i2p.bote.android.util.ContentAttachment;
import i2p.bote.email.Attachment;
import i2p.bote.email.Email;
import i2p.bote.folder.EmailFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AttachmentProviderTests extends ProviderTestCase2<AttachmentProvider> {
    private static final String URI_PREFIX = "content://" + AttachmentProvider.AUTHORITY;
    private static final String INVALID_URI = URI_PREFIX + "/invalid";
    private static final String NO_MATCH_URI = URI_PREFIX + "/foo/bar/1/RAW";

    public AttachmentProviderTests() {
        super(AttachmentProvider.class, AttachmentProvider.AUTHORITY);
        setContext(InstrumentationRegistry.getTargetContext());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testAndroidTestCaseSetupProperly();
        new InitActivities(getMockContext().getDir("botetest", Context.MODE_PRIVATE).getAbsolutePath()).initialize();
    }

    @Test(expected = IllegalArgumentException.class)
    public void queryWithInvalidUriThrows() {
        Uri invalidUri = Uri.parse(INVALID_URI);
        getMockContentResolver().query(invalidUri, null, null, null, null);
    }

    @Test
    public void getTypeWithInvalidUriReturnsNull() {
        Uri invalidUri = Uri.parse(INVALID_URI);
        String type = getMockContentResolver().getType(invalidUri);
        assertThat("Type was not null", type, is(nullValue()));
    }

    @Test(expected = FileNotFoundException.class)
    public void openFileWithInvalidUriThrows() throws FileNotFoundException {
        Uri invalidUri = Uri.parse(INVALID_URI);
        getMockContentResolver().openFileDescriptor(invalidUri, "r");
    }

    @Test
    public void queryWithNoMatchReturnsNull() {
        Uri noMatchUri = Uri.parse(NO_MATCH_URI);
        Cursor c = getMockContentResolver().query(noMatchUri, null, null, null, null);
        assertThat(c, is(nullValue()));
    }

    @Test
    public void queryWithValidTextUri() throws Exception {
        ContentAttachment attachment = createTextAttachment();
        Uri uri = createEmailWithAttachment(attachment);

        Cursor c = getMockContentResolver().query(uri, null, null, null, null);
        assertThat(c.getCount(), is(1));
        int nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = c.getColumnIndex(OpenableColumns.SIZE);
        c.moveToFirst();
        assertThat(c.getString(nameIndex), is(equalTo(attachment.getFileName())));
        assertThat(c.getLong(sizeIndex), is(equalTo(attachment.getSize())));
    }

    @Test
    public void getTypeWithValidTextUri() throws Exception {
        ContentAttachment attachment = createTextAttachment();
        Uri uri = createEmailWithAttachment(attachment);
        String type = getMockContentResolver().getType(uri);
        assertThat("Type was not correct", type, is("text/plain"));
    }

    @Test
    public void getTypeWithValidImageUri() throws Exception {
        ContentAttachment attachment = createImageAttachment();
        Uri uri = createEmailWithAttachment(attachment);
        String type = getMockContentResolver().getType(uri);
        assertThat("Type was not correct", type, is("image/png"));
    }

    @Test
    public void openFileWithValidTextUri() throws Exception {
        ContentAttachment attachment = createTextAttachment();
        Uri uri = createEmailWithAttachment(attachment);
        openFileWithValidUri(attachment, uri);
    }

    @Test
    public void openFileWithValidImageUri() throws Exception {
        ContentAttachment attachment = createImageAttachment();
        Uri uri = createEmailWithAttachment(attachment);
        openFileWithValidUri(attachment, uri);

        assertThat("Image could not be decoded",
                BitmapFactory.decodeStream(getMockContentResolver().openInputStream(uri)),
                is(notNullValue()));
    }

    private void openFileWithValidUri(ContentAttachment attachment, Uri uri) throws Exception {
        InputStream inProv = getMockContentResolver().openInputStream(uri);
        InputStream inOrig = attachment.getDataHandler().getInputStream();
        ByteArrayOutputStream outProv = new ByteArrayOutputStream();
        ByteArrayOutputStream outOrig = new ByteArrayOutputStream();
        BoteHelper.copyStream(inProv, outProv);
        BoteHelper.copyStream(inOrig, outOrig);

        assertThat("Provider content does not match original content",
                outProv.toByteArray(), is(equalTo(outOrig.toByteArray())));
    }

    private ContentAttachment createTextAttachment() throws Exception {
        return createAttachment("test.txt", "text/plain", "Test file content".getBytes());
    }

    private ContentAttachment createImageAttachment() throws Exception {
        Bitmap bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.intro_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        return createAttachment("intro_1.png", "image/png", out.toByteArray());
    }

    private ContentAttachment createAttachment(final String fileName, final String mimeType, final byte[] content) throws Exception {
        Part part = new MimeBodyPart();
        part.setDataHandler(new DataHandler(new DataSource() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(content);
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
                return fileName;
            }
        }));
        part.setFileName(fileName);
        return new ContentAttachment(getMockContext(), part);
    }

    private Uri createEmailWithAttachment(Attachment attachment) throws Exception {
        List<Attachment> attachments = new ArrayList<Attachment>();
        attachments.add(attachment);
        Email email = new Email(false);
        email.setContent("", attachments);
        I2PBote.getInstance().getInbox().add(email);
        return AttachmentProvider.getUriForAttachment(
                I2PBote.getInstance().getInbox().getName(),
                email.getMessageID(),
                1
        );
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        EmailFolder inbox = I2PBote.getInstance().getInbox();
        for (Email email : BoteHelper.getEmails(inbox, null, true)) {
            inbox.delete(email.getMessageID());
        }
        System.setProperty("i2pbote.initialized", "false");
    }
}
