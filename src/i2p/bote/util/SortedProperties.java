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

package i2p.bote.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Same as <code>java.util.Properties</code> but writes properties
 * sorted by key. Relies on an implementation detail, so it may
 * not work on JVMs other than OpenJDK, or future versions.
 */
public class SortedProperties extends Properties {
    private static final long serialVersionUID = -3663917284130106235L;

    @Override
    public synchronized Enumeration<Object> keys() {
        Enumeration<Object> unsorted = super.keys();
        List<Object> list = Collections.list(unsorted);
        Collections.sort(list, new Comparator<Object>() {

            @Override
            public int compare(Object o1, Object o2) {
                if (o1==null && o2==null)
                    return 0;
                else if (o1 == null)
                    return -1;
                else if (o2 == null)
                    return 1;
                else
                    return o1.toString().compareTo(o2.toString());
            }
        });
        Enumeration<Object> sorted = Collections.enumeration(list);
        return sorted;
    }
}