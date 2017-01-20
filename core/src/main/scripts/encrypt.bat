REM This script only works if I2P is installed in the standard directory
@java -cp "%PROGRAMFILES%\\i2p\\lib\\i2p.jar;i2pbote.jar;scrypt-1.4.0.jar" i2p.bote.fileencryption.Encrypt %*
