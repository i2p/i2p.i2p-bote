package i2p.bote.network;

public class BanReason {
    public enum Reason {
        WRONG_PROTO_VER,
    }

    private Reason reason;
    private String[] args;

    public BanReason(Reason reason, String... args) {
        this.reason = reason;
        this.args = args;
    }

    public Reason getReason() {
        return reason;
    }

    public String[] getArgs() {
        return args;
    }
}
