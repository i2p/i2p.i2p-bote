/**
 * Copyright (C) 2015  str4d@mail.i2p
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

import net.i2p.util.Log;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.servlet.ServletUtilities;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import i2p.bote.I2PBote;
import i2p.bote.Util;
import i2p.bote.network.BanReason;
import i2p.bote.network.BannedPeer;
import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.DhtPeerStatsRow;
import i2p.bote.network.RelayPeer;

import static i2p.bote.web.WebappUtil._t;

public class PeerInfoTag extends SimpleTagSupport {
    private Log log = new Log(PeerInfoTag.class);

    @Override
    public void doTag() {
        PageContext pageContext = (PageContext) getJspContext();
        JspWriter out = pageContext.getOut();
        
        try {
            // Print DHT peer info
            DhtPeerStats dhtStats = I2PBote.getInstance().getDhtStats(new WebappPeerStatsRenderer());
            if (dhtStats == null)
                return;

            int numDhtPeers = dhtStats.getData().size();

            // Get a sorted list of relay peers
            RelayPeer[] relayPeers = I2PBote.getInstance().getRelayPeers().toArray(new RelayPeer[0]);
            Arrays.sort(relayPeers, new Comparator<RelayPeer>() {
                @Override
                public int compare(RelayPeer peer1, RelayPeer peer2) {
                    return peer2.getReachability() - peer1.getReachability();
                }
            });

            // Print charts
            out.println("<div class=\"network-charts\">");
            out.println("<div class=\"chart\">");
            out.println("<img src=\"displayChart?filename=" + createDhtChart(dhtStats) + "\"/>");
            out.println("<div class=\"chart-text\">" + numDhtPeers + "</div>");
            out.println("</div>");
            out.println("<div class=\"chart\">");
            out.println("<img src=\"displayChart?filename=" + createRelayChart(relayPeers) + "\"/>");
            out.println("<div class=\"chart-text\">" + relayPeers.length + "</div>");
            out.println("</div>");
            out.println("</div>");
            out.println("<br>");

            out.println("<span class=\"subheading\">" + _t("Kademlia Peers:") + " " + numDhtPeers + "</span>");
            if (numDhtPeers > 0) {
                out.println("<table");
                
                // header
                out.println("<tr>");
                for (String columnHeader: dhtStats.getHeader())
                    out.println("<th>" + columnHeader + "</th>");
                out.println("</tr>");
                
                // data
                for (DhtPeerStatsRow row: dhtStats.getData()) {
                    out.println("<tr>");
                    for (String cellData: row.toStrings())
                        out.println("<td class=\"ellipsis\">" + cellData + "</td>");
                    out.println("</tr>");
                }
                
                out.println("</table>");
            }
            
            out.println("<br/>");
            
            // Print relay peer info
            out.println("<span class=\"subheading\">" + _t("Relay Peers:") + " " + relayPeers.length + "</span>");
            if (relayPeers.length > 0) {
                out.println("<table");
                out.println("<tr>");
                out.println("<th>" + _t("Peer") + "</th>");
                out.println("<th>" + _t("I2P Destination") + "</th>");
                out.println("<th>" + _t("Reachability %") + "</th>");
                out.println("</tr>");
                
                int i = 1;
                for (RelayPeer peer: relayPeers) {
                    out.println("<tr>");
                    out.println("<td>" + i + "</td>");
                    out.println("<td class=\"ellipsis\">" + Util.toBase32(peer) + "</td>");
                    int reachability = peer.getReachability();
                    out.println("<td>" + (reachability == 0 ? _t("Untested") : reachability) + "</td>");
                    out.println("</tr>");
                    i++;
                }
                out.println("</table>");
            }
            
            out.println("<br/>");
            
            // List banned peers
            Collection<BannedPeer> bannedPeers = I2PBote.getInstance().getBannedPeers();
            out.println("<span class=\"subheading\">" + _t("Banned Peers:") + " " + bannedPeers.size() + "</span>");
            if (bannedPeers.size() > 0) {
                out.println("<table>");
                out.println("<tr>");
                out.println("<th>" + _t("Peer") + "</th>");
                out.println("<th>" + _t("Destination Hash") + "</th>");
                out.println("<th>" + _t("Ban Reason") + "</th>");
                out.println("</tr>");
                
                int peerIndex = 1;
                for (BannedPeer peer: bannedPeers) {
                    out.println("<tr>");
                    out.println("<td>" + peerIndex++ + "</td>");
                    out.println("<td class=\"ellipsis\">" + Util.toBase32(peer.getDestination()) + "</td>");
                    out.println("<td>" + getBanReason(peer) + "</td>");
                    out.println("</tr>");
                }
                
                out.println("</table>");
            }
        } catch (IOException e) {
            log.error("Can't write output to HTML page", e);
        }
    }

    private String createDhtChart(DhtPeerStats dhtStats) throws IOException {
        RingPlot plot;
        int numDhtPeers = dhtStats.getData().size();
        if (numDhtPeers == 0) {
            DefaultPieDataset dataset = new DefaultPieDataset();
            dataset.setValue("", 100);

            plot = new RingPlot(dataset);
            plot.setSectionPaint("", Color.gray);
        } else {
            int reachable = 0;
            for (DhtPeerStatsRow row : dhtStats.getData()) {
                if (row.isReachable())
                    reachable += 1;
            }
            int unreachable = numDhtPeers - reachable;

            DefaultPieDataset dataset = new DefaultPieDataset();
            if (reachable > 0)
                dataset.setValue(_t("Reachable"), reachable);
            if (unreachable > 0)
                dataset.setValue(_t("Unreachable"), unreachable);

            plot = new RingPlot(dataset);
            plot.setSectionPaint(_t("Reachable"), Color.green);
            plot.setSectionPaint(_t("Unreachable"), Color.red);
        }
        plot.setLabelGenerator(null);
        plot.setShadowGenerator(null);

        JFreeChart dhtChart = new JFreeChart(
                _t("Kademlia Peers:"), JFreeChart.DEFAULT_TITLE_FONT,
                plot, numDhtPeers == 0 ? false : true);
        return ServletUtilities.saveChartAsPNG(dhtChart, 400, 300, null);
    }

    private String createRelayChart(RelayPeer[] relayPeers) throws IOException {
        RingPlot plot;
        if (relayPeers.length == 0) {
            DefaultPieDataset dataset = new DefaultPieDataset();
            dataset.setValue("", 100);

            plot = new RingPlot(dataset);
            plot.setSectionPaint("", Color.gray);
        } else {
            int good = 0;
            int untested = 0;
            for (RelayPeer relayPeer : relayPeers) {
                int reachability = relayPeer.getReachability();
                if (reachability == 0)
                    untested += 1;
                else if (reachability > 80)
                    good += 1;
            }
            int bad = relayPeers.length - good - untested;

            DefaultPieDataset dataset = new DefaultPieDataset();
            if (good > 0)
                dataset.setValue(_t("Good"), good);
            if (bad > 0)
                dataset.setValue(_t("Unreliable"), bad);
            if (untested > 0)
                dataset.setValue(_t("Untested"), untested);

            plot = new RingPlot(dataset);
            plot.setSectionPaint(_t("Good"), Color.green);
            plot.setSectionPaint(_t("Unreliable"), Color.red);
            plot.setSectionPaint(_t("Untested"), Color.orange);
        }
        plot.setLabelGenerator(null);
        plot.setShadowGenerator(null);

        JFreeChart chart = new JFreeChart(
                _t("Relay Peers:"), JFreeChart.DEFAULT_TITLE_FONT,
                plot, relayPeers.length == 0 ? false : true);
        return ServletUtilities.saveChartAsPNG(chart, 400, 300, null);
    }

    private String getBanReason(BannedPeer peer) {
        BanReason reason = peer.getBanReason();
        if (reason == null) {
            return "";
        }
        switch (reason.getReason()) {
            case WRONG_PROTO_VER:
                return _t("Wrong protocol version:") + " " + reason.getArgs()[0];
            default:
                return "";
        }
    }
}
