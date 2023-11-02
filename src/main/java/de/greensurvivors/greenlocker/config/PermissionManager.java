package de.greensurvivors.greenlocker.config;

public enum PermissionManager {
    edit("greenlocker.edit"),
    adminEdit("greenlocker.admin.edit"),
    adminBreak("greenlocker.admin.break"),
    adminUse("greenlocker.admin.use"),
    adminInterfere("greenlocker.admin.interfere"),
    debug("greenlocker.debug"),

    noExpire("greenlocker.noexpire"),

    cmdHelp("greenlocker.cmd.help"),
    cmdInfo("greenlocker.cmd.info"),
    cmdReload("greenlocker.cmd.reload"),
    cmdUpdateSign("greenlocker.cmd.updatesign"),
    cmdVersion("greenlocker.cmd.version"),

    actionLock("greenlocker.action.lock"),
    actionLockOthers("greenlocker.action.lockothers");

    private final String perm;

    PermissionManager(String perm) {
        this.perm = perm;
    }

    public String getPerm() {
        return perm;
    }
}
