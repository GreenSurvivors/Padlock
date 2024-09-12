package de.greensurvivors.padlock.config;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * manages all permissions used by this plugin
 */
public enum PermissionManager {
    CMD_APPLY_PASSWORD(new Permission("padlock.cmd.applypassword",
            "Users can get temporarily member access via `/padlock applypassword`",
            PermissionDefault.TRUE)),
    CMD_HELP(new Permission("padlock.cmd.help",
            "Allows to use the /padlock help command - But what commands are shown there is guarded behind the permissions of the specific commands.",
            PermissionDefault.TRUE)),
    CMD_INFO(new Permission("padlock.cmd.info",
            "Allows members and owners of a lock to acquire more information about it via /padlock info.",
            PermissionDefault.TRUE)),
    CMD_RELOAD(new Permission("padlock.cmd.reload",
            "Reloads the config and language files via `/padlock reload`.",
            PermissionDefault.OP)),
    CMD_SET_ACCESS_TYPE(new Permission("padlock.cmd.setaccesstype",
            "Set with /padlock setaccesstype <type> how players that are neither owners nor members interact with the locked block",
            PermissionDefault.TRUE)),
    CMD_SET_CONNECTED(new Permission("padlock.cmd.setconnected",
            "Owners of a sign may set if adjacent openable blocks of the same type should open / close together.",
            PermissionDefault.TRUE)),
    CMD_SET_PASSWORD(new Permission("padlock.cmd.setpassword",
            "Owners of a sign may set a password via `/padlock setpw` so users can get temporary member access",
            PermissionDefault.TRUE)),
    CMD_SET_TIMER(new Permission("padlock.cmd.settimer",
            "Owners of a sign may set a duration after witch openable blocks (doors, trapdoors, fence gates) should toggle after getting used.",
            PermissionDefault.TRUE)),
    CMD_UPDATE_DISPLAY(new Permission("padlock.cmd.updatedisplay",
        "Owners or members of a sign may update the display, to reflect the latest language changes or the changed username.",
        PermissionDefault.OP)),
    CMD_VERSION(new Permission("padlock.cmd.version",
            "`/padlock version` returns the version of this plugin.",
            PermissionDefault.OP)),
    ADMIN_COMMANDS(new Permission("padlock.admin.cmds",
            "Permission to use all Commands.",
            PermissionDefault.OP,
        Map.ofEntries(
            Map.entry(CMD_APPLY_PASSWORD.perm.getName(), true),
            Map.entry(CMD_HELP.perm.getName(), true),
            Map.entry(CMD_INFO.perm.getName(), true),
            Map.entry(CMD_RELOAD.perm.getName(), true),
            Map.entry(CMD_SET_ACCESS_TYPE.perm.getName(), true),
            Map.entry(CMD_SET_CONNECTED.perm.getName(), true),
            Map.entry(CMD_SET_PASSWORD.perm.getName(), true),
            Map.entry(CMD_SET_TIMER.perm.getName(), true),
            Map.entry(CMD_UPDATE_DISPLAY.perm.getName(), true),
            Map.entry(CMD_VERSION.perm.getName(), true)
            ))),

    ACTION_LOCK(new Permission("padlock.action.lock",
            "Allows a user to create lock signs.",
            PermissionDefault.TRUE)),
    ACTION_LOCK_OTHERS(new Permission("padlock.action.lockothers",
            "Allows a user to create locks they don't own. ",
            PermissionDefault.OP)),
    EDIT(new Permission("padlock.edit",
            "Edit your own lock",
            PermissionDefault.TRUE)),

    ADMIN_EDIT(new Permission("padlock.admin.edit",
            "Edit locks of other players",
            PermissionDefault.OP, Map.of(EDIT.getPerm().getName(), true))),
    ADMIN_BREAK(new Permission("padlock.admin.break",
            "Break locks of other players",
            PermissionDefault.OP)),
    ADMIN_PASSWORD(new Permission("padlock.admin.password",
            "Allows to manage passwords of Signs one does not own. Dangerous!",
            PermissionDefault.FALSE)),
    ADMIN_USE(new Permission("padlock.admin.use",
            "Use locked blocks of other players",
            PermissionDefault.OP)),
    ADMIN_INTERFERE(new Permission("padlock.admin.interfere",
            "Place blocks that would interfere with locked blocks.",
            PermissionDefault.OP)),
    DEBUG(new Permission("padlock.debug",
            "Debug permission. Only for development/serious problems.",
            PermissionDefault.OP)),

    // last so it can call all the others
    WILDCARD_ALL(new Permission("padlock.admin", "All permissions", PermissionDefault.OP,
            Map.of(
                    ADMIN_EDIT.perm.getName(), true,
                    ADMIN_BREAK.perm.getName(), true,
                    ADMIN_USE.perm.getName(), true,
                    ADMIN_INTERFERE.perm.getName(), true,
                    ADMIN_COMMANDS.perm.getName(), true,
                    ADMIN_PASSWORD.perm.getName(), true,
                    DEBUG.perm.getName(), true
            )));

    private final Permission perm;

    PermissionManager(@NotNull Permission perm) {
        this.perm = perm;

        Bukkit.getPluginManager().addPermission(perm);
    }

    public Permission getPerm() {
        return perm;
    }
}
