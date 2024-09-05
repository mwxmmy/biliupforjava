package top.sshh.bililiverecoder.util;

public enum UploadEnums {
    CS_BDA2("CS_BDA2", "upos", "cs", "bda2", "ugcupos/bup"),
    CS_BLDSA("CS_BLDSA", "upos", "cs", "bldsa", "ugcupos/bup"),
    CS_TX("CS_TX", "upos", "cs", "tx", "ugcupos/bup"),
    CS_TXA("CS_TXA", "upos", "cs", "txa", "ugcupos/bup"),
    CS_ALIA("CS_ALIA", "upos", "cs", "alia", "ugcupos/bup"),
    JD_BD("JD_BD", "upos", "cs", "bd", "ugcupos/bup"),
    JD_BLDSA("JD_BLDSA", "upos", "cs", "bldsa", "ugcupos/bup"),
    JD_TX("JD_TX", "upos", "cs", "tx", "ugcupos/bup"),
    JD_TXA("JD_TXA", "upos", "cs", "txa", "ugcupos/bup"),
    JD_ALIA("JD_ALIA", "upos", "cs", "alia", "ugcupos/bup"),
    APP("APP_不推荐", "app", "", "", "ugcfr/pc3"),
    CS_QN("CS_QN_废弃", "upos", "cs", "qn", "ugcupos/bup"),
    CS_QNHK("CS_QNHK_废弃", "upos", "cs", "qnhk", "ugcupos/bup"),
    SZ_WS("SZ_WS_废弃", "upos", "sz", "ws", "ugcupos/bup"),
    KODO("KODO_废弃", "kodo", "", "", "ugcupos/bupfetch");

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
        return UploadEnums.CS_BLDSA;
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
