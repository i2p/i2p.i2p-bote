package i2p.bote.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;

import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;

import i2p.bote.Configuration;
import i2p.bote.TestUtil;
import i2p.bote.email.Email;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Identities;
import i2p.bote.folder.Outbox;
import i2p.bote.folder.RelayPacketFolder;
import i2p.bote.network.DHT;
import i2p.bote.network.NetworkStatusSource;
import i2p.bote.packet.dht.DhtStorablePacket;
import i2p.bote.packet.dht.EncryptedEmailPacket;
import i2p.bote.packet.dht.IndexPacket;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class OutboxProcessorTest {
    @Rule public MockitoRule rule = MockitoJUnit.rule();

    @Mock DHT dht;
    @Mock Outbox outbox;
    @Mock RelayPeerManager peerManager;
    @Mock RelayPacketFolder relayPacketFolder;
    @Mock Identities identities;
    @Mock Configuration configuration;
    @Mock NetworkStatusSource networkStatusSource;

    OutboxProcessor op;
    Email testEmail;

    @Before
    public void setUp() {
        op = new OutboxProcessor(
                dht,
                outbox,
                peerManager,
                relayPacketFolder,
                identities,
                configuration,
                networkStatusSource);
    }

    @Test
    public void testSendEmail() throws Exception {
        EmailIdentity identity = TestUtil.createTestIdentities().get(0).identity;
        String address = "tester <" + identity.getKey() + ">";
        when(identities.extractIdentity(address)).thenReturn(identity);

        testEmail = new Email(true);
        testEmail.setFrom(new InternetAddress(address));
        testEmail.addRecipient(RecipientType.TO, new InternetAddress("Erika Mustermann <m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UY>"));
        testEmail.setSubject("Test", "UTF-8");
        testEmail.setText("foobar");

        op.sendEmail(testEmail);

        ArgumentCaptor<DhtStorablePacket> arg = ArgumentCaptor.forClass(DhtStorablePacket.class);
        verify(dht, times(2)).store(arg.capture());
        List<DhtStorablePacket> values = arg.getAllValues();
        assertTrue(values.get(0) instanceof EncryptedEmailPacket);
        assertTrue(values.get(1) instanceof IndexPacket);
        assertTrue(((IndexPacket)values.get(1)).contains(((EncryptedEmailPacket)values.get(0)).getDhtKey()));
    }

    @Test
    public void testSendAnonymousEmail() throws Exception {
        testEmail = new Email(true);
        testEmail.setFrom(new InternetAddress("anonymous"));
        testEmail.addRecipient(RecipientType.TO, new InternetAddress("Erika Mustermann <m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UY>"));
        testEmail.setSubject("Test", "UTF-8");
        testEmail.setText("foobar");

        op.sendEmail(testEmail);

        ArgumentCaptor<DhtStorablePacket> arg = ArgumentCaptor.forClass(DhtStorablePacket.class);
        verify(dht, times(2)).store(arg.capture());
        List<DhtStorablePacket> values = arg.getAllValues();
        assertTrue(values.get(0) instanceof EncryptedEmailPacket);
        assertTrue(values.get(1) instanceof IndexPacket);
        assertTrue(((IndexPacket)values.get(1)).contains(((EncryptedEmailPacket)values.get(0)).getDhtKey()));
    }
}
