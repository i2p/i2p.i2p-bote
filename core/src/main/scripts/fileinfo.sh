#!/bin/sh
java -cp $I2P/lib/i2p.jar:`dirname $0`/i2pbote.jar i2p.bote.fileencryption.FileInfo $@
