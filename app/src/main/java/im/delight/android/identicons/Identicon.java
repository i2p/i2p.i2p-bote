package im.delight.android.identicons;

import android.graphics.Color;

public class Identicon extends IdenticonBase {
    private static final int CENTER_COLUMN_INDEX = 3;

    @Override
    protected int getRowCount() {
        return 5;
    }

    @Override
    protected int getColumnCount() {
        return 5;
    }

    protected int getSymmetricColumnIndex(int row) {
        if (row < CENTER_COLUMN_INDEX) {
            return row;
        } else {
            return getColumnCount() - row - 1;
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
}
