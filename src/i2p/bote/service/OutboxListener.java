package i2p.bote.service;

import i2p.bote.email.Email;

public interface OutboxListener {

    void emailSent(Email email);
}