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