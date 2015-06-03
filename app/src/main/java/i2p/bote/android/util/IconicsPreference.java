package i2p.bote.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;

import com.mikepenz.iconics.IconicsDrawable;

import i2p.bote.android.R;

public class IconicsPreference extends Preference {
    public IconicsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("NewApi")
    public IconicsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        // Icons only work on API 11+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            return;

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.IconicsPreference, defStyle, 0);
        String iconName = a.getString(R.styleable.IconicsPreference_ip_icon);
        if (iconName == null)
            return;

        IconicsDrawable icon = new IconicsDrawable(context, iconName);
        int color = a.getColor(R.styleable.IconicsPreference_ip_color, 0);
        if (color != 0)
            icon.color(color);
        int size = a.getDimensionPixelSize(R.styleable.IconicsPreference_ip_size, 0);
        if (size != 0)
            icon.sizePx(size);
        int padding = a.getDimensionPixelSize(R.styleable.IconicsPreference_ip_padding, 0);
        if (padding != 0)
            icon.paddingPx(padding);

        a.recycle();
        setIcon(icon);
    }
}
