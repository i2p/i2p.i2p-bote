package i2p.bote.addressbook;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.SortedSet;

import i2p.bote.TestUtil;
import i2p.bote.fileencryption.PasswordHolder;
import i2p.bote.packet.dht.Contact;

import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AddressBookTest {
    private File testDir;
    private File addressBookFile;
    private PasswordHolder passwordHolder;
    private AddressBook addressBook;

    @Before
    public void setUp() throws Exception {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tmpDir, "AddressBookTest-" + System.currentTimeMillis());
        addressBookFile = new File(testDir, "addressBook");
        assertTrue("Can't create directory: " + testDir.getAbsolutePath(), testDir.mkdir());
        passwordHolder = TestUtil.createPasswordCache(testDir);

        addressBook = new AddressBook(addressBookFile, passwordHolder);
        for (TestUtil.TestIdentity identity : TestUtil.createTestIdentities())
            addressBook.add(new Contact(identity.identity.getPublicName(), identity.identity));
    }

    @After
    public void tearDown() throws Exception {
        // Current tests don't cause this to be written out
        //assertTrue("Can't delete file: " + addressBookFile.getAbsolutePath(), addressBookFile.delete());
        File derivParamsFile = TestUtil.createConfiguration(testDir).getKeyDerivationParametersFile();
        assertTrue("Can't delete file: " + derivParamsFile, derivParamsFile.delete());
        assertTrue("Can't delete directory: " + testDir.getAbsolutePath(), testDir.delete());
    }

    @Test
    public void testExportImport() throws Exception {
        File exportFile = new File(testDir, "ExportImportTest-" + System.currentTimeMillis() + ".txt");
        addressBook.export(exportFile, null);

        File tmpAddressBookFile = new File(testDir, "ExportImportAB-" + System.currentTimeMillis());
        AddressBook tmpAddressBook = new AddressBook(tmpAddressBookFile, passwordHolder);
        FileInputStream fis = new FileInputStream(exportFile);
        try {
            tmpAddressBook.importFromFileDescriptor(fis.getFD(), null, false, false);

            SortedSet<Contact> start = addressBook.getAll();
            SortedSet<Contact> end = tmpAddressBook.getAll();

            for (Contact contact : start) {
                assertThat(contact, isIn(end));
            }
            for (Contact contact : end) {
                assertThat(contact, isIn(start));
            }
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
            }
            assertTrue("Can't delete file: " + exportFile.getAbsolutePath(), exportFile.delete());
            assertTrue("Can't delete file: " + tmpAddressBookFile.getAbsolutePath(), tmpAddressBookFile.delete());
        }
    }
}
