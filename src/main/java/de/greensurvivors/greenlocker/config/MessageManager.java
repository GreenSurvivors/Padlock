package de.greensurvivors.greenlocker.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;

public class MessageManager { //todo dokument whole plugin
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

        nakedSigns.put(LangPath.PRIVATE_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.PRIVATE_SIGN.getPath(), LangPath.PRIVATE_SIGN.getDefaultValue())));
        nakedSigns.put(LangPath.ADDITIONAL_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.ADDITIONAL_SIGN.getPath(), LangPath.ADDITIONAL_SIGN.getDefaultValue())));
        nakedSigns.put(LangPath.EVERYONE_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.EVERYONE_SIGN.getPath(), LangPath.EVERYONE_SIGN.getDefaultValue())));
        nakedSigns.put(LangPath.TIMER_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.TIMER_SIGN.getPath(), LangPath.TIMER_SIGN.getDefaultValue())));
        nakedSigns.put(LangPath.INVALID_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.INVALID_SIGN.getPath(), LangPath.INVALID_SIGN.getDefaultValue())));
        nakedSigns.put(LangPath.EXPIRE_SIGN, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.EXPIRE_SIGN.getPath(), LangPath.EXPIRE_SIGN.getDefaultValue())));
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

    public void sendMessages(@NotNull CommandSender sender, @Nullable Component messages) { //todo preparation for MessageManager
        if (messages != null) {
            sender.sendMessage(prefixComp.append(messages));
        }
    }

    public @NotNull Component getLang(@NotNull MessageManager.LangPath path) { // todo lang in minimessage format
        Component result = langCache.get(path);

        if (result == null) {
            result = MiniMessage.miniMessage().deserialize(lang.getString(path.path, path.defaultValue));

            langCache.put(path, result);
        }
        return result;
    }

    public void sendLang(@NotNull CommandSender sender, @NotNull LangPath path) {
        Component message = langCache.get(path);

        if (message == null) {
            message = MiniMessage.miniMessage().deserialize(lang.getString(path.path, path.defaultValue));

            langCache.put(path, message);
        }

        sender.sendMessage(prefixComp.append(message));
    }

    public boolean isSignComp(@NotNull Component compToTest, @NotNull LangPath langPath) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(compToTest).trim();

        return strToTest.equalsIgnoreCase(nakedSigns.get(langPath));
    }

    public boolean isTimerSignComp(@NotNull Component compToTest) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(compToTest).trim();

        String[] splitted = nakedSigns.get(LangPath.TIMER_SIGN).split("@", 2);
        return strToTest.startsWith(splitted[0]) && strToTest.endsWith(splitted[1]);
    }

    public int getTimer(Component compToTest) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(compToTest).trim();
        String[] splitted = nakedSigns.get(LangPath.TIMER_SIGN).split("@", 2);

        if (strToTest.startsWith(splitted[0]) && strToTest.endsWith(splitted[1])) {
            String newmessage = strToTest.replace(splitted[0], "").replace(splitted[1], "");
            try {
                int seconds = Integer.parseInt(newmessage);
                return Math.min(seconds, 20);
            } catch (Exception ignored) {
            }
        }

        return 0;
    }

    public enum LangPath {
        PLUGIN_PREFIX("prefix", "&6[GreenLocker]&r"), //todo miniMessage

        PRIVATE_SIGN("sign.line.private", "[Private]"),
        @Deprecated(forRemoval = true)
        ADDITIONAL_SIGN("sign.line.additional", "[More Users]"),
        EVERYONE_SIGN("sign.line.everyone", "[Everyone]"),
        TIMER_SIGN("sign.line.everyone", "[Timer:@]"),
        EXPIRE_SIGN("sign.line.expire", "&3[Expired]"), //todo miniMessage
        ERROR_SIGN("sign.line.error", "[Error]"),
        INVALID_SIGN("sign.line.invalid", "[Invalid]"),

        HELP_HEADER("cmd.help.header"),
        HELP_NO_PERMISSION_SUBCOMMAND("cmd.help.no-perm-subcommand"),
        HELP_ADD_MEMBER("cmd.help.add-member"),
        HELP_REMOVE_MEMBER("cmd.help.remove-member"),
        HELP_ADD_OWNER("cmd.help.add-owner"),
        HELP_REMOVE_OWNER("cmd.help.remove-owner"),
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
        REMOVE_MEMBER_ERROR("cmd.remove-owner.error"),
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
        QUICK_LOCK_ERROR("action.quick-lock.error"),
        LOCK_ERROR_ALREADY_LOCKED("action.lock.error.already-locked"),
        LOCK_ERROR_NOT_LOCKABLE("action.lock.error.not-lockable"),
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
