package de.greensurvivors.padlock.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;

/**
 * manages all translatable and placeholders used by this plugin.
 */
public class MessageManager {
    private final Plugin plugin;
    /**
     * contains all sign lines without any decorations like color but still with every placeholder
     */
    private final HashMap<LangPath, String> nakedSignLiness = new HashMap<>(); // path -> naked
    /**
     * caches every component without placeholder for faster access in future
     */
    private final HashMap<LangPath, Component> langCache = new HashMap<>();
    private FileConfiguration lang;
    private FileConfiguration fallback;
    private String langfilename = "lang/lang_en.yml";

    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    protected void setLangFileName(String langfilename) {
        this.langfilename = langfilename;
    }

    private @NotNull String getStringFromLang(@NotNull LangPath path) {
        return lang.getString(path.getPath(), fallback.getString(path.getPath(), path.getDefaultValue()));
    }

    /**
     * reload language file. Please call {@link #setLangFileName(String)} before calling this for the first time
     */
    protected void reload() {
        initLangFiles();
        lang = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), langfilename));
        fallback = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "lang/lang_en.yml")); //todo don't hardcode

        langCache.put(LangPath.PLUGIN_PREFIX, MiniMessage.miniMessage().deserialize(getStringFromLang(LangPath.PLUGIN_PREFIX)));

        nakedSignLiness.put(LangPath.PRIVATE_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.PRIVATE_SIGN)).toLowerCase());
        nakedSignLiness.put(LangPath.ADDITIONAL_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.ADDITIONAL_SIGN)).toLowerCase());
        nakedSignLiness.put(LangPath.EVERYONE_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.EVERYONE_SIGN)).toLowerCase());
        nakedSignLiness.put(LangPath.TIMER_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.TIMER_SIGN)).toLowerCase());
        nakedSignLiness.put(LangPath.ERROR_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.ERROR_SIGN)).toLowerCase());
        nakedSignLiness.put(LangPath.INVALID_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.INVALID_SIGN)).toLowerCase());
        nakedSignLiness.put(LangPath.EXPIRE_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.EXPIRE_SIGN)).toLowerCase());
        nakedSignLiness.put(LangPath.PLAYER_NAME_ON_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.PLAYER_NAME_ON_SIGN)).toLowerCase());
    }

    /**
     * saves lang files from resources to the plugins datafolder
     */
    private void initLangFiles() {
        //todo don't hardcode them
        String[] availablefiles = {"lang/lang_de.yml", "lang/lang_en.yml", "lang/lang_es.yml", "lang/lang_hu.yml", "lang/lang_it.yml", "lang/lang_zh-cn.yml"};
        for (String filename : availablefiles) {
            File langfile = new File(plugin.getDataFolder(), filename);
            if (!langfile.exists()) {
                plugin.saveResource(filename, false);
            }
        }
    }

    /**
     * prepend the message with the plugins prefix before sending it to the audience.
     */
    public void sendMessageWithPrefix(@NotNull Audience audience, @Nullable Component messages) {
        if (messages != null) {
            audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).append(messages));
        }
    }

    /**
     * get a component from lang file and apply the given tag resolver.
     * Note: might be slightly slower than {@link #getLang(LangPath)} since this can not use cache.
     */
    public @NotNull Component getLang(@NotNull MessageManager.LangPath path, @NotNull TagResolver resolver) {
        return MiniMessage.miniMessage().deserialize(getStringFromLang(path), resolver);
    }

    /**
     * get a component from lang file
     */
    public @NotNull Component getLang(@NotNull MessageManager.LangPath path) {
        Component result = langCache.get(path);

        if (result == null) {
            result = MiniMessage.miniMessage().deserialize(getStringFromLang(path));

            langCache.put(path, result);
        }
        return result;
    }

    /**
     * send a component from the lang file to the audience, prefixed with this plugins prefix.
     */
    public void sendLang(@NotNull Audience audience, @NotNull LangPath path) {
        Component message = langCache.get(path);

        if (message == null) {
            message = MiniMessage.miniMessage().deserialize(getStringFromLang(path));

            langCache.put(path, message);
        }

        audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).append(message));
    }

    /**
     * send a component from the lang file to the audience, prefixed with this plugins prefix and applying the given tag resolver.
     * Note: might be slightly slower than {@link #sendLang(Audience, LangPath)} since this can not use cache.
     */
    public void sendLang(@NotNull Audience audience, @NotNull LangPath path, @NotNull TagResolver resolver) {
        audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).append(
                MiniMessage.miniMessage().deserialize(getStringFromLang(path), resolver)));
    }

    /**
     * checks if the given component is a spefic kind of sign-line defined by {@link LangPath}
     * please note: this will only work for simple lines. If the line has a placeholder in it, this will most likely fail!
     *
     * @return true if the component is the kind of sign.
     */
    public boolean isSignComp(@NotNull Component compToTest, @NotNull LangPath langPath) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(compToTest).trim();

        return strToTest.toLowerCase().startsWith(nakedSignLiness.get(langPath));
    }

    /**
     * get the line of a sign without any decorations, but still with its placeholders.
     *
     * @param langPath defines the type of line we need
     */
    public @Nullable String getNakedSignText(@NotNull LangPath langPath) {
        return nakedSignLiness.get(langPath);
    }

    /**
     * placeholder strings used. will be surrounded in Minimassage typical format of <>
     */
    public enum PlaceHolder {
        TIME("time"),
        PLAYER("player"),
        ARGUMENT("argument");

        private final String placeholder;

        PlaceHolder(String placeholder) {
            this.placeholder = placeholder;
        }

        /**
         * Since this will be used in Mini-messages placeholder only the pattern "[!?#]?[a-z0-9_-]*" is valid.
         * if used inside an unparsed text you have to add surrounding <> yourself.
         */
        @Subst("name") // substitution; will be inserted if the IDE/compiler tests if input is valid.
        public String getPlaceholder() {
            return placeholder;
        }
    }

    /**
     * Paths of all translatable
     */
    public enum LangPath {
        PLUGIN_PREFIX("prefix", "<gold>[Padlock]</gold> "),

        PRIVATE_SIGN("sign.line.private", "[Private]"),
        @Deprecated(forRemoval = true)
        ADDITIONAL_SIGN("sign.line.additional", "[More Users]"),
        EVERYONE_SIGN("sign.line.everyone", "[Everyone]"),
        TIMER_SIGN("sign.line.timer", "[Timer:<" + PlaceHolder.TIME.getPlaceholder() + ">]"),
        EXPIRE_SIGN("sign.line.expired", "[<dark_aqua>Expired</dark_aqua>]"),
        ERROR_SIGN("sign.line.error", "[<dark_red>Error</dark_red>]"),
        INVALID_SIGN("sign.line.invalid", "[Invalid]"),
        PLAYER_NAME_ON_SIGN("sign.line.player-name", "<" + PlaceHolder.PLAYER.getPlaceholder() + ">"), // used for formatting displayed player names

        HELP_HEADER("cmd.help.header"),
        HELP_ADD_MEMBER("cmd.help.add-member"),
        HELP_REMOVE_MEMBER("cmd.help.remove-member"),
        HELP_ADD_OWNER("cmd.help.add-owner"),
        HELP_REMOVE_OWNER("cmd.help.remove-owner"),
        HELP_SETCREATED("cmd.help.set-created"),
        HELP_SETEVERYONE("cmd.help.set-everyone"),
        HELP_SETTIMER("cmd.help.set-timer"),
        HELP_DEBUG("cmd.help.debug"),
        HELP_HELP("cmd.help.help"),
        HELP_INFO("cmd.help.info"),
        HELP_RELOAD("cmd.help.reload"),
        HELP_UPDATE_SIGN("cmd.help.update-sign"),
        HELP_VERSION("cmd.help.version"),

        ADD_OWNER_SUCCESS("cmd.add-owner.success"),
        REMOVE_OWNER_SUCCESS("cmd.remove-owner.success"),
        REMOVE_OWNER_ERROR("cmd.remove-owner.error"),
        ADD_MEMBER_SUCCESS("cmd.add-member.success"),
        REMOVE_MEMBER_SUCCESS("cmd.remove-member.success"),
        REMOVE_MEMBER_ERROR("cmd.remove-member.error"),
        SET_CREATED_SUCCESS("cmd.set-created.success"),
        SET_CREATED_ERROR("cmd.set-created.error"),
        SET_EVERYONE_SUCCESS("cmd.set-everyone.success"),
        SET_TIMER_SUCCESS_ON("cmd.set-timer.success.on"),
        SET_TIMER_SUCCESS_OFF("cmd.set-timer.success.off"),
        SET_TIMER_ERROR("cmd.set-timer.error"),
        UPDATE_SIGN_SUCCESS("cmd.sign-update.success"),
        RELOAD_SUCCESS("cmd.reload.success"),

        INFO_OWNERS("cmd.info.owners"),
        INFO_MEMBERS("cmd.info.members"),
        INFO_TIMER("cmd.info.timer"),
        INFO_EXPIRED("cmd.info.expired"),

        SIGN_NEED_RESELECT("cmd.error.sign-need-reselect"),
        SIGN_NOT_SELECTED("cmd.error.no-sign-selected"),
        UNKNOWN_PLAYER("cmd.error.unknown-player"),
        NOT_A_PLAYER("cmd.error.not-a-player"),
        NOT_ENOUGH_ARGS("cmd.error.not-enough-args"),
        NOT_A_BOOL("cmd.error.not-a-bool"),
        CMD_USAGE("cmd.usage"),
        CMD_NOT_A_SUBCOMMAND("cmd.not-a-subcommand"),

        NO_PERMISSION("no-permission"),
        NOT_OWNER("lock.not-owner"),

        LOCK_SUCCESS("action.lock.success"),
        LOCK_ERROR_ALREADY_LOCKED("action.lock.error.already-locked"),
        LOCK_ERROR_NOT_LOCKABLE("action.lock.error.not-lockable"),
        LOCK_ERROR_NO_OWNER("action.lock.error.no-owner"),
        QUICK_LOCK_ERROR("action.quick-lock.error"),
        SELECT_SIGN("action.select-sign.success"),
        BREAK_LOCK_SUCCESS("action.break-lock.success"),
        ACTION_PREVENTED_LOCKED("action.prevented.locked"),
        ACTION_PREVENTED_INTERFERE("action.prevented.interfere-with-others"),

        NOTICE_QUICK_LOCK("notice.quick-lock"),
        NOTICE_MANUEL_LOCK("notice.manual-lock");

        private final String path;
        private final String defaultValue;

        LangPath(String path) {
            this.path = path;
            this.defaultValue = "";
        }

        LangPath(String path, String defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }

        public String getPath() {
            return path;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }
}
