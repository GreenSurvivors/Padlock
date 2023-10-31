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
    private FileConfiguration lang;
    private String langfilename = "lang.yml";
    private String invalidString = "[Invalid]"; //todo^2
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

    public void reload() {//todo
        initDefaultConfig();
        initAdditionalFiles();
        lang = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), langfilename));


        privatestring = lang.getString("private-sign", "[Private]");
        additionalstring = lang.getString("additional-sign", "[More Users]");
        everyonestring = lang.getString("everyone-sign", "[Everyone]");
        timerstring = lang.getString("timer-sign", "[Timer:@]");

        lockexpirestring = ChatColor.translateAlternateColorCodes('&',
                lang.getString("lock-expire-string", "&3[Expired]"));
    }

    private void initDefaultConfig() {//todo remove
        plugin.saveDefaultConfig();

        lang.addDefault("private-signs", "[Private]");
        lang.addDefault("additional-signs", "[More Users]");
        lang.addDefault("everyone-signs", "[Everyone]");
        lang.addDefault("timer-signs", "[Timer:@]");
        lang.addDefault("lock-expire-string", "&3[Expired]");

        lang.options().copyDefaults(true);
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
    }

    public @NotNull Component getCmdHelp(String subCommand) {
        return getLang("cmd.help." + subCommand);
    }

    public @NotNull Component getLang(@NotNull String path) { // todo lang in minimessage format
        return MiniMessage.miniMessage().deserialize(lang.getString(path, ""));
    }

    public void sendLang(CommandSender sender, @NotNull String path) {
        sendMessages(sender, MiniMessage.miniMessage().deserialize(lang.getString(path, "")));
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
}
