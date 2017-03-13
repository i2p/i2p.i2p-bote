/**
 * Copyright (C) 2017  str4d@mail.i2p
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

import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.DhtPeerStatsRenderer;

import static i2p.bote.web.WebappUtil._t;

/**
 * Renders UI strings for DHT peer stats.
 * @see DhtPeerStatsRenderer
 */
class WebappPeerStatsRenderer implements DhtPeerStatsRenderer {

    WebappPeerStatsRenderer() {
    }

    @Override
    public String translateHeading(DhtPeerStats.Columns column) {
        switch (column) {
            case PEER:
                return _t("Peer");
            case DESTINATION:
                return _t("I2P Destination");
            case BUCKET_PREFIX:
                return _t("BktPfx");
            case DISTANCE:
                return _t("Distance");
            case LOCKED:
                return _t("Locked?");
            case FIRST_SEEN:
                return _t("First Seen");
            default:
                return "";
        }
    }

    @Override
    public String translateContent(DhtPeerStats.Content content) {
        switch (content) {
            case YES:
                return _t("Yes");
            case NO:
                return _t("No");
            case BUCKET_PREFIX_S:
                return _t("(S)");
            case BUCKET_PREFIX_NONE:
                return _t("(None)");
            default:
                return "";
        }
    }
}
