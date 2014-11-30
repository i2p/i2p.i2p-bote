package i2p.bote.android;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androidplot.pie.PieChart;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import i2p.bote.I2PBote;
import i2p.bote.network.BannedPeer;
import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.RelayPeer;

import static i2p.bote.Util._;

public class NetworkInfoFragment extends Fragment {
    private PieChart mKademliaPie;
    private TextView mKademliaPeers;
    private PieChart mRelayPie;
    private TextView mRelayPeers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_network_info, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mKademliaPie = (PieChart) view.findViewById(R.id.kademlia_peers_pie);
        mKademliaPeers = (TextView) view.findViewById(R.id.kademlia_peers);
        mRelayPie = (PieChart) view.findViewById(R.id.relay_peers_pie);
        mRelayPeers = (TextView) view.findViewById(R.id.relay_peers);

        setupKademliaPeers();
        setupRelayPeers();

        Collection<BannedPeer> bannedPeers = I2PBote.getInstance().getBannedPeers();
        ((TextView) view.findViewById(R.id.banned_peers)).setText(
                "" + bannedPeers.size());

        Exception e = I2PBote.getInstance().getConnectError();
        if (e != null)
            ((TextView) view.findViewById(R.id.error)).setText(e.toString());
    }

    private void setupSegmentFormatter(SegmentFormatter sf) {
        sf.getLabelPaint().setTextSize(20);
    }

    private void setupKademliaPeers() {
        DhtPeerStats dhtStats = I2PBote.getInstance().getDhtStats();
        if (dhtStats != null) {
            if (dhtStats.getData().size() == 0) {
                Segment n = new Segment("", 100);

                SegmentFormatter nf = new SegmentFormatter();
                setupSegmentFormatter(nf);
                nf.getFillPaint().setColor(getResources().getColor(android.R.color.darker_gray));

                mKademliaPie.addSeries(n, nf);
            } else {
                int reachable = 0;
                for (List<String> row : dhtStats.getData()) {
                    if (_("No").equals(row.get(4)))
                        reachable += 1;
                }
                int unreachable = dhtStats.getData().size() - reachable;

                mKademliaPeers.setText("" + dhtStats.getData().size());

                if (reachable > 0) {
                    Segment r = new Segment(getString(R.string.reachable), reachable);

                    SegmentFormatter rf = new SegmentFormatter();
                    setupSegmentFormatter(rf);
                    rf.getFillPaint().setColor(getResources().getColor(R.color.green));

                    mKademliaPie.addSeries(r, rf);
                }

                if (unreachable > 0) {
                    Segment u = new Segment(getString(R.string.unreachable), dhtStats.getData().size() - reachable);

                    SegmentFormatter uf = new SegmentFormatter();
                    setupSegmentFormatter(uf);
                    uf.getFillPaint().setColor(getResources().getColor(R.color.error_color));

                    mKademliaPie.addSeries(u, uf);
                }
            }
        }

        mKademliaPie.getBorderPaint().setColor(Color.TRANSPARENT);
        mKademliaPie.getBackgroundPaint().setColor(Color.TRANSPARENT);
    }

    private void setupRelayPeers() {
        Set<RelayPeer> relayPeers = I2PBote.getInstance().getRelayPeers();
        mRelayPeers.setText("" + relayPeers.size());

        if (relayPeers.size() == 0) {
            Segment n = new Segment("", 100);

            SegmentFormatter nf = new SegmentFormatter();
            setupSegmentFormatter(nf);
            nf.getFillPaint().setColor(getResources().getColor(android.R.color.darker_gray));

            mRelayPie.addSeries(n, nf);
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
            int bad = relayPeers.size() - good - untested;

            if (good > 0) {
                Segment g = new Segment(getString(R.string.good), good);

                SegmentFormatter gf = new SegmentFormatter();
                setupSegmentFormatter(gf);
                gf.getFillPaint().setColor(getResources().getColor(R.color.green));

                mRelayPie.addSeries(g, gf);
            }

            if (bad > 0) {
                Segment b = new Segment(getString(R.string.unreliable), bad);

                SegmentFormatter bf = new SegmentFormatter();
                setupSegmentFormatter(bf);
                bf.getFillPaint().setColor(getResources().getColor(R.color.red));

                mRelayPie.addSeries(b, bf);
            }

            if (untested > 0) {
                Segment u = new Segment(getString(R.string.untested), untested);

                SegmentFormatter uf = new SegmentFormatter();
                setupSegmentFormatter(uf);
                uf.getFillPaint().setColor(getResources().getColor(R.color.accent));

                mRelayPie.addSeries(u, uf);
            }
        }

        mRelayPie.getBorderPaint().setColor(Color.TRANSPARENT);
        mRelayPie.getBackgroundPaint().setColor(Color.TRANSPARENT);
    }
}
