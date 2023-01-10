package top.sshh.bililiverecoder.util;

public enum UploadEnums {
    APP("APP", "app", "", "", "ugcfr/pc3"), SZ_BDA2("SZ_BDA2", "upos", "sz", "bda2", "ugcupos/bup"),
    CS_BDA2("CS_BDA2", "upos", "cs", "bda2", "ugcupos/bup"), SZ_QN("SZ_QN", "upos", "sz", "qn", "ugcupos/bup"),
    CS_QN("CS_QN", "upos", "cs", "qn", "ugcupos/bup"), CS_QNHK("CS_QNHK", "upos", "cs", "qnhk", "ugcupos/bup"),
    CS_BLDSA("CS_BLDSA", "upos", "cs", "bldsa", "ugcupos/bup"),
    SZ_WS("SZ_WS", "upos", "sz", "ws", "ugcupos/bup"),
    KODO("KODO", "kodo", "", "", "ugcupos/bupfetch");

    private final String line;
    private final String os;
    private final String zone;
    private final String cdn;
    private final String profile;
    private final String lineQuery;

    UploadEnums(String line, String os, String zone, String cdn, String profile) {
        this.line = line;
        this.os = os;
        this.zone = zone;
        this.cdn = cdn;
        this.profile = profile;
        this.lineQuery = "?os=" + os + "&zone=" + zone + "&upcdn=" + cdn;
    }

    public static UploadEnums find(String line) {
        for (UploadEnums value : UploadEnums.values()) {
            if (value.getLine().equals(line)) {
                return value;
            }
        }
        return UploadEnums.SZ_BDA2;
    }

    public String getLine() {
        return line;
    }

    public String getOs() {
        return os;
    }

    public String getZone() {
        return zone;
    }

    public String getCdn() {
        return cdn;
    }

    public String getProfile() {
        return profile;
    }

    public String getLineQuery() {
        return lineQuery;
    }
}
