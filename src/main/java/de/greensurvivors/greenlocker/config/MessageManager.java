package de.greensurvivors.greenlocker.config;

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

public class MessageManager {
    private final Plugin plugin;
    private final HashMap<LangPath, String> nakedSigns = new HashMap<>(); // path -> naked
    private final HashMap<LangPath, Component> langCache = new HashMap<>();
    private @NotNull Component prefixComp = MiniMessage.miniMessage().deserialize(LangPath.PLUGIN_PREFIX.getDefaultValue());
    private FileConfiguration lang;
    private String langfilename = "lang/lang_en.yml";


    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    protected void setLangFileName(String langfilename) {
        this.langfilename = langfilename;
    }

    protected void reload() {
        initAdditionalFiles();
        lang = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), langfilename));

        prefixComp = MiniMessage.miniMessage().deserialize(lang.getString(LangPath.PLUGIN_PREFIX.getPath(), LangPath.PLUGIN_PREFIX.getDefaultValue()));

        nakedSigns.put(LangPath.PRIVATE_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.PRIVATE_SIGN.getPath(), LangPath.PRIVATE_SIGN.getDefaultValue())).toLowerCase());
        nakedSigns.put(LangPath.ADDITIONAL_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.ADDITIONAL_SIGN.getPath(), LangPath.ADDITIONAL_SIGN.getDefaultValue())).toLowerCase());
        nakedSigns.put(LangPath.EVERYONE_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.EVERYONE_SIGN.getPath(), LangPath.EVERYONE_SIGN.getDefaultValue())).toLowerCase());
        nakedSigns.put(LangPath.TIMER_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.TIMER_SIGN.getPath(), LangPath.TIMER_SIGN.getDefaultValue())).toLowerCase());
        nakedSigns.put(LangPath.INVALID_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.INVALID_SIGN.getPath(), LangPath.INVALID_SIGN.getDefaultValue())).toLowerCase());
        nakedSigns.put(LangPath.EXPIRE_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.EXPIRE_SIGN.getPath(), LangPath.EXPIRE_SIGN.getDefaultValue())).toLowerCase());
    }

    private void initAdditionalFiles() {
        //todo don't hardcode them
        String[] availablefiles = {"lang/lang_de.yml", "lang/lang_en.yml", "lang/lang_es.yml", "lang/lang_hu.yml", "lang/lang_it.yml", "lang/lang_zh-cn.yml"};
        for (String filename : availablefiles) {
            File langfile = new File(plugin.getDataFolder(), filename);
            if (!langfile.exists()) {
                plugin.saveResource(filename, false);
            }
        }
    }

    public void sendMessages(@NotNull Audience audience, @Nullable Component messages) {
        if (messages != null) {
            audience.sendMessage(prefixComp.append(messages));
        }
    }

    public @NotNull Component getLang(@NotNull MessageManager.LangPath path, TagResolver resolver) {
        return MiniMessage.miniMessage().deserialize(lang.getString(path.path, path.defaultValue), resolver);
    }

    public @NotNull Component getLang(@NotNull MessageManager.LangPath path) { // todo lang in minimessage format
        Component result = langCache.get(path);

        if (result == null) {

            result = MiniMessage.miniMessage().deserialize(lang.getString(path.path, path.defaultValue));

            langCache.put(path, result);
        }
        return result;
    }

    public void sendLang(@NotNull Audience audience, @NotNull LangPath path) {
        Component message = langCache.get(path);

        if (message == null) {
            message = MiniMessage.miniMessage().deserialize(lang.getString(path.path, path.defaultValue));

            langCache.put(path, message);
        }

        audience.sendMessage(prefixComp.append(message));
    }

    public boolean isSignComp(@NotNull Component compToTest, @NotNull LangPath langPath) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(compToTest).trim();

        return strToTest.toLowerCase().startsWith(nakedSigns.get(langPath));
    }

    public String getNakedSignText(@NotNull LangPath langPath) {
        return nakedSigns.get(langPath);
    }

    public enum PlaceHolder {
        TIME("time");

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

    public enum LangPath {
        PLUGIN_PREFIX("prefix", "<gold>[GreenLocker]</gold> "),

        PRIVATE_SIGN("sign.line.private", "[Private]"),
        @Deprecated(forRemoval = true)
        ADDITIONAL_SIGN("sign.line.additional", "[More Users]"),
        EVERYONE_SIGN("sign.line.everyone", "[Everyone]"),
        TIMER_SIGN("sign.line.timer", "[Timer:<" + PlaceHolder.TIME.getPlaceholder() + ">]"),
        EXPIRE_SIGN("sign.line.expired", "[<dark_aqua>Expired</dark_aqua>]"),
        ERROR_SIGN("sign.line.error", "[Error]"),
        INVALID_SIGN("sign.line.invalid", "[Invalid]"),

        HELP_HEADER("cmd.help.header"),
        HELP_NO_PERMISSION_SUBCOMMAND("cmd.help.no-perm-subcommand"),
        HELP_ADD_MEMBER("cmd.help.add-member"),
        HELP_REMOVE_MEMBER("cmd.help.remove-member"),
        HELP_ADD_OWNER("cmd.help.add-owner"),
        HELP_REMOVE_OWNER("cmd.help.remove-owner"),
        HELP_SETCREATED("cmd.help.set-created"),
        HELP_SETEVERYONE("cmd.help.set-everyone"),
        HELP_SETREDSTONE("cmd.help.set-redstone"),
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
        SET_EVERYONE_ERROR("cmd.set-everyone.error"),
        SET_TIMER_SUCCESS_ON("cmd.set-timer.success.on"),
        SET_TIMER_SUCCESS_OFF("cmd.set-timer.success.off"),
        SET_TIMER_ERROR("cmd.set-timer.error"),
        UPDATE_SIGN_SUCCESS("cmd.sign-update.success"),
        INFO_OWNERS("cmd.info.owners"),
        INFO_MEMBERS("cmd.info.members"),
        RELOAD_SUCCESS("cmd.reload.success"),

        SIGN_NEED_RESELECT("cmd.sign-need-reselect"),
        SIGN_NOT_SELECTED("cmd.no-sign-selected"),
        UNKNOWN_PLAYER("cmd.unknown-player"),
        NOT_A_PLAYER("cmd.not-a-player"),
        NOT_ENOUGH_ARGS("cmd.not-enough-args"),
        CMD_USAGE("cmd.usage"),

        NO_PERMISSION("no-permission"),
        NOT_OWNER("lock.not-owner"),

        LOCK_SUCCESS("action.lock.success"),
        LOCK_ERROR_ALREADY_LOCKED("action.lock.error.already-locked"),
        LOCK_ERROR_NOT_LOCKABLE("action.lock.error.not-lockable"),
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
