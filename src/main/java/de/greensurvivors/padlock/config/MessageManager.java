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
import java.util.stream.Collectors;

/**
 * manages all translatable and placeholders used by this plugin.
 */
public class MessageManager {
    private final Plugin plugin;
    /**
     * contains all sign lines without any decorations like color but still with every placeholder
     */
    private final HashMap<LangPath, String> nakedSignLines = new HashMap<>(); // path -> naked
    @Deprecated(forRemoval = true)
    private final HashMap<LangPath, Set<String>> nakedLegacySignLines = new HashMap<>();
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
            return lang.getString(path.getPath());
        } catch (MissingResourceException | ClassCastException e) {
            plugin.getLogger().log(Level.WARNING, "couldn't find path: \"" + path.getPath() + "\" in lang files using fallback.", e);
            return path.getDefaultValue();
        }
    }

    private @NotNull Set<@NotNull String> getStringSetFromLang(@NotNull LangPath path) {
        String value;
        try {
            value = lang.getString(path.getPath());
        } catch (MissingResourceException | ClassCastException e) {
            plugin.getLogger().log(Level.WARNING, "couldn't find path: \"" + path.getPath() + "\" in lang files using fallback.", e);
            value = path.getDefaultValue();
        }

        return Set.of(value.split("\\s?+,\\s?+"));
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
        langCache.cleanUp();
        langCache.asMap().clear();

        nakedSignLines.put(LangPath.PRIVATE_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.PRIVATE_SIGN)).toLowerCase());
        nakedSignLines.put(LangPath.PUBLIC_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.PUBLIC_SIGN)).toLowerCase());
        nakedSignLines.put(LangPath.DONATION_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.DONATION_SIGN)).toLowerCase());
        nakedSignLines.put(LangPath.DISPLAY_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.DISPLAY_SIGN)).toLowerCase());
        nakedSignLines.put(LangPath.SUPPLY_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.SUPPLY_SIGN)).toLowerCase());
        nakedSignLines.put(LangPath.TIMER_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.TIMER_SIGN)).toLowerCase());

        nakedLegacySignLines.put(LangPath.LEGACY_ADDITIONAL_SIGN, getStringSetFromLang(LangPath.LEGACY_ADDITIONAL_SIGN).stream().map(s -> MiniMessage.miniMessage().stripTags(s).toLowerCase()).collect(Collectors.toSet()));
        nakedLegacySignLines.put(LangPath.LEGACY_EVERYONE_SIGN, getStringSetFromLang(LangPath.LEGACY_EVERYONE_SIGN).stream().map(s -> MiniMessage.miniMessage().stripTags(s).toLowerCase()).collect(Collectors.toSet()));
        nakedLegacySignLines.put(LangPath.LEGACY_PRIVATE_SIGN, getStringSetFromLang(LangPath.LEGACY_PRIVATE_SIGN).stream().map(s -> MiniMessage.miniMessage().stripTags(s).toLowerCase()).collect(Collectors.toSet()));
        nakedLegacySignLines.put(LangPath.LEGACY_TIMER_SIGN, getStringSetFromLang(LangPath.LEGACY_TIMER_SIGN).stream().map(s -> MiniMessage.miniMessage().stripTags(s).toLowerCase()).collect(Collectors.toSet()));
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
        audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).append(Component.text(" ")).append(messages));
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
        audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).append(Component.text(" ")).append(langCache.get(path)));
    }

    /**
     * send a component from the lang file to the audience, prefixed with this plugins prefix and applying the given tag resolver.
     * Note: might be slightly slower than {@link #sendLang(Audience, LangPath)} since this can not use cache.
     */
    public void sendLang(@NotNull Audience audience, @NotNull LangPath path, @NotNull TagResolver resolver) {
        audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).append(Component.text(" ")).append(
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

        return strToTest.equalsIgnoreCase(nakedSignLines.get(langPath));
    }

    @Deprecated(forRemoval = true)
    public boolean isLegacySignComp(@NotNull Component compToTest, @NotNull LangPath langPath) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(compToTest).toLowerCase().trim();

        for (String legacyLine : nakedLegacySignLines.get(langPath)) {
            if (strToTest.startsWith(legacyLine)) {
                return true;
            }
        }

        return false;
    }

    /**
     * get the line of a sign without any decorations, but still with its placeholders.
     *
     * @param langPath defines the type of line we need
     */
    public @Nullable String getNakedSignText(@NotNull LangPath langPath) {
        return nakedSignLines.get(langPath);
    }

    @Deprecated(forRemoval = true)
    public @Nullable Set<@NotNull String> getNakedLegacyText(@NotNull LangPath langPath) {
        return nakedLegacySignLines.get(langPath);
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
        PUBLIC_SIGN("sign.line.public", "[Public]"),
        DONATION_SIGN("sign.line.donation", "[Donation]"),
        DISPLAY_SIGN("sign.line.display", "[Display]"),
        SUPPLY_SIGN("sign.line.supply", "[Supply]"),
        TIMER_SIGN("sign.line.timer", "[Timer:<" + PlaceHolder.TIME.getPlaceholder() + ">]"),
        ERROR_SIGN("sign.line.error", "[<dark_red>Error</dark_red>]"),
        INVALID_SIGN("sign.line.invalid", "[Invalid]"),
        PLAYER_NAME_ON_SIGN("sign.line.player-name", "<" + PlaceHolder.PLAYER.getPlaceholder() + ">"), // used for formatting displayed player names
        MORE_USERS_ON_SIGN("sign.line.more-users", "[More Users]"), // not the same as the LEGACY_ADDITIONAL_SIGN line. This indicates that there are users that can't get displayed.

        @Deprecated(forRemoval = true)
        LEGACY_ADDITIONAL_SIGN("sign.legacy.additional", "[More Users]"),
        @Deprecated(forRemoval = true)
        LEGACY_EVERYONE_SIGN("sign.legacy.everyone", "[Everyone]"),
        @Deprecated(forRemoval = true)
        LEGACY_PRIVATE_SIGN("sign.legacy.private", "[Private]"),
        @Deprecated(forRemoval = true)
        LEGACY_TIMER_SIGN("sign.legacy.timer", "[Timer:<timer>]"),

        HELP_HEADER("cmd.help.header"),
        HELP_ADD_MEMBER("cmd.help.add-member"),
        HELP_REMOVE_MEMBER("cmd.help.remove-member"),
        HELP_ADD_OWNER("cmd.help.add-owner"),
        HELP_REMOVE_OWNER("cmd.help.remove-owner"),
        HELP_SET_ACCESS_TYPE("cmd.help.set-access-type"),
        HELP_SET_CONNECTED("cmd.help.set-connected"),
        HELP_SET_PASSWORD("cmd.help.set-password"),
        HELP_SET_TIMER("cmd.help.set-timer"),
        HELP_DEBUG("cmd.help.debug"),
        HELP_HELP("cmd.help.help"),
        HELP_PASSWORD("cmd.help.password"),
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
        SET_ACCESS_TYPE_SUCCESS("cmd.set-access-type.success"),
        SET_PASSWORD_SUCCESS("cmd.set-password.set.success"),
        SET_PASSWORD_REMOVE_SUCCESS("cmd.set-password.remove.success"),
        SET_TIMER_SUCCESS_ON("cmd.set-timer.success.turned-on"),
        SET_TIMER_SUCCESS_OFF("cmd.set-timer.success.turned-off"),
        SET_TIMER_ERROR("cmd.set-timer.error"),
        UPDATE_SIGN_SUCCESS("cmd.sign-update.success"),
        PASSWORD_ACCESS_GRANTED("cmd.password.access-granted"),
        PASSWORD_WRONG_PASSWORD("cmd.password.wrong-password"),
        PASSWORD_ON_COOLDOWN("cmd.password.on-cooldown"),
        PASSWORD_SAFETY_WARNING("cmd.password.safety-warning", "<dark_red>Warning: never use a password, you are using anywhere else! While I did everything I could for your safety, there <bold>ARE</bold> ways your password could get leaked!</dark_red>"),
        PASSWORD_START_PROCESSING("cmd.password.start-processing"),
        RELOAD_SUCCESS("cmd.reload.success"),
        SET_CONNECTED_SUCCESS("cmd.set-connected.success"),

        INFO_OWNERS("cmd.info.owners"),
        INFO_MEMBERS("cmd.info.members"),
        INFO_TIMER("cmd.info.timer"),
        INFO_ACCESS_TYPE("cmd.info.access-type"),
        INFO_EXPIRED("cmd.info.expired"),

        SIGN_NEED_RESELECT("cmd.error.sign-need-reselect"),
        SIGN_NOT_SELECTED("cmd.error.no-sign-selected"),
        UNKNOWN_PLAYER("cmd.error.unknown-player"),
        NOT_A_PLAYER("cmd.error.not-a-player"),
        NOT_ENOUGH_ARGS("cmd.error.not-enough-args"),
        NOT_ACCESS_TYPE("cmd.error.not-access-type"),
        NOT_A_BOOL("cmd.error.not-a-bool"),
        CMD_USAGE("cmd.usage"),
        CMD_NOT_A_SUBCOMMAND("cmd.not-a-subcommand"),

        NO_PERMISSION("no-permission"),
        NOT_OWNER("lock.not-owner"),
        NEEDS_PASSWORD("lock.needs-password"),

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
