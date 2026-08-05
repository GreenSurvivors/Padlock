package de.greensurvivors.padlock.language;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.greensurvivors.padlock.Padlock;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.io.FileUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
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
public class MessageManager /*extends MiniMessageTranslator*/ {
    protected static final @NotNull NamespacedKey TRANSLATOR_KEY = new NamespacedKey(Padlock.getPlugin(), "MessageManager");
    protected final @NotNull String BUNDLE_NAME = "lang";
    protected final @NotNull Pattern BUNDLE_FILE_NAME_PATTERN = Pattern.compile(BUNDLE_NAME + "(?:_.*)?.properties");
    protected final @NotNull Plugin plugin;
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
    private final @NotNull LoadingCache<@NotNull LangPath, @NotNull Component> langCache = Caffeine.newBuilder().build(
            path -> MiniMessage.miniMessage().deserialize(getStringFromLang(path)));

    public MessageManager(final @NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    private @NotNull String getStringFromLang(final @NotNull LangPath path) {
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
    public void reload(String langfilename) {
        lang = null; // reset last bundle

        // save all missing keys
        initLangFiles();

        Locale locale = Locale.forLanguageTag(langfilename.replace("_", "-"));
        plugin.getLogger().info("Locale set to language: " + locale.toLanguageTag());
        File langDictionary = new File(plugin.getDataFolder(), BUNDLE_NAME);

        URL[] urls;
        try {
            urls = new URL[]{langDictionary.toURI().toURL()};
            lang = ResourceBundle.getBundle(BUNDLE_NAME, locale, new URLClassLoader(urls));

        } catch (SecurityException | MalformedURLException e) {
            plugin.getLogger().log(Level.WARNING, "Exception while reading lang bundle. Using internal", e);
        } catch (MissingResourceException ignored) { // how? missing write access?
            plugin.getLogger().log(Level.WARNING, "No translation file for " + locale.toLanguageTag() + " found on disc. Using internal");
        }

        if (lang == null) { // fallback, since we are always trying to save defaults this never should happen
            try {
                lang = PropertyResourceBundle.getBundle(BUNDLE_NAME, locale, plugin.getClass().getClassLoader());
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
    public @NotNull Component getLang(@NotNull LangPath path, @NotNull TagResolver resolver) {
        return MiniMessage.miniMessage().deserialize(getStringFromLang(path), resolver);
    }

    /**
     * get a component from lang file
     */
    public @NotNull Component getLang(@NotNull LangPath path) {
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

    /*
    @Override
    protected @Nullable String getMiniMessageString(final @NotNull String key, final @NotNull Locale locale) {
        return null; // todo
    }

    @Override
    public @NotNull Key name() {
        return TRANSLATOR_KEY;
    }*/
}
