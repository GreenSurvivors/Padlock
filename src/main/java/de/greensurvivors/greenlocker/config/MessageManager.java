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
    private @Nullable Component prefixComp = null;
    private FileConfiguration lang;
    private String langfilename = "lang_en.yml";


    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    protected void setLangFileName(String langfilename) {
        this.langfilename = langfilename;
    }

    protected void reload() {
        initAdditionalFiles();
        lang = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), langfilename));

        prefixComp = MiniMessage.miniMessage().deserialize(lang.getString(LangPath.pluginPrefix.getPath(), LangPath.pluginPrefix.getDefaultValue()));

        nakedSigns.put(LangPath.privateSign, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.privateSign.getPath(), LangPath.privateSign.getDefaultValue())));
        nakedSigns.put(LangPath.additionalSign, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.additionalSign.getPath(), LangPath.additionalSign.getDefaultValue())));
        nakedSigns.put(LangPath.everyoneSign, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.everyoneSign.getPath(), LangPath.everyoneSign.getDefaultValue())));
        nakedSigns.put(LangPath.timerSign, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.timerSign.getPath(), LangPath.timerSign.getDefaultValue())));
        nakedSigns.put(LangPath.invalidSign, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.invalidSign.getPath(), LangPath.invalidSign.getDefaultValue())));
        nakedSigns.put(LangPath.expireSign, MiniMessage.miniMessage().stripTags(lang.getString(LangPath.expireSign.getPath(), LangPath.expireSign.getDefaultValue())));
    }

    private void initAdditionalFiles() {
        String[] availablefiles = {"lang_de.yml", "lang_en.yml", "lang_es.yml", "lang_hu.yml", "lang_it.yml", "lang_zh-cn.yml"};
        for (String filename : availablefiles) {
            File langfile = new File(plugin.getDataFolder(), filename);
            if (!langfile.exists()) {
                plugin.saveResource(filename, false);
            }
        }
    }

    public void sendMessages(@NotNull CommandSender sender, @Nullable Component messages) { //todo preparation for MessageManager
        if (messages != null) {
            if (prefixComp != null) {
                sender.sendMessage(prefixComp.append(messages));
            } else {
                sender.sendMessage(messages);
                plugin.getLogger().severe("send a message in chat, but the prefix was null. Pretty sure the language file was never loaded.");
            }
        }
    }

    public @NotNull Component getLang(@NotNull MessageManager.LangPath path) { // todo lang in minimessage format
        return MiniMessage.miniMessage().deserialize(lang.getString(path.path, path.defaultValue));
    }

    public void sendLang(@NotNull CommandSender sender, @NotNull LangPath path) {
        if (prefixComp != null) {
            sender.sendMessage(prefixComp.append(MiniMessage.miniMessage().deserialize(lang.getString(path.path, path.defaultValue))));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString(path.path, path.defaultValue)));
            plugin.getLogger().severe("send a message in chat, but the prefix was null. Pretty sure the language file was never loaded.");
        }
    }

    public boolean isSignComp(@NotNull Component compToTest, @NotNull LangPath langPath) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(compToTest).trim();

        return strToTest.equalsIgnoreCase(nakedSigns.get(langPath));
    }

    public boolean isTimerSignComp(@NotNull Component compToTest) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(compToTest).trim();

        String[] splitted = nakedSigns.get(LangPath.timerSign).split("@", 2);
        return strToTest.startsWith(splitted[0]) && strToTest.endsWith(splitted[1]);
    }

    public int getTimer(Component compToTest) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(compToTest).trim();
        String[] splitted = nakedSigns.get(LangPath.timerSign).split("@", 2);

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
        pluginPrefix("prefix", "&6[GreenLocker]&r"), //todo miniMessage

        privateSign("sign.line.private", "[Private]"),
        @Deprecated(forRemoval = true)
        additionalSign("sign.line.additional", "[More Users]"),
        everyoneSign("sign.line.everyone", "[Everyone]"),
        timerSign("sign.line.everyone", "[Timer:@]"),
        expireSign("sign.line.expire", "&3[Expired]"), //todo miniMessage
        errorSign("sign.line.error", "[Error]"),
        invalidSign("sign.line.invalid", "[Invalid]"),

        helpHeader("cmd.help.header"),
        helpNoPermissionSubcommand("cmd.help.no-perm-subcommand"),
        helpAddMember("cmd.help.add-member"),
        helpRemoveMember("cmd.help.remove-member"),
        helpAddOwner("cmd.help.add-owner"),
        helpRemoveOwner("cmd.help.remove-owner"),
        helpDebug("cmd.help.debug"),
        helpHelp("cmd.help.help"),
        helpInfo("cmd.help.info"),
        helpReload("cmd.help.reload"),
        helpUpdateSign("cmd.help.update-sign"),
        helpVersion("cmd.help.version"),

        addOwnerSuccess("cmd.add-owner.success"),
        removeOwnerSuccess("cmd.remove-owner.success"),
        removeOwnerError("cmd.remove-owner.error"),
        addMemberSuccess("cmd.add-member.success"),
        removeMemberSuccess("cmd.remove-member.success"),
        removeMemberError("cmd.remove-owner.error"),
        updateSignSuccess("cmd.sign-update.success"),
        infoOwners("cmd.info.owners"),
        infoMembers("cmd.info.members"),
        reloadSuccess("cmd.reload.success"),

        signNeedReselect("cmd.sign-need-reselect"),
        signNotSelected("cmd.no-sign-selected"),
        unknownPlayer("cmd.unknown-player"),
        notAPlayer("cmd.not-a-player"),
        notEnoughArgs("cmd.not-enough-args"),
        cmdUsage("cmd.usage"),

        noPermission("no-permission"),
        notOwner("lock.not-owner"),

        lockSuccess("action.lock.success"),
        quickLockError("action.quick-lock.error"),
        lockErrorAlreadyLocked("action.lock.error.already-locked"),
        lockErrorNotLockable("action.lock.error.not-lockable"),
        selectSign("action.select-sign.success"),
        breakLockSuccess("action.break-lock.success"),
        actionPreventedLocked("action.prevented.locked"),
        actionPreventedInterfere("action.prevented.interfere-with-others"),

        noticeQuickLock("notice.quick-lock"),
        noticeManuelLock("notice.manual-lock");

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
