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

import i2p.bote.I2PBote;
import i2p.bote.Util;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.Outbox.EmailStatus;
import i2p.bote.util.GeneralHelper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import net.i2p.I2PAppContext;
import net.i2p.data.Destination;
import net.i2p.util.Log;
import net.i2p.util.Translate;

import static i2p.bote.web.WebappUtil._t;

/**
 * Implements the JSP functions defined in the <code>i2pbote.tld</code> file,
 * and serves as a bean for JSPs.
 */
public class JSPHelper extends GeneralHelper {
    private static final String CONSOLE_BUNDLE_NAME = "net.i2p.router.web.messages";
    private static final String RECIPIENT_KEY_PREFIX = "nofilter_recipient";

    /**
     * @since 0.4.4
     */
    public static String csrfErrorMsg() {
        I2PAppContext ctx = I2PAppContext.getGlobalContext();
        return "<p>" + consoleGetString(
                "Invalid form submission, probably because you used the 'back' or 'reload' button on your browser. Please resubmit.",
                ctx
            ) + "</p><p>" + consoleGetString(
                "If the problem persists, verify that you have cookies enabled in your browser.",
                ctx
            ) + "</p>";
    }

    /**
     * Translate with the console bundle.
     * @since 0.4.4
     */
    private static String consoleGetString(String s, I2PAppContext ctx) {
        return Translate.getString(s, ctx, CONSOLE_BUNDLE_NAME);
    }

    /**
     * Returns a new <code>SortedMap<String, String></code> that contains only those
     * entries from the original map whose key is <code>"nofilter_recipient"</code>,
     * followed by a whole number.
     * @param parameters
     * @return A map whose keys start with "nofilter_recipient", sorted by key
     */
    public static SortedMap<String, String> getSortedRecipientParams(Map<String, String> parameters) {
        SortedMap<String, String> newMap = new TreeMap<String, String>();
        for (String key: parameters.keySet()) {
            if (key == null)
                continue;
            if (key.startsWith(RECIPIENT_KEY_PREFIX)) {
                String indexString = key.substring(RECIPIENT_KEY_PREFIX.length());
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
        String[] newAddressesArray = request.getParameterValues("nofilter_selectedContact");
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

    private static String getHumanReadableSize(long numBytes) {
        String language = Translate.getLanguage(I2PAppContext.getGlobalContext());
        int unit = (63-Long.numberOfLeadingZeros(numBytes)) / 10;   // 0 if totalBytes<1K, 1 if 1K<=totalBytes<1M, etc.
        double value = (double)numBytes / (1<<(10*unit));
        String messageKey;
        switch (unit) {
            case 0: messageKey = "{0} Bytes"; break;
            case 1: messageKey = "{0} KBytes"; break;
            case 2: messageKey = "{0} MBytes"; break;
            default: messageKey = "{0} GBytes";
        }
        NumberFormat formatter = NumberFormat.getInstance(new Locale(language));
        if (value < 100)
            formatter.setMaximumFractionDigits(1);
        else
            formatter.setMaximumFractionDigits(0);
        return _t(messageKey, formatter.format(value));
    }

    public static String getHumanReadableSize(File file) {
        long size = file.length();
        return getHumanReadableSize(size);
    }

    /** Returns the size of an email attachment in a user-friendly format
     * @throws MessagingException
     * @throws IOException */
    public static String getHumanReadableSize(Part part) throws IOException, MessagingException {
        return getHumanReadableSize(Util.getPartSize(part));
    }

    public String getLocalDestination() {
        Destination dest = I2PBote.getInstance().getLocalDestination();
        if (dest != null)
            return Util.toBase32(dest);
        return _t("Not set.");
    }

    public static String getFileSize(String filename) {
        return getHumanReadableSize(new File(filename));
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
            return _t("Queued");
        case SENDING:
            return _t("Sending");
        case SENT_TO:
            return _t("Sent to {0} out of {1} recipients",
                    emailStatus.getParam1(), emailStatus.getParam2());
        case EMAIL_SENT:
            return _t("Email sent");
        case GATEWAY_DISABLED:
            return _t("Gateway disabled");
        case NO_IDENTITY_MATCHES:
            return _t("No identity matches the sender/from field: {0}",
                    emailStatus.getParam1());
        case INVALID_RECIPIENT:
            return _t("Invalid recipient address: {0}", emailStatus.getParam1());
        case ERROR_CREATING_PACKETS:
            return _t("Error creating email packets: {0}", emailStatus.getParam1());
        case ERROR_SENDING:
            return _t("Error while sending email: {0}", emailStatus.getParam1());
        case ERROR_SAVING_METADATA:
            return _t("Error saving email metadata: {0}", emailStatus.getParam1());
        default:
            return "";
        }
    }

    public String getNewEmailNotificationContent() {
        String title = "I2P-Bote";
        String body = "";
        try {
            Email email = I2PBote.getInstance().getInbox().getLatestUnreadEmail();
            if (email != null) {
                title = "I2P-Bote: " + getNameAndShortDestination(email.getOneFromAddress());
                body = email.getSubject();
            }
        } catch (PasswordException e) {
        } catch (MessagingException e) {
        } catch (IOException e) {
        } catch (GeneralSecurityException e) {
        }
        if (body == "")
            body = _t("New email received");
        return "<script>\nnotifTitle=\"" + title + "\";\nnotifBody=\"" + body + "\";\n</script>";
    }
}
