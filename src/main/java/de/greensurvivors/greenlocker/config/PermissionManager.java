package de.greensurvivors.greenlocker.config;

public enum PermissionManager {
    EDIT("greenlocker.edit"),
    ADMIN_EDIT("greenlocker.admin.edit"),
    ADMIN_BREAK("greenlocker.admin.break"),
    ADMIN_USE("greenlocker.admin.use"),
    ADMIN_INTERFERE("greenlocker.admin.interfere"),
    DEBUG("greenlocker.debug"),

    NO_EXPIRE("greenlocker.noexpire"),

    CMD_HELP("greenlocker.cmd.help"),
    CMD_INFO("greenlocker.cmd.info"),
    CMD_RELOAD("greenlocker.cmd.reload"),
    CMD_UPDATE_SIGN("greenlocker.cmd.updatesign"),
    CMD_VERSION("greenlocker.cmd.version"),

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
