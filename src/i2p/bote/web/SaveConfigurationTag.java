package i2p.bote.web;

import i2p.bote.I2PBote;

import javax.servlet.jsp.tagext.SimpleTagSupport;

public class SaveConfigurationTag extends SimpleTagSupport {

    public void doTag() {
        I2PBote.getInstance().getConfiguration().save();
    }
}