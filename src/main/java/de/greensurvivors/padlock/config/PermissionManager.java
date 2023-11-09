package de.greensurvivors.padlock.config;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.Map;

/**
 * manages all permissions used by this plugin
 */
public enum PermissionManager {
    CMD_HELP(new Permission("padlock.cmd.help", "", PermissionDefault.TRUE)),
    CMD_INFO(new Permission("padlock.cmd.info", "", PermissionDefault.TRUE)),
    CMD_RELOAD(new Permission("padlock.cmd.reload", "", PermissionDefault.OP)),
    CMD_UPDATE_SIGN(new Permission("padlock.cmd.updatesign", "", PermissionDefault.OP)),
    CMD_SET_TIMER(new Permission("padlock.cmd.setTimer", PermissionDefault.TRUE)),
    CMD_SET_CREATED(new Permission("padlock.cmd.setcreated", "", PermissionDefault.OP)),
    CMD_SET_EVERYONE(new Permission("padlock.cmd.everyone", "", PermissionDefault.TRUE)),
    CMD_VERSION(new Permission("padlock.cmd.version", "", PermissionDefault.OP)),

    ACTION_LOCK(new Permission("padlock.action.lock", "", PermissionDefault.TRUE)),
    ACTION_LOCK_OTHERS(new Permission("padlock.action.lockothers", "", PermissionDefault.OP)),

    NO_EXPIRE(new Permission("padlock.noexpire", "", PermissionDefault.OP)),

    EDIT(new Permission("padlock.edit", "Edit sign permission", PermissionDefault.TRUE)),
    ADMIN_EDIT(new Permission("padlock.admin.edit", "", PermissionDefault.OP, Map.of(EDIT.getPerm().getName(), true))),
    ADMIN_BREAK(new Permission("padlock.admin.break", "", PermissionDefault.OP)),
    ADMIN_USE(new Permission("padlock.admin.use", "", PermissionDefault.OP)),
    ADMIN_INTERFERE(new Permission("padlock.admin.interfere", "", PermissionDefault.OP)),
    ADMIN_COMMANDS(new Permission("padlock.admin.cmds", "", PermissionDefault.OP,
            Map.of(
                    CMD_HELP.perm.getName(), true,
                    CMD_INFO.perm.getName(), true,
                    CMD_RELOAD.perm.getName(), true,
                    CMD_UPDATE_SIGN.perm.getName(), true,
                    CMD_SET_TIMER.perm.getName(), true,
                    CMD_SET_CREATED.perm.getName(), true,
                    CMD_SET_EVERYONE.perm.getName(), true,
                    CMD_VERSION.perm.getName(), true
            ))),
    DEBUG(new Permission("padlock.debug", "", PermissionDefault.OP)),

    // last so it can call all the others
    WILDCARD_ALL(new Permission("padlock.admin.*", "", PermissionDefault.OP,
            Map.of(
                    ADMIN_EDIT.perm.getName(), true,
                    ADMIN_BREAK.perm.getName(), true,
                    ADMIN_USE.perm.getName(), true,
                    ADMIN_COMMANDS.perm.getName(), true
            )));

    private final Permission perm;

    PermissionManager(Permission perm) {
        this.perm = perm;

        Bukkit.getPluginManager().addPermission(perm);
    }

    public Permission getPerm() {
        return perm;
    }
}
