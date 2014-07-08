package i2p.bote.android;

import java.util.Collection;
import java.util.Set;

import net.i2p.data.Destination;
import i2p.bote.I2PBote;
import i2p.bote.Util;
import i2p.bote.network.BannedPeer;
import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.RelayPeer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class NetworkInfoFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_network_info, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Destination dest = I2PBote.getInstance().getLocalDestination();
        if (dest != null)
            ((TextView) view.findViewById(R.id.local_destination)).setText(
                    Util.toBase32(dest));

        DhtPeerStats dhtStats = I2PBote.getInstance().getDhtStats();
        if (dhtStats != null)
            ((TextView) view.findViewById(R.id.kademlia_peers)).setText(
                    "" + dhtStats.getData().size());

        Set<RelayPeer> relayPeers = I2PBote.getInstance().getRelayPeers();
        ((TextView) view.findViewById(R.id.relay_peers)).setText(
                "" + relayPeers.size());

        Collection<BannedPeer> bannedPeers = I2PBote.getInstance().getBannedPeers();
        ((TextView) view.findViewById(R.id.banned_peers)).setText(
                "" + bannedPeers.size());

        Exception e = I2PBote.getInstance().getConnectError();
        if (e != null)
            ((TextView) view.findViewById(R.id.error)).setText(e.getLocalizedMessage());
    }
}
