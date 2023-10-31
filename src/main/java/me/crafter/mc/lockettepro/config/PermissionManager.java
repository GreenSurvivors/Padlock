package me.crafter.mc.lockettepro.config;

public enum PermissionManager {
    edit("lockettepro.edit"),
    adminEdit("lockettepro.admin.edit"),
    adminBreak("lockettepro.admin.break"),
    adminUse("lockettepro.admin.use"),
    adminInterfere("lockettepro.admin.interfere"),
    debug("lockettepro.debug"),

    noExpire("lockettepro.noexpire"),

    cmdHelp("lockettepro.cmd.help"),
    cmdInfo("lockettepro.cmd.info"),
    cmdReload("lockettepro.cmd.reload"),
    cmdUpdateSign("lockettepro.cmd.updatesign"),
    cmdVersion("lockettepro.cmd.version"),

    actionLock("lockettepro.action.lock"),
    actionLockOthers("lockettepro.action.lockothers");

    private final String perm;

    private PermissionManager(String perm) {
        this.perm = perm;
    }

    public String getPerm() {
        return perm;
    }
}
