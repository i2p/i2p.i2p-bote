package im.delight.android.identicons;

import android.graphics.Color;

public class Identicon extends IdenticonBase {
    private static final int CENTER_COLUMN_INDEX = 5;

    @Override
    protected int getRowCount() {
        return 9;
    }

    @Override
    protected int getColumnCount() {
        return 9;
    }

    protected int getSymmetricColumnIndex(int col) {
        if (col < CENTER_COLUMN_INDEX) {
            return col;
        } else {
            return getColumnCount() - col - 1;
        }
    }

    @Override
    protected boolean isCellVisible(int row, int column) {
        return getByte(3 + row * CENTER_COLUMN_INDEX + getSymmetricColumnIndex(column)) >= 0;
    }

    @Override
    protected int getIconColor() {
        return Color.rgb(getByte(0) + 128, getByte(1) + 128, getByte(2) + 128);
    }

    @Override
    protected int getBackgroundColor() {
        float[] hsv = new float[3];
        Color.colorToHSV(getIconColor(), hsv);
        if (hsv[2] < 0.5)
            return Color.parseColor("#ffeeeeee"); // @color/background_material_light
        else
            return Color.parseColor("#ff303030"); // @color/background_material_dark
    }
}
