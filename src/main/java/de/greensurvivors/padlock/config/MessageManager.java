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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * manages all translatable and placeholders used by this plugin.
 */
public class MessageManager {
    private final String BUNDLE_NAME = "lang";
    final Pattern BUNDLE_FILE_NAME_PATTERN = Pattern.compile(BUNDLE_NAME + "(?:_.*)?.properties");
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

    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
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
     * reload language file.
     */
    protected void reload(String langfilename) {
        lang = null; // reset last bundle

        // save all missing keys
        initLangFiles();

        Locale locale = Locale.forLanguageTag(langfilename.replace("_", "-"));
        plugin.getLogger().info("Locale set to language: " + locale.toLanguageTag());
        File langDictionary = new File(plugin.getDataFolder(), BUNDLE_NAME);

        URL[] urls;
        try {
            urls = new URL[]{langDictionary.toURI().toURL()};
            lang = ResourceBundle.getBundle(BUNDLE_NAME, locale, new URLClassLoader(urls), UTF8ResourceBundleControl.get());

        } catch (SecurityException | MalformedURLException e) {
            plugin.getLogger().log(Level.WARNING, "Exception while reading lang bundle. Using internal", e);
        } catch (MissingResourceException ignored) { // how? missing write access?
            plugin.getLogger().log(Level.WARNING, "No translation file for " + UTF8ResourceBundleControl.get().toBundleName(BUNDLE_NAME, locale) + " found on disc. Using internal");
        }

        if (lang == null) { // fallback, since we are always trying to save defaults this never should happen
            try {
                lang = PropertyResourceBundle.getBundle(BUNDLE_NAME, locale, plugin.getClass().getClassLoader(), new UTF8ResourceBundleControl());
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

    private String saveConvert(String theString, boolean escapeSpace) {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuilder convertedStrBuilder = new StringBuilder(bufLen);

        for (int i = 0; i < theString.length(); i++) {
            char aChar = theString.charAt(i);
            // Handle common case first
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    if (i + 1 < theString.length()) {
                        final char bChar = theString.charAt(i + 1);
                        if (bChar == ' ' || bChar == 't' || bChar == 'n' || bChar == 'r' ||
                                bChar == 'f' || bChar == '\\' || bChar == 'u' || bChar == '=' ||
                                bChar == ':' || bChar == '#' || bChar == '!') {
                            // don't double escape already escaped chars
                            convertedStrBuilder.append(aChar);
                            convertedStrBuilder.append(bChar);
                            i++;
                            continue;
                        } else {
                            // any other char following
                            convertedStrBuilder.append('\\');
                        }
                    } else {
                        // last char was a backslash. escape!
                        convertedStrBuilder.append('\\');
                    }
                }
                convertedStrBuilder.append(aChar);
                continue;
            }

            // escape non escaped chars that have to get escaped
            switch (aChar) {
                case ' ' -> {
                    if (escapeSpace) {
                        convertedStrBuilder.append('\\');
                    }
                    convertedStrBuilder.append(' ');
                }
                case '\t' -> convertedStrBuilder.append("\\t");
                case '\n' -> convertedStrBuilder.append("\\n");
                case '\r' -> convertedStrBuilder.append("\\r");
                case '\f' -> convertedStrBuilder.append("\\f");
                case '=', ':', '#', '!' -> {
                    convertedStrBuilder.append('\\');
                    convertedStrBuilder.append(aChar);
                }
                default -> convertedStrBuilder.append(aChar);
            }
        }

        return convertedStrBuilder.toString();
    }

    /**
     * saves all missing lang files from resources to the plugins datafolder
     */
    private void initLangFiles() {
        CodeSource src = this.getClass().getProtectionDomain().getCodeSource();
        if (src != null) {
            URL jarUrl = src.getLocation();
            try (ZipInputStream zipStream = new ZipInputStream(jarUrl.openStream())) {
                ZipEntry zipEntry;
                while ((zipEntry = zipStream.getNextEntry()) != null) {
                    if (zipEntry.isDirectory()) {
                        continue;
                    }

                    String entryName = zipEntry.getName();

                    if (BUNDLE_FILE_NAME_PATTERN.matcher(entryName).matches()) {
                        File langFile = new File(new File(plugin.getDataFolder(), BUNDLE_NAME), entryName);
                        if (!langFile.exists()) { // don't overwrite existing files
                            FileUtils.copyToFile(zipStream, langFile);
                        } else { // add defaults to file to expand in case there are key-value pairs missing
                            Properties defaults = new Properties();
                            // don't close reader, since we need the stream to be still open for the next entry!
                            defaults.load(new InputStreamReader(zipStream, StandardCharsets.UTF_8));

                            Properties current = new Properties();
                            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
                                current.load(reader);
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.WARNING, "couldn't get current properties file for " + entryName + "!", e);
                                continue;
                            }

                            try (FileWriter fw = new FileWriter(langFile, StandardCharsets.UTF_8, true);
                                 // we are NOT using Properties#store since it gets rid of comments and doesn't guarantee ordering
                                 BufferedWriter bw = new BufferedWriter(fw)) {
                                boolean updated = false; // only write comment once
                                for (Map.Entry<Object, Object> translationPair : defaults.entrySet()) {
                                    if (current.get(translationPair.getKey()) == null) {
                                        if (!updated) {
                                            bw.write("# New Values where added. Is everything else up to date? Time of update: " + new Date());
                                            bw.newLine();

                                            plugin.getLogger().fine("Updated langfile \"" + entryName + "\". Might want to check the new translation strings out!");

                                            updated = true;
                                        }

                                        String key = saveConvert((String) translationPair.getKey(), true);
                                        /* No need to escape embedded and trailing spaces for value, hence
                                         * pass false to flag.
                                         */
                                        String val = saveConvert((String) translationPair.getValue(), false);
                                        bw.write((key + "=" + val));
                                        bw.newLine();
                                    } // current already knows the key
                                } // end of for
                            } // end of try
                        } // end of else (file exists)
                    } // doesn't match
                } // end of elements
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Couldn't save lang files", e);
            }
        } else {
            plugin.getLogger().warning("Couldn't save lang files: no CodeSource!");
        }
    }

    /**
     * prepend the message with the plugins prefix before sending it to the audience.
     */
    public void sendMessageWithPrefix(@NotNull Audience audience, @NotNull Component messages) {
        audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).appendSpace().append(messages));
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
        audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).appendSpace().append(langCache.get(path)));
    }

    /**
     * send a component from the lang file to the audience, prefixed with this plugins prefix and applying the given tag resolver.
     * Note: might be slightly slower than {@link #sendLang(Audience, LangPath)} since this can not use cache.
     */
    public void sendLang(@NotNull Audience audience, @NotNull LangPath path, @NotNull TagResolver resolver) {
        audience.sendMessage(langCache.get(LangPath.PLUGIN_PREFIX).appendSpace().append(
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
