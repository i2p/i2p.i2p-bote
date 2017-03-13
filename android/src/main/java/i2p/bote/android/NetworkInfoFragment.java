package i2p.bote.android;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.pie.PieChart;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;

import net.i2p.android.ui.I2PAndroidHelper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Set;

import i2p.bote.I2PBote;
import i2p.bote.network.BannedPeer;
import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.DhtPeerStatsRow;
import i2p.bote.network.RelayPeer;

public class NetworkInfoFragment extends Fragment {
    private Exception mConnectError;

    private PieChart mKademliaPie;
    private TextView mKademliaPeers;
    private PieChart mRelayPie;
    private TextView mRelayPeers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mConnectError = I2PBote.getInstance().getConnectError();
        if (mConnectError == null)
            return inflater.inflate(R.layout.fragment_network_info, container, false);
        else
            return inflater.inflate(R.layout.fragment_network_error, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mConnectError == null) {
            mKademliaPie = (PieChart) view.findViewById(R.id.kademlia_peers_pie);
            mKademliaPeers = (TextView) view.findViewById(R.id.kademlia_peers);
            mRelayPie = (PieChart) view.findViewById(R.id.relay_peers_pie);
            mRelayPeers = (TextView) view.findViewById(R.id.relay_peers);

            setupKademliaPeers();
            setupRelayPeers();

            Collection<BannedPeer> bannedPeers = I2PBote.getInstance().getBannedPeers();
            ((TextView) view.findViewById(R.id.banned_peers)).setText(
                    "" + bannedPeers.size());
        } else {
            ((TextView) view.findViewById(R.id.error)).setText(mConnectError.toString());

            view.findViewById(R.id.copy_error).setOnClickListener(new View.OnClickListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onClick(View view) {
                    String fullError = joinStackTrace(mConnectError);
                    Object clipboardService = getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) clipboardService;
                        clipboard.setText(fullError);
                    } else {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) clipboardService;
                        android.content.ClipData clip = android.content.ClipData.newPlainText(
                                getString(R.string.bote_connection_error), fullError);
                        clipboard.setPrimaryClip(clip);
                    }
                    Toast.makeText(getActivity(), R.string.full_error_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                }
            });

            if ((new I2PAndroidHelper(getActivity())).isI2PAndroidInstalled())
                view.findViewById(R.id.error_page_i2p_content).setVisibility(View.VISIBLE);
        }
    }

    private void setupSegmentFormatter(SegmentFormatter sf) {
        sf.getLabelPaint().setTextSize(20);
    }

    private void setupKademliaPeers() {
        DhtPeerStats dhtStats = I2PBote.getInstance().getDhtStats(new AndroidPeerStatsRenderer());
        if (dhtStats != null) {
            if (dhtStats.getData().size() == 0) {
                Segment n = new Segment("", 100);

                SegmentFormatter nf = new SegmentFormatter(getResources().getColor(android.R.color.darker_gray));
                setupSegmentFormatter(nf);

                mKademliaPie.addSeries(n, nf);
            } else {
                int reachable = 0;
                for (DhtPeerStatsRow row : dhtStats.getData()) {
                    if (row.isReachable())
                        reachable += 1;
                }
                int unreachable = dhtStats.getData().size() - reachable;

                mKademliaPeers.setText("" + dhtStats.getData().size());

                if (reachable > 0) {
                    Segment r = new Segment(getString(R.string.reachable), reachable);

                    SegmentFormatter rf = new SegmentFormatter(getResources().getColor(R.color.green));
                    setupSegmentFormatter(rf);

                    mKademliaPie.addSeries(r, rf);
                }

                if (unreachable > 0) {
                    Segment u = new Segment(getString(R.string.unreachable), dhtStats.getData().size() - reachable);

                    SegmentFormatter uf = new SegmentFormatter(getResources().getColor(R.color.error_color));
                    setupSegmentFormatter(uf);

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

            SegmentFormatter nf = new SegmentFormatter(getResources().getColor(android.R.color.darker_gray));
            setupSegmentFormatter(nf);

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

                SegmentFormatter gf = new SegmentFormatter(getResources().getColor(R.color.green));
                setupSegmentFormatter(gf);

                mRelayPie.addSeries(g, gf);
            }

            if (bad > 0) {
                Segment b = new Segment(getString(R.string.unreliable), bad);

                SegmentFormatter bf = new SegmentFormatter(getResources().getColor(R.color.red));
                setupSegmentFormatter(bf);

                mRelayPie.addSeries(b, bf);
            }

            if (untested > 0) {
                Segment u = new Segment(getString(R.string.untested), untested);

                SegmentFormatter uf = new SegmentFormatter(getResources().getColor(R.color.accent));
                setupSegmentFormatter(uf);

                mRelayPie.addSeries(u, uf);
            }
        }

        mRelayPie.getBorderPaint().setColor(Color.TRANSPARENT);
        mRelayPie.getBackgroundPaint().setColor(Color.TRANSPARENT);
    }

    private static String joinStackTrace(Throwable e) {
        StringWriter writer = null;
        try {
            writer = new StringWriter();
            joinStackTrace(e, writer);
            return writer.toString();
        }
        finally {
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e1) {
                    // ignore
                }
        }
    }

    private static void joinStackTrace(Throwable e, StringWriter writer) {
        PrintWriter printer = null;
        try {
            printer = new PrintWriter(writer);

            while (e != null) {

                printer.println(e);
                StackTraceElement[] trace = e.getStackTrace();
                for (StackTraceElement aTrace : trace) printer.println("\tat " + aTrace);

                e = e.getCause();
                if (e != null)
                    printer.println("Caused by:\r\n");
            }
        }
        finally {
            if (printer != null)
                printer.close();
        }
    }
}
