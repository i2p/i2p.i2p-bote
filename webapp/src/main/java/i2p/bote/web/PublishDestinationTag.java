/**
 * Copyright (C) 2009  HungryHobo@mail.i2p
 * 
 * The GPG fingerprint for HungryHobo@mail.i2p is:
 * 6DD3 EAA2 9990 29BC 4AD2 7486 1E2C 7B61 76DC DC12
 * 
 * This file is part of I2P-Bote.
 * I2P-Bote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * I2P-Bote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 */

package i2p.bote.web;

import i2p.bote.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/** Publishes an email destination to the DHT. */
public class PublishDestinationTag extends SimpleTagSupport {
    private String destination;
    private String pictureFilename;
    private String text;
    
    @Override
    public void doTag() throws JspException {
        InputStream inputStream = null;
        try {
            byte[] picArray = null;
            if (pictureFilename!=null && !pictureFilename.isEmpty()) {
                inputStream = new FileInputStream(pictureFilename);
                picArray = Util.readBytes(inputStream);
            }
            JSPHelper.publishDestination(destination, picArray, text);
        } catch (Exception e) {
            throw new JspException(e);
        } finally {
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new JspException(e);
                }
        }
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getPictureFilename() {
        return pictureFilename;
    }

    public void setPictureFilename(String pictureFilename) {
        this.pictureFilename = pictureFilename;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}