package i2p.bote;

public enum RecipientType {
    TO("To"), CC("CC"), BCC("BCC");
    
    private String value;

    private RecipientType(String value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    public boolean equalsString(String string) {
        return value.equalsIgnoreCase(string);
    }
}