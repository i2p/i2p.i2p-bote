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

import static i2p.bote.Util._;
import i2p.bote.email.Email;
import i2p.bote.folder.Outbox.EmailStatus;
import i2p.bote.util.GeneralHelper;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import net.i2p.I2PAppContext;
import net.i2p.util.Log;
import net.i2p.util.Translate;

/**
 * Implements the JSP functions defined in the <code>i2pbote.tld</code> file,
 * and serves as a bean for JSPs.
 */
public class JSPHelper extends GeneralHelper {
    /**
     * Returns a new <code>SortedMap<String, String></code> that contains only those
     * entries from the original map whose key is <code>"recipient"</code>, followed
     * by a whole number.
     * @param parameters
     * @return A map whose keys start with "recipient", sorted by key
     */
    public static SortedMap<String, String> getSortedRecipientParams(Map<String, String> parameters) {
        SortedMap<String, String> newMap = new TreeMap<String, String>();
        for (String key: parameters.keySet()) {
            if (key == null)
                continue;
            if (key.startsWith("recipient")) {
                String indexString = key.substring("recipient".length());
                if (isNumeric(indexString)) {
                    String value = parameters.get(key);
                    if (value ==null)
                        value = "";
                    newMap.put(key, value);
                }
            }
        }
        return newMap;
    }

    public static List<RecipientAddress> mergeRecipientFields(ServletRequest request) {
        Log log = new Log(GeneralHelper.class);

        // Convert request.getParameterMap() to a Map<String, String>
        Map<String, String[]> parameterArrayMap = request.getParameterMap();
        Map<String, String> parameterStringMap = new HashMap<String, String>();
        for (Map.Entry<String, String[]> parameter: parameterArrayMap.entrySet()) {
            String[] value = parameter.getValue();
            if (value!=null && value.length>0)
                parameterStringMap.put(parameter.getKey(), value[0]);
            else
                parameterStringMap.put(parameter.getKey(), "");
        }
        Map<String, String> oldAddresses = getSortedRecipientParams(parameterStringMap);

        String action = request.getParameter("action");
        int indexToRemove = -1;
        if (action!=null && action.startsWith("removeRecipient")) {
            String indexString = action.substring("removeRecipient".length());
            if (isNumeric(indexString))
                indexToRemove = Integer.valueOf(indexString);
        }

        // make an Iterator over the selectedContact values
        String[] newAddressesArray = request.getParameterValues("selectedContact");
        Iterator<String> newAddresses;
        if (newAddressesArray == null)
            newAddresses = new ArrayList<String>().iterator();
        else
            newAddresses = Arrays.asList(newAddressesArray).iterator();

        // make selectedContact values and oldAddresses into one List
        List<RecipientAddress> mergedAddresses = new ArrayList<RecipientAddress>();
        int i = 0;
        for (String address: oldAddresses.values()) {
            // don't add it if it needs to be removed
            if (i == indexToRemove) {
                i++;
                continue;
            }

            String typeKey = "recipientType" + i;
            String type;
            if (parameterStringMap.containsKey(typeKey))
                type = parameterStringMap.get(typeKey);
            else {
                log.error("Request contains a parameter named recipient" + i + ", but no parameter named recipientType" + i + ".");
                type = "to";
            }

            if (!address.trim().isEmpty())
                mergedAddresses.add(new RecipientAddress(type, address));
            // if an existing address field is empty and a selectedContact is available, put the selectedContact into the address field
            else if (newAddresses.hasNext())
                mergedAddresses.add(new RecipientAddress(type, newAddresses.next()));
            else
                mergedAddresses.add(new RecipientAddress(type, ""));

            i++;
        }

        // add any remaining selectedContacts
        while ((newAddresses.hasNext()))
            mergedAddresses.add(new RecipientAddress("to", newAddresses.next()));

        if ("addRecipientField".equalsIgnoreCase(action))
            mergedAddresses.add(new RecipientAddress("to", ""));
        // Make sure there is a blank recipient field at the end so all non-empty fields have a remove button next to them
        else if (mergedAddresses.isEmpty() || !mergedAddresses.get(mergedAddresses.size()-1).getAddress().isEmpty())
            mergedAddresses.add(new RecipientAddress("to", ""));

        return mergedAddresses;
    }

    public static class RecipientAddress {
        private String addressType;
        private String address;

        public RecipientAddress(String addressType, String address) {
            this.addressType = addressType;
            this.address = address;
        }

        public String getAddressType() {
            return addressType;
        }

        public String getAddress() {
            return address;
        }
    }

    public static String escapeQuotes(String s) {
        if (s == null)
            return null;

        return s.replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
    }

    public static String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            new Log(JSPHelper.class).error("UTF-8 not supported!", e);
            return input;
        }
    }

    public static String urlDecode(String input) {
        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            new Log(JSPHelper.class).error("UTF-8 not supported!", e);
            return input;
        }
    }

    /**
     * Inserts the current locale (<code>de</code>, <code>fr</code>, etc.) into a filename
     * if the locale is not <code>en</code>. For example, <code>FAQ.html</code> becomes
     * <code>FAQ_fr.html</code> in the French locale.
     * @param baseName The filename for the <code>en</code> locale
     * @param context To find out if a localized file exists
     * @return The name of the localized file, or <code>baseName</code> if no localized version exists
     */
    public static String getLocalizedFilename(String baseName, ServletContext context) {
        String language = Translate.getLanguage(I2PAppContext.getGlobalContext());

        if (language.equals("en"))
            return baseName;
        else {
            String localizedName;
            if (baseName.contains(".")) {
                int dotIndex = baseName.lastIndexOf('.');
                localizedName = baseName.substring(0, dotIndex) + "_" + language + baseName.substring(dotIndex);
            }
            else
                localizedName = baseName + "_" + language;

            try {
                if (context.getResource("/" + localizedName) != null)
                    return localizedName;
                else
                    return baseName;
            } catch (MalformedURLException e) {
                new Log(JSPHelper.class).error("Invalid URL: </" + localizedName + ">", e);
                return baseName;
            }
        }
    }

    public static String getEmailStatusText(Email email) {
        EmailStatus emailStatus = getEmailStatus(email);
        switch (emailStatus.getStatus()) {
        case QUEUED:
            return _("Queued");
        case SENDING:
            return _("Sending");
        case SENT_TO:
            return _("Sent to {0} out of {1} recipients",
                    emailStatus.getParam1(), emailStatus.getParam2());
        case EMAIL_SENT:
            return _("Email sent");
        case GATEWAY_DISABLED:
            return _("Gateway disabled");
        case NO_IDENTITY_MATCHES:
            return _("No identity matches the sender/from field: {0}",
                    emailStatus.getParam1());
        case INVALID_RECIPIENT:
            return _("Invalid recipient address: {0}", emailStatus.getParam1());
        case ERROR_CREATING_PACKETS:
            return _("Error creating email packets: {0}", emailStatus.getParam1());
        case ERROR_SENDING:
            return _("Error while sending email: {0}", emailStatus.getParam1());
        case ERROR_SAVING_METADATA:
            return _("Error saving email metadata: {0}", emailStatus.getParam1());
        default:
            return "";
        }
    }
}