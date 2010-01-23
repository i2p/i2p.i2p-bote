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

import i2p.bote.I2PBote;
import i2p.bote.network.DhtPeer;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import net.i2p.util.Log;

public class DhtPeerInfoTag extends SimpleTagSupport {
    private Log log = new Log(DhtPeerInfoTag.class);

    public void doTag() {
        PageContext pageContext = (PageContext) getJspContext();
        JspWriter out = pageContext.getOut();
        
        try {
            if (I2PBote.getInstance().getNumDhtPeers() > 0) {
                out.println("<table>");
                out.println("<tr>");
                out.println("<th>Peer</th>");
                out.println("<th>Destination Hash</th>");
                out.println("<th>Active Since</th>");
                out.println("<th>Stale Ctr</th>");
                out.println("<th>Banned?</th>");
                out.println("<th>Ban Reason</th>");
                out.println("</tr>");
            }
            
            Collection<? extends DhtPeer> peerInfoCollection = I2PBote.getInstance().getPeers();
            int peerIndex = 1;
            for (DhtPeer peer: peerInfoCollection) {
                out.println("<tr>");
                out.println("<td>" + peerIndex++ + "</td>");
                out.println("<td>" + peer.getDestination().calculateHash().toBase64() + "</td>");
                out.println("<td>" + new Date(peer.getActiveSince()) + "</td>");
                out.println("<td>" + peer.getStaleCounter() + "</td>");
                out.println("<td>" + (peer.isBanned()?"Yes":"No") + "</td>");
                out.println("<td>" + (peer.getBanReason()==null?"":peer.getBanReason()) + "</td>");
                out.println("</tr>");
            }
            
            out.println("</table>");
        } catch (IOException e) {
            log.error("Can't write output to HTML page", e);
        }
    }
}