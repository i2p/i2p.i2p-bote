/**
 * Copyright (C) 2017  str4d@mail.i2p
 * <p>
 * This file is part of I2P-Bote.
 * I2P-Bote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * I2P-Bote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 */

package i2p.bote.android;

import i2p.bote.network.DhtPeerStats;
import i2p.bote.network.DhtPeerStatsRenderer;

/**
 * Renders UI strings for DHT peer stats.
 * @see DhtPeerStatsRenderer
 */
class AndroidPeerStatsRenderer implements DhtPeerStatsRenderer {

    AndroidPeerStatsRenderer() {
    }

    @Override
    public String translateHeading(DhtPeerStats.Columns column) {
        // No-op, headings currently unused
        return "";
    }

    @Override
    public String translateContent(DhtPeerStats.Content content) {
        // No-op, content currently unused
        return "";
    }
}
