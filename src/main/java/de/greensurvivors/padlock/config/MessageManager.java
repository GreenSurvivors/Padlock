package de.greensurvivors.padlock.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.apache.commons.io.FileUtils;
import org.bukkit.plugin.Plugin;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * manages all translatable and placeholders used by this plugin.
 */
public class MessageManager {
    private final Plugin plugin;
    /**
     * contains all sign lines without any decorations like color but still with every placeholder
     */
    private final HashMap<LangPath, String> nakedSignLiness = new HashMap<>(); // path -> naked
    private ResourceBundle lang;
    /**
     * caches every component without placeholder for faster access in future and loads missing values automatically
     */
    private final LoadingCache<LangPath, Component> langCache = Caffeine.newBuilder().build(
            path -> MiniMessage.miniMessage().deserialize(getStringFromLang(path)));
    private String langfilename = "lang/lang_en.yml";

    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    protected void setLangFileName(String langfilename) {
        this.langfilename = langfilename;
    }

    private @NotNull String getStringFromLang(@NotNull LangPath path) {
        try {
            lang.getString(path.getPath());
            return lang.getString(path.getPath());
        } catch (MissingResourceException | ClassCastException e) {
            plugin.getLogger().log(Level.WARNING, "couldn't find path: \"" + path.getPath() + "\" in lang files using fallback.", e);
            return path.getDefaultValue();
        }
    }

    /**
     * reload language file. Please call {@link #setLangFileName(String)} before calling this for the first time
     */
    protected void reload() {
        Locale locale = Locale.forLanguageTag(langfilename);
        plugin.getLogger().info("Locale set to language: " + locale.toLanguageTag());
        File langFile = new File(new File(plugin.getDataFolder(), "lang"), UTF8ResourceBundleControl.get().toBundleName("lang", locale) + ".properties");

        if (!langFile.exists()) {
            // save all of them
            initLangFiles();
        }

        lang = null;
        if (langFile.exists()) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
                lang = new PropertyResourceBundle(inputStreamReader);
            } catch (FileNotFoundException ignored) {
                plugin.getLogger().info("No translation file found. Using internal");
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "IO exception while reading lang bundle", e);
            }
        }

        if (lang == null) {
            try {
                lang = PropertyResourceBundle.getBundle("lang", locale, plugin.getClass().getClassLoader(), new UTF8ResourceBundleControl());
            } catch (MissingResourceException e) {
                plugin.getLogger().log(Level.SEVERE, "Couldn't get Ressource bundle \"lang\" for locale \"" + locale.toLanguageTag() + "\". Messages WILL be broken!", e);
            }
        }

        // clear component cache
        langCache.invalidateAll();
        langCache.asMap().clear();

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
        Enumeration<URL> en;
        try {
            en = plugin.getClass().getClassLoader().getResources("lang.properties");

            while (en.hasMoreElements()) {
                URL url = en.nextElement();
                JarURLConnection urlcon;
                urlcon = (JarURLConnection) (url.openConnection());

                JarFile jar = urlcon.getJarFile();
                Enumeration<JarEntry> entries = jar.entries();
                boolean found = false;

                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();

                    String entry = jarEntry.getName();

                    if (entry.startsWith("lang_")) { // never save the fallback
                        try {
                            InputStream inputStream = plugin.getResource(entry);
                            File langFile = new File(new File(plugin.getDataFolder(), "lang"), entry);
                            FileUtils.copyInputStreamToFile(inputStream, langFile); // It should never be null, we already found the resource!
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().log(Level.WARNING, "Couldn't save lang file \"" + entry + "\".", e);
                        }

                        found = true;
                    } else if (found && entry.contains("/")) {
                        break; // already over it
                    }
                }

                jar.close();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Couldn't save lang files", e);
        }
    }

    /**
     * prepend the message with the plugins prefix before sending it to the audience.
     */
    public void sendMessageWithPrefix(@NotNull Audience audience, @NotNull Component messages) {
        audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).append(messages));
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
        return langCache.get(path);
    }

    /**
     * send a component from the lang file to the audience, prefixed with this plugins prefix.
     */
    public void sendLang(@NotNull Audience audience, @NotNull LangPath path) {
        audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).append(langCache.get(path)));
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
        SET_EVERYONE_SUCCESS("cmd.set-everyone.success"),
        SET_TIMER_SUCCESS_ON("cmd.set-timer.success.turned-on"),
        SET_TIMER_SUCCESS_OFF("cmd.set-timer.success.turned-off"),
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
        ACTION_PREVENTED_USE_CMDS("action.prevented.use-commands"),

        NOTICE_QUICK_LOCK("notice.quick-lock"),
        NOTICE_MANUEL_LOCK("notice.manual-lock");

        private final String path;
        private final String defaultValue;

        LangPath(String path) {
            this.path = path;
            this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
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
