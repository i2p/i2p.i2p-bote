package i2p.bote.util;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class SummaryEditTextPreference extends EditTextPreference {

    public SummaryEditTextPreference(Context context) {
        super(context);
    }

    public SummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public CharSequence getSummary() {
        return String.format((String) super.getSummary(), getText());
    }
}
