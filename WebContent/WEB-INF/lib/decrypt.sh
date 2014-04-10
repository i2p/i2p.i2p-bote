#!/bin/sh
java -cp $I2P/lib/i2p.jar:`dirname $0`/i2pbote.jar:`dirname $0`/scrypt-1.4.0.jar i2p.bote.fileencryption.Decrypt $@
