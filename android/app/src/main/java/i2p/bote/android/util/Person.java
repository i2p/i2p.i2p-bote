package i2p.bote.android.util;

import android.graphics.Bitmap;

import java.io.Serializable;

public class Person implements Serializable {
    private static final long serialVersionUID = -2874686247798691378L;
    private String name;
    private String address;
    private Bitmap picture;
    private boolean isExternal;

    public Person(String n, String a, Bitmap p) { this(n, a, p, false); }
    public Person(String n, String a, Bitmap p, boolean e) { name = n; address = a; picture = p; isExternal = e; }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public Bitmap getPicture() { return picture; }
    public boolean isExternal() { return isExternal; }

    @Override
    public boolean equals(Object other) {
        return other instanceof Person && address.equals(((Person) other).address);
    }

    @Override
    public String toString() { return name; }
}
