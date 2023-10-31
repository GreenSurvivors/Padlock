package me.crafter.mc.lockettepro.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class MessageManager {
    private final Plugin plugin;
    private String invalidString = "[Invalid]"; //todo^2
    private FileConfiguration lang;
    private String langfilename = "lang_en.yml";
    private String privatestring = "[Private]";
    private String additionalstring = "[More Users]";
    private String everyonestring = "[Everyone]";
    private String timerstring = "[Timer:@]";
    private String lockexpirestring = "&3[Expired]";
    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    protected void setLangFileName(String langfilename) {
        this.langfilename = langfilename;
    }

    protected void reload() {//todo
        initAdditionalFiles();
        lang = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), langfilename));


        //todo mini-message
        privatestring = lang.getString(LangPath.privateSign.getPath(), LangPath.privateSign.getDefaultValue());
        additionalstring = lang.getString(LangPath.additionalSign.getPath(), LangPath.additionalSign.getDefaultValue());
        everyonestring = lang.getString(LangPath.everyoneSign.getPath(), LangPath.everyoneSign.getDefaultValue());
        timerstring = lang.getString(LangPath.timerSign.getPath(), LangPath.timerSign.getDefaultValue());
        invalidString = lang.getString(LangPath.invalidSign.getPath(), LangPath.invalidSign.getDefaultValue());

        lockexpirestring = ChatColor.translateAlternateColorCodes('&',
                lang.getString(LangPath.expireSign.getPath(), LangPath.expireSign.getDefaultValue()));
    }

    private void initAdditionalFiles() {
        String[] availablefiles = {"lang_de.yml", "lang_en.yml", "lang_es.yml", "lang_hu.yml", "lang_it.yml", "lang_zh-cn.yml"}; //todo update
        for (String filename : availablefiles) {
            File langfile = new File(plugin.getDataFolder(), filename);
            if (!langfile.exists()) {
                plugin.saveResource(filename, false);
            }
        }
    }

    public void sendMessages(@NotNull CommandSender sender, @Nullable Component messages) { //todo preparation for MessageManager
        if (messages != null) {
            sender.sendMessage(messages);
        }
    }

    public String getLockExpireString() {
        return lockexpirestring;
    } //todo

    public @NotNull Component getLang(@NotNull MessageManager.LangPath path) { // todo lang in minimessage format
        return MiniMessage.miniMessage().deserialize(lang.getString(path.path, path.defaultValue));
    }

    public void sendLang(CommandSender sender, @NotNull LangPath path) {
        sendMessages(sender, MiniMessage.miniMessage().deserialize(lang.getString(path.path, path.defaultValue)));
    }

    public Component getInvalidString() {
        return MiniMessage.miniMessage().deserialize(invalidString);
    }

    public boolean isPrivateSignComp(Component message) { //todo ignore casing
        return MiniMessage.miniMessage().deserialize(privatestring).contains(message);
    }

    @Deprecated(forRemoval = true)
    public boolean isPrivateSignString(String message) {
        return privatestring.contains(message);
    }

    public boolean isAdditionalSignString(String message) {
        return additionalstring.contains(message);
    }

    public boolean isEveryoneSignString(String message) {
        return everyonestring.contains(message);
    } //todo

    public boolean isTimerSignString(String message) { //todo
        String[] splitted = timerstring.split("@", 2);
        return message.startsWith(splitted[0]) && message.endsWith(splitted[1]);
    }

    public int getTimer(String message) {
        String[] splitted = timerstring.split("@", 2);
        if (message.startsWith(splitted[0]) && message.endsWith(splitted[1])) {
            String newmessage = message.replace(splitted[0], "").replace(splitted[1], "");
            try {
                int seconds = Integer.parseInt(newmessage);
                return Math.min(seconds, 20);
            } catch (Exception ignored) {
            }
        }

        return 0;
    }

    public String getPrivateString() {
        return privatestring;
    }

    public String getDefaultAdditionalString() {
        return additionalstring;
    }

    public enum LangPath {
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
