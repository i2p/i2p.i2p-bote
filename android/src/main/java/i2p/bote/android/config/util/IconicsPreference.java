package i2p.bote.android.config.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

import com.mikepenz.iconics.IconicsDrawable;

import i2p.bote.android.R;

public class IconicsPreference extends Preference {
    public IconicsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public IconicsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void init(Context context, AttributeSet attrs) {
        // Icons only work on API 11+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            return;

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.IconicsPreference, 0, 0);
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
