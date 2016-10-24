package i2p.bote.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.mikepenz.iconics.IconicsDrawable;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;

import i2p.bote.android.R;

public class IconicsFloatingActionButton extends FloatingActionButton {
    public IconicsFloatingActionButton(Context context) {
        this(context, null);
    }

    public IconicsFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public IconicsFloatingActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconicsFloatingActionButton, 0, 0);
        String iconName = a.getString(R.styleable.IconicsFloatingActionButton_ifab_icon);
        if (iconName == null)
            return;

        IconicsDrawable icon = new IconicsDrawable(context, iconName);
        int color = a.getColor(R.styleable.IconicsFloatingActionButton_ifab_color, 0);
        if (color != 0)
            icon.color(color);
        int size = a.getDimensionPixelSize(R.styleable.IconicsFloatingActionButton_ifab_size, 0);
        if (size != 0)
            icon.sizePx(size);
        int padding = a.getDimensionPixelSize(R.styleable.IconicsFloatingActionButton_ifab_padding, 0);
        if (padding != 0)
            icon.paddingPx(padding);

        a.recycle();
        setIconDrawable(icon);
    }
}
