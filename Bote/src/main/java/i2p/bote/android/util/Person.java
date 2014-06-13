package i2p.bote.android.util;

import java.io.Serializable;

public class Person implements Serializable {
    private static final long serialVersionUID = -2874686247798691378L;
    private String name;
    private String address;
    private boolean isExternal;

    public Person(String n, String a) { this(n, a, false); }
    public Person(String n, String a, boolean e) { name = n; address = a; isExternal = e; }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public boolean isExternal() { return isExternal; }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Person))
            return false;
        return address.equals(((Person)other).address);
    }

    @Override
    public String toString() { return name; }
}
