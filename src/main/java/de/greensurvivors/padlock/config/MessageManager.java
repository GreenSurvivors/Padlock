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

        nakedSignLines.put(LangPath.SIGN_LINE_PRIVATE, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.SIGN_LINE_PRIVATE)).toLowerCase());
        nakedSignLines.put(LangPath.SIGN_LINE_PUBLIC, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.SIGN_LINE_PUBLIC)).toLowerCase());
        nakedSignLines.put(LangPath.SIGN_LINE_DONATION, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.SIGN_LINE_DONATION)).toLowerCase());
        nakedSignLines.put(LangPath.SIGN_LINE_DISPLAY, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.SIGN_LINE_DISPLAY)).toLowerCase());
        nakedSignLines.put(LangPath.SIGN_LINE_SUPPLY_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.SIGN_LINE_SUPPLY_SIGN)).toLowerCase());
        nakedSignLines.put(LangPath.SIGN_LINE_TIMER_SIGN, MiniMessage.miniMessage().stripTags(getStringFromLang(LangPath.SIGN_LINE_TIMER_SIGN)).toLowerCase());

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
        ARGUMENT("argument"),
        PLAYER("player"),
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
        HELP_UPDATE_SIGN("cmd.help.update-sign"),
        HELP_VERSION("cmd.help.version"),
        //info cmd
        INFO_ACCESS_TYPE("cmd.info.access-type"),
        INFO_EXPIRED("cmd.info.expired"),
        INFO_MEMBERS("cmd.info.members"),
        INFO_OWNERS("cmd.info.owners"),
        INFO_TIMER("cmd.info.timer"),
        //legacy
        @Deprecated(forRemoval = true)
        LEGACY_ADDITIONAL_SIGN("sign.legacy.additional", "[More Users]"),
        @Deprecated(forRemoval = true)
        LEGACY_EVERYONE_SIGN("sign.legacy.everyone", "[Everyone]"),
        @Deprecated(forRemoval = true)
        LEGACY_PRIVATE_SIGN("sign.legacy.private", "[Private]"),
        @Deprecated(forRemoval = true)
        LEGACY_TIMER_SIGN("sign.legacy.timer", "[Timer:<timer>]"),
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
        SIGN_MORE_USERS("sign.line.more-users", "[More Users]"), // not the same as the LEGACY_ADDITIONAL_SIGN line. This indicates that there are users that can't get displayed.
        //
        SIGN_NEED_RESELECT("cmd.error.sign-need-reselect"),
        SIGN_NOT_SELECTED("cmd.error.no-sign-selected"),
        SIGN_PLAYER_NAME_ON("sign.line.player-name", "<" + PlaceHolder.PLAYER.getPlaceholder() + ">"), // used for formatting displayed player names
        UNKNOWN_PLAYER("cmd.error.unknown-player"),
        UPDATE_SIGN_SUCCESS("cmd.sign-update.success");

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
