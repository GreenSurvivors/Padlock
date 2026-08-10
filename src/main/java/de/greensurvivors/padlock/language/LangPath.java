package de.greensurvivors.padlock.language;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

/**
 * Paths of all translatable
 */
public enum LangPath {
    //actions
    ACTION_PREVENTED_INTERFERE("action.prevented.interfere-with-others"),
    ACTION_PREVENTED_LOCKED("action.prevented.locked"),
    ACTION_PREVENTED_USE_CMDS("action.prevented.use-commands"),
    //
    ADD_MEMBER_SUCCESS("cmd.add-member.success"),
    ADD_OWNER_SUCCESS("cmd.add-owner.success"),
    BREAK_LOCK_SUCCESS("action.break-lock.success"),
    CMD_NOT_A_SUBCOMMAND("cmd.not-a-subcommand"),
    CMD_USAGE("cmd.usage"),
    //help
    HELP_ADD_MEMBER("cmd.help.add-member"),
    HELP_ADD_OWNER("cmd.help.add-owner"),
    HELP_DEBUG("cmd.help.debug"),
    HELP_HEADER("cmd.help.header"),
    HELP_DESCRIPTION("cmd.help.description"),
    HELP_HELP("cmd.help.help"),
    HELP_INFO("cmd.help.info"),
    HELP_PASSWORD("cmd.help.password"),
    HELP_RELOAD("cmd.help.reload"),
    HELP_REMOVE_MEMBER("cmd.help.remove-member"),
    HELP_REMOVE_OWNER("cmd.help.remove-owner"),
    HELP_SET_ACCESS_TYPE("cmd.help.set-access-type"),
    HELP_SET_CONNECTED("cmd.help.set-connected"),
    HELP_SET_PASSWORD("cmd.help.set-password"),
    HELP_SET_TIMER("cmd.help.set-timer"),
    HELP_UPDATE_DISPLAY("cmd.help.update-display"),
    HELP_VERSION("cmd.help.version"),
    //info cmd
    INFO_HEAD("cmd.info.head"),
    INFO_ACCESS_TYPE("cmd.info.access-type"),
    INFO_EXPIRED("cmd.info.expired"),
    INFO_MEMBERS("cmd.info.members"),
    INFO_OWNERS("cmd.info.owners"),
    INFO_TIMER("cmd.info.timer"),
    //
    LOCK_ERROR_ALREADY_LOCKED("action.lock.error.already-locked"),
    LOCK_ERROR_NOT_LOCKABLE("action.lock.error.not-lockable"),
    LOCK_ERROR_NO_OWNER("action.lock.error.no-owner"),
    LOCK_SUCCESS("action.lock.success"),
    //notices
    NOTICE_MANUEL_LOCK("notice.manual-lock"),
    NOTICE_QUICK_LOCK("notice.quick-lock"),
    // user cmd args errors
    NOT_ACCESS_TYPE("cmd.error.not-access-type"),
    NOT_A_BOOL("cmd.error.not-a-bool"),
    NOT_A_PLAYER("cmd.error.not-a-player"),
    NOT_ENOUGH_ARGS("cmd.error.not-enough-args"),
    NOT_OWNER("lock.not-owner"),
    NO_PERMISSION("no-permission"),
    //passwords
    PASSWORD_ACCESS_GRANTED("cmd.password.access-granted"),
    PASSWORD_ON_COOLDOWN("cmd.password.on-cooldown"),
    PASSWORD_SAFETY_WARNING("cmd.password.safety-warning", "<dark_red>Warning: never use a password, you are using anywhere else! While I did everything I could for your safety, there <bold>ARE</bold> ways your password could get leaked!</dark_red>"),
    PASSWORD_START_PROCESSING("cmd.password.start-processing"),
    PASSWORD_WRONG_PASSWORD("cmd.password.wrong-password"),
    //plugin prefix
    PLUGIN_PREFIX("prefix", "<gold>[Padlock]</gold> "),
    //
    QUICK_LOCK_ERROR("action.quick-lock.error"),
    RELOAD_SUCCESS("cmd.reload.success"),
    REMOVE_MEMBER_ERROR("cmd.remove-member.error"),
    REMOVE_MEMBER_SUCCESS("cmd.remove-member.success"),
    REMOVE_OWNER_ERROR("cmd.remove-owner.error"),
    REMOVE_OWNER_SUCCESS("cmd.remove-owner.success"),
    SELECT_SIGN("action.select-sign.success"),
    SET_ACCESS_TYPE_SUCCESS("cmd.set-access-type.success"),
    SET_CONNECTED_SUCCESS("cmd.set-connected.success"),
    SET_PASSWORD_REMOVE_SUCCESS("cmd.set-password.remove.success"),
    SET_PASSWORD_SUCCESS("cmd.set-password.set.success"),
    SET_TIMER_ERROR("cmd.set-timer.error"),
    SET_TIMER_SUCCESS_OFF("cmd.set-timer.success.turned-off"),
    SET_TIMER_SUCCESS_ON("cmd.set-timer.success.turned-on"),
    // display on signs
    SIGN_LINE_DISPLAY("sign.line.display", "[Display]"),
    SIGN_LINE_DONATION("sign.line.donation", "[Donation]"),
    SIGN_LINE_ERROR("sign.line.error", "[<dark_red>Error</dark_red>]"),
    SIGN_LINE_INVALID("sign.line.invalid", "[Invalid]"),
    SIGN_LINE_PRIVATE("sign.line.private", "[Private]"),
    SIGN_LINE_PUBLIC("sign.line.public", "[Public]"),
    SIGN_LINE_SUPPLY_SIGN("sign.line.supply", "[Supply]"),
    SIGN_LINE_TIMER_SIGN("sign.line.timer", "[Timer:<" + PlaceHolder.TIME.getPlaceholder() + ">]"),
    SING_LINE_HAS_PASSWORD("sign.line.has-password", "[Password]"),
    SIGN_MORE_USERS("sign.line.more-users", "[More Users]"), // not the same as the LEGACY_ADDITIONAL_SIGN line. This indicates that there are users that can't get displayed.
    //
    SIGN_NEED_RESELECT("cmd.error.sign-need-reselect"),
    SIGN_NOT_SELECTED("cmd.error.no-sign-selected"),
    SIGN_PLAYER_NAME_ON("sign.line.player-name", "<" + PlaceHolder.PLAYER.getPlaceholder() + ">"), // used for formatting displayed player names
    UNKNOWN_PLAYER("cmd.error.unknown-player"),
    UPDATE_DISPLAY_SUCCESS("cmd.update-display.success");

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    LangPath(final @NonNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    LangPath(final @NonNull String path, final @NonNull String defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public @NonNull String getPath() {
        return path;
    }

    public @NonNull String getDefaultValue() {
        return defaultValue;
    }
}
