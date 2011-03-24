/* ========================================================================
 *
 *  This file is part of CODEC, which is a Java package for encoding
 *  and decoding ASN.1 data structures.
 *
 *  Author: Fraunhofer Institute for Computer Graphics Research IGD
 *          Department A8: Security Technology
 *          Fraunhoferstr. 5, 64283 Darmstadt, Germany
 *
 *  Rights: Copyright (c) 2004 by Fraunhofer-Gesellschaft 
 *          zur Foerderung der angewandten Forschung e.V.
 *          Hansastr. 27c, 80686 Munich, Germany.
 *
 * ------------------------------------------------------------------------
 *
 *  The software package is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU Lesser General Public License as 
 *  published by the Free Software Foundation; either version 2.1 of the 
 *  License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public 
 *  License along with this software package; if not, write to the Free 
 *  Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 *  MA 02110-1301, USA or obtain a copy of the license at 
 *  http://www.fsf.org/licensing/licenses/lgpl.txt.
 *
 * ------------------------------------------------------------------------
 *
 *  The CODEC library can solely be used and distributed according to 
 *  the terms and conditions of the GNU Lesser General Public License for 
 *  non-commercial research purposes and shall not be embedded in any 
 *  products or services of any user or of any third party and shall not 
 *  be linked with any products or services of any user or of any third 
 *  party that will be commercially exploited.
 *
 *  The CODEC library has not been tested for the use or application 
 *  for a determined purpose. It is a developing version that can 
 *  possibly contain errors. Therefore, Fraunhofer-Gesellschaft zur 
 *  Foerderung der angewandten Forschung e.V. does not warrant that the 
 *  operation of the CODEC library will be uninterrupted or error-free. 
 *  Neither does Fraunhofer-Gesellschaft zur Foerderung der angewandten 
 *  Forschung e.V. warrant that the CODEC library will operate and 
 *  interact in an uninterrupted or error-free way together with the 
 *  computer program libraries of third parties which the CODEC library 
 *  accesses and which are distributed together with the CODEC library.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  does not warrant that the operation of the third parties's computer 
 *  program libraries themselves which the CODEC library accesses will 
 *  be uninterrupted or error-free.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  shall not be liable for any errors or direct, indirect, special, 
 *  incidental or consequential damages, including lost profits resulting 
 *  from the combination of the CODEC library with software of any user 
 *  or of any third party or resulting from the implementation of the 
 *  CODEC library in any products, systems or services of any user or 
 *  of any third party.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  does not provide any warranty nor any liability that utilization of 
 *  the CODEC library will not interfere with third party intellectual 
 *  property rights or with any other protected third party rights or will 
 *  cause damage to third parties. Fraunhofer Gesellschaft zur Foerderung 
 *  der angewandten Forschung e.V. is currently not aware of any such 
 *  rights.
 *
 *  The CODEC library is supplied without any accompanying services.
 *
 * ========================================================================
 */
package codec.asn1;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This is the root class of all ASN.1 time types. In principle, the known time
 * types are all of type VisibleString.
 * 
 * @author Volker Roth
 * @version "$Id: ASN1Time.java,v 1.7 2004/09/20 15:22:31 pebinger Exp $"
 */
abstract public class ASN1Time extends ASN1VisibleString {
    /**
     * The <code>TimeZone</code> representing universal coordinated time
     * (UTC).
     */
    private static final TimeZone TZ = TimeZone.getTimeZone("GMT");

    /**
     * Used to fill with zeroes.
     */
    protected static final String ZEROES = "0000";

    /**
     * The internal storage of the date.
     */
    protected Date date_;

    /**
     * Returns a Java Date instance representing the time in this ASN.1 time
     * type.
     * 
     * @return The time as a Java Date instance.
     */
    public Date getDate() {
	return (Date) date_.clone();
    }

    /**
     * Returns a Java long representing the time in milliseconds since January
     * 1, 1970, 00:00:00 GMT in this ASN.1 time type.
     * 
     * @return The number of milliseconds since January 1, 1970, 00:00:00 GMT.
     */
    public long getTime() {
	return date_.getTime();
    }

    /**
     * Sets the time from the given <code>Calendar</code>.
     * 
     * @param calendar
     *                The <code>Calendar</code> with the date that shall be
     *                set.
     */
    public void setDate(Calendar calendar) {
	if (calendar == null) {
	    throw new NullPointerException("calendar");
	}
	date_ = calendar.getTime();

	setString0(toString(date_));
    }

    /**
     * Sets the time from the given Date instance.
     * 
     * @param date
     *                The Date.
     */
    public void setDate(Date date) {
	if (date == null) {
	    throw new NullPointerException("date");
	}
	date_ = (Date) date.clone();

	setString0(toString(date_));
    }

    /**
     * Sets the time from the given time in milliseconds since January 1, 1970,
     * 00:00:00 GMT.
     * 
     * @param time
     *                The number of milliseconds since January 1, 1970, 00:00:00
     *                GMT.
     */
    public void setDate(long time) {
	date_ = new Date(time);

	setString0(toString(date_));
    }

    /**
     * Sets the date to the one represented by the given string. The internal
     * string representation is normalized and complies to DER. The date string
     * is thus converted to GMT.
     * 
     * @param date
     *                The date as a X.680 date string.
     * @throws IllegalArgumentException
     *                 if the string is not well-formed.
     * @throws StringIndexOutOfBoundsException
     *                 if the string is not well-formed.
     */
    public void setDate(String date) {
	if (date == null) {
	    throw new NullPointerException("date string");
	}
	date_ = toDate(date);

	setString0(toString(date_));
    }

    /**
     * Sets the string value.
     * 
     * @param s
     *                The string value.
     */
    public void setString(String s) {
	date_ = toDate(s);

	/*
	 * The value must be set literally because this method is called by the
	 * decoders. This ensures that the encoding is bitwise identical to the
	 * decoding.
	 */
	setString0(s);
    }

    public void encode(Encoder enc) throws ASN1Exception, IOException {
	enc.writeTime(this);
    }

    public void decode(Decoder enc) throws ASN1Exception, IOException {
	enc.readTime(this);
    }

    abstract protected int[] getFields();

    abstract protected int[] getFieldLengths();

    abstract protected int[] getFieldCorrections();

    /**
     * Converts the given <code>Date</code> into a string representation
     * according to DER as described in X.690.
     * 
     * @param date
     *                The <code>Date</code> that is converted.
     * @return The string with the date.
     */
    protected String toString(Date date) {
	StringBuffer buf;
	Calendar cal;
	String s;
	int[] lengths;
	int[] correct;
	int[] fields;
	int len;
	int w;
	int n;
	int v;
	int lastzero;

	if (date == null) {
	    throw new NullPointerException("date");
	}
	cal = new GregorianCalendar(TZ);
	fields = getFields();
	correct = getFieldCorrections();
	lengths = getFieldLengths();
	buf = new StringBuffer(20);

	/*
	 * Date is UTC time (most of the time ;-) and we set Calendar to UTC.
	 */
	cal.setTime(date);

	for (n = 0; n < fields.length; n++) {
	    v = cal.get(fields[n]) - correct[n];
	    s = String.valueOf(v);
	    len = s.length();

	    /*
	     * If the target length is zero then we truncate to the left, and
	     * take only the hundreds if they are greater than zero. Hence, only
	     * one digit is printed. In summary, we handle the case of
	     * milliseconds.
	     */
	    w = lengths[n];

	    if (w == 0) {
		if (v > 0) {

		    buf.append(".");
		    // add leading 0s if necessary
		    s = ZEROES.substring(0, 3 - s.length()) + s;

		    if (s.charAt(s.length() - 1) != '0') {
			buf.append(s);
		    } else {
			lastzero = s.length() - 1;
			while ((lastzero > 0)
				&& (s.charAt(lastzero - 1) == '0')) {
			    lastzero--;
			}
			buf.append(s.substring(0, lastzero));
		    }
		}
		continue;
	    }
	    /*
	     * If we have to fill up then we fill zeroes to the left. This
	     * accounts for days as well as hours and minutes.
	     */
	    if (w < 0) {
		w = -w;
	    }
	    if (len < w) {
		buf.append(ZEROES.substring(0, w - len));
		buf.append(s);
	    }
	    /*
	     * If we must truncate then we take the rightmost characters. This
	     * accounts for truncated years e.g. "98" instead of "1998".
	     */
	    else if (len > w) {
		buf.append(s.substring(len - w));
	    }
	    /*
	     * Everything is fine, we got the length we need.
	     */
	    else {
		buf.append(s);
	    }
	}
	buf.append('Z');

	return buf.toString();
    }

    /**
     * Converts the given string to a <code>Date</code> object.
     * 
     * @param code
     *                The string encoding of the date to be converted.
     * @return The <code>Date</code> object.
     * @throws IllegalArgumentException
     *                 if the given string is not a valid BER encoding of a
     *                 date.
     */
    protected Date toDate(String code) {
	Calendar cal;
	Calendar res;
	TimeZone tz;
	int[] lengths;
	int[] correct;
	int[] fields;
	String s;
	int pos;
	int len;
	int n;
	int w;
	int v;
	int c;

	if (code == null) {
	    throw new NullPointerException("code");
	}
	cal = new GregorianCalendar(TZ);
	cal.setTime(new Date(0));
	fields = getFields();
	correct = getFieldCorrections();
	lengths = getFieldLengths();
	len = code.length();

	for (pos = 0, n = 0; n < fields.length; n++) {
	    /*
	     * If the field length is zero then we handle milliseconds. In
	     * particular, we test whether the milliseconds are present.
	     */
	    w = lengths[n];

	    if (w == 0) {
		/*
		 * No character, no period or comma, therefor no milliseconds
		 * either.
		 */
		if (pos >= len) {
		    continue;
		}
		c = code.charAt(pos);

		/*
		 * No period or comma but another character presumably means
		 * that there are no millis but a time zone offset - or a bad
		 * code.
		 */
		if (c != '.' && c != ',') {
		    continue;
		}
		pos++;

		/*
		 * We have millis, and now we're gonna read them!
		 */
		for (v = 0; (v < 3 && pos < len); v++) {
		    if (!Character.isDigit(code.charAt(pos))) {
			break;
		    }
		    pos++;
		}
		/*
		 * If we did not consume at least one digit then we have a bad
		 * encoding.
		 */
		if (v == 0) {
		    throw new IllegalArgumentException(
			    "Milliseconds format error!");
		}
		s = code.substring(pos - v, pos);

		if (v < 3) {
		    s = s + ZEROES.substring(0, 3 - v);
		}
		v = Integer.parseInt(s);
		v = v + correct[n];

		cal.set(fields[n], v);

		continue;
	    }
	    /*
	     * Here we deal with optional digit fields such as seconds in BER.
	     */
	    if (w < 0) {
		w = -w;

		if (pos >= len || !Character.isDigit(code.charAt(pos))) {
		    continue;
		}
	    }
	    /*
	     * We fetch the required number of characters and try to decode
	     * them.
	     */
	    s = code.substring(pos, pos + w);
	    v = Integer.parseInt(s);
	    v = v + correct[n];
	    pos = pos + w;

	    /*
	     * Special case for UTCTime: we have to correct for years before
	     * 1970.
	     */
	    if (fields[n] == Calendar.YEAR && lengths[n] == 2) {
		v = v + ((v < 70) ? 2000 : 1900);
	    }
	    cal.set(fields[n], v);
	}
	/*
	 * We still have to deal with time zone offsets and time zone
	 * specifications - nasty stuff.
	 */
	if (pos < len) {
	    c = code.charAt(pos);

	    /*
	     * If there is a '+' or '-' then we have a time differential to GMT
	     * and no trailing 'Z'.
	     */
	    if (c == '+' || c == '-') {
		s = code.substring(pos, pos + 5);
		tz = TimeZone.getTimeZone("GMT" + s);
		pos = pos + 5;
	    }
	    /*
	     * No time differential means we either have a 'Z' or a bad
	     * encoding.
	     */
	    else if (code.charAt(pos) != 'Z') {
		throw new IllegalArgumentException(
			"Illegal char in place of 'Z' (" + pos + ")");
	    }
	    /*
	     * We got the 'Z', thus we have GMT.
	     */
	    else {
		tz = TimeZone.getTimeZone("GMT");
		pos++;
	    }
	}
	/*
	 * We reached the end of the string without encountering a time
	 * differential or a 'Z', therefor we use the local time zone. This
	 * should rarely happen unless someone screws up. Nevertheless, it's a
	 * valid code.
	 */
	else {
	    tz = TimeZone.getDefault();
	}
	if (pos != len) {
	    throw new IllegalArgumentException(
		    "Trailing characters after encoding! (" + pos + ")");
	}
	/*
	 * we now have a Calendar calibrated to GMT and a time zone in tz. Now
	 * we merge both together in order to get the correct time according to
	 * GMT.
	 */
	res = Calendar.getInstance(tz);
	res.setTime(new Date(0));

	for (n = 0; n < fields.length; n++) {
	    res.set(fields[n], cal.get(fields[n]));
	}
	return res.getTime();
    }

}
