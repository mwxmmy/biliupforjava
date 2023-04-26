package top.sshh.bililiverecoder.entity;


import lombok.Data;

@Data
public class ExportConfigParams {

    private boolean exportRoom;
    private boolean exportUser;
    private boolean exportHistory;
}
