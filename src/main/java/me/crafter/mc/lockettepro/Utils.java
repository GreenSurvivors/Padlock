package me.crafter.mc.lockettepro;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Utils {
    private static final Pattern usernamePattern = Pattern.compile("^[a-zA-Z0-9_]*$");
    private final static NamespacedKey storedUUIDKey = new NamespacedKey(LockettePro.getPlugin(), "UUIDs");
    private static final LoadingCache<UUID, Block> selectedsign = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                public Block load(UUID key) {
                    return null;
                }
            });
    private static final Set<UUID> notified = new HashSet<>();
    private static final UUUIDListDataType uuidListData = new UUUIDListDataType();

    // Helper functions
    public static Block putSignOn(Block block, BlockFace blockface, String line1, String line2, Material material) {
        Block newsign = block.getRelative(blockface);
        Material blockType = Material.getMaterial(material.name().replace("_SIGN", "_WALL_SIGN"));
        if (blockType != null && Tag.WALL_SIGNS.isTagged(blockType)) {
            newsign.setType(blockType);
        } else {
            newsign.setType(Material.OAK_WALL_SIGN);
        }
        BlockData data = newsign.getBlockData();
        if (data instanceof Directional) {
            ((Directional) data).setFacing(blockface);
            newsign.setBlockData(data, true);
        }
        updateSign(newsign);
        Sign sign = (Sign) newsign.getState();
        if (newsign.getType() == Material.DARK_OAK_WALL_SIGN) {
            sign.getSide(Side.FRONT).setColor(DyeColor.WHITE);
        }
        sign.getSide(Side.FRONT).setLine(0, line1);
        sign.getSide(Side.FRONT).setLine(1, line2);
        sign.setWaxed(true);
        sign.update();
        return newsign;
    }

    private static void setUUID(@NotNull final Sign sign, int i, @NotNull final String uuid) {
        PersistentDataContainer container = sign.getPersistentDataContainer();

        Map<Integer, String> map = container.get(storedUUIDKey, uuidListData);
        if (map == null) {
            map = new HashMap<>(4);
        }

        map.put(i, uuid);

        sign.getPersistentDataContainer().set(storedUUIDKey, uuidListData, map);
        sign.update(); // update the block to make the UUID change effective
    }

    private static @Nullable Map<Integer, String> getUUIDs(@NotNull final Sign sign) {
        return sign.getPersistentDataContainer().get(storedUUIDKey, uuidListData);
    }

    public static void setSignLine(Sign sign, int line, String text) {
        sign.getSide(Side.FRONT).setLine(line, text);
        sign.update();
    }

    public static void removeASign(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (player.getInventory().getItemInMainHand().getAmount() == 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        }
    }

    public static void updateSign(Block block) {
        block.getState().update();
    }

    public static Block getSelectedSign(Player player) {
        Block b = selectedsign.getIfPresent(player.getUniqueId());
        if (b != null && !player.getWorld().getName().equals(b.getWorld().getName())) {
            selectedsign.invalidate(player.getUniqueId());
            return null;
        }
        return b;
    }

    public static void selectSign(Player player, Block block) {
        selectedsign.put(player.getUniqueId(), block);
    }

    public static void playLockEffect(Player player, Block block) {
//		player.playSound(block.getLocation(), Sound.DOOR_CLOSE, 0.3F, 1.4F);
//		player.spigot().playEffect(block.getLocation().add(0.5, 0.5, 0.5), Effect.CRIT, 0, 0, 0.3F, 0.3F, 0.3F, 0.1F, 64, 64);
    }

    public static void playAccessDenyEffect(Player player, Block block) {
//		player.playSound(block.getLocation(), Sound.VILLAGER_NO, 0.3F, 0.9F);
//		player.spigot().playEffect(block.getLocation().add(0.5, 0.5, 0.5), Effect.FLAME, 0, 0, 0.3F, 0.3F, 0.3F, 0.01F, 64, 64);
    }

    public static void sendMessages(@NotNull CommandSender sender, @Nullable String messages) {
        if (messages == null || messages.isEmpty()) return;
        sender.sendMessage(messages);
    }

    public static boolean shouldNotify(Player player) {
        if (notified.contains(player.getUniqueId())) {
            return false;
        } else {
            notified.add(player.getUniqueId());
            return true;
        }
    }

    public static boolean hasValidCache(Block block) {
        List<MetadataValue> metadatas = block.getMetadata("expires");
        if (!metadatas.isEmpty()) {
            long expires = metadatas.get(0).asLong();
            return expires > System.currentTimeMillis();
        }
        return false;
    }

    public static boolean getAccess(Block block) { // Requires hasValidCache()
        List<MetadataValue> metadatas = block.getMetadata("locked");
        return metadatas.get(0).asBoolean();
    }

    public static void setCache(Block block, boolean access) {
        block.removeMetadata("expires", LockettePro.getPlugin());
        block.removeMetadata("locked", LockettePro.getPlugin());
        block.setMetadata("expires", new FixedMetadataValue(LockettePro.getPlugin(), System.currentTimeMillis() + Config.getCacheTimeMillis()));
        block.setMetadata("locked", new FixedMetadataValue(LockettePro.getPlugin(), access));
    }

    public static void resetCache(Block block) {
        block.removeMetadata("expires", LockettePro.getPlugin());
        block.removeMetadata("locked", LockettePro.getPlugin());
        for (BlockFace blockface : LocketteProAPI.newsfaces) {
            Block relative = block.getRelative(blockface);
            if (relative.getType() == block.getType()) {
                relative.removeMetadata("expires", LockettePro.getPlugin());
                relative.removeMetadata("locked", LockettePro.getPlugin());
            }
        }
    }

    public static void updateUuidOnSign(@NotNull Sign sign) {
        for (int line = 1; line < 4; line++) {
            updateUuidByUsername(sign, line);
        }
    }

    public static void updateUuidByUsername(@NotNull Sign sign, int line) {
        final String original = sign.getSide(Side.FRONT).getLine(line);
        Bukkit.getScheduler().runTaskAsynchronously(LockettePro.getPlugin(), () -> {

            //clean name of UUID
            final String username;
            if (original.contains("#")) {
                username = original.split("#")[0];
            } else {
                username = original;
            }

            if (!isUserName(username)) return;
            final String uuid;
            Player user = Bukkit.getPlayerExact(username);
            if (user != null) { // User is online
                uuid = user.getUniqueId().toString();
            } else { // User is not online, fetch string
                uuid = getUuidByUsernameFromMojang(username);
            }

            if (uuid != null) {
                Bukkit.getScheduler().runTask(LockettePro.getPlugin(), () -> {
                    setSignLine(sign, line, username);
                    setUUID(sign, line, uuid);
                });
            }
        });
    }

    public static void updateUsernameByUuid(@NotNull Sign sign, int line) {
        String original = sign.getSide(Side.FRONT).getLine(line);
        String uuidStr = sign.getPersistentDataContainer().get(storedUUIDKey, PersistentDataType.STRING);

        //if we didn't get any UUID from persistent data container, try legacy way of written on the sign
        if (uuidStr == null) {
            if (isUsernameUuidLine(original)) {
                uuidStr = getUuidFromLine(original);
            }
        }

        if (uuidStr != null) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuidStr));

            if (player != null) {
                setSignLine(sign, line, player.getName());
            }
        }
    }

    public static void updateLineByPlayer(@NotNull Sign sign, @NotNull Player player, int line) {
        setSignLine(sign, line, player.getName());
        setUUID(sign, line, player.getUniqueId().toString());
    }

    public static void updateLineWithTime(Sign sign, boolean noexpire) {
        if (noexpire) {
            sign.getSide(Side.FRONT).setLine(0, sign.getSide(Side.FRONT).getLine(0) + "#created:" + -1);
        } else {
            sign.getSide(Side.FRONT).setLine(0, sign.getSide(Side.FRONT).getLine(0) + "#created:" + (int) (System.currentTimeMillis() / 1000));
        }
        sign.update();
    }

    public static boolean isUserName(String text) {
        return text.length() < 17 && text.length() > 2 && usernamePattern.matcher(text).matches();
    }

    // Warning: don't use this in a sync way
    public static String getUuidByUsernameFromMojang(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            String responsestring = response.toString();
            JsonObject json = JsonParser.parseString(responsestring).getAsJsonObject();
            String rawuuid = json.get("id").getAsString();
            return rawuuid.substring(0, 8) + "-" + rawuuid.substring(8, 12) + "-" + rawuuid.substring(12, 16) + "-" + rawuuid.substring(16, 20) + "-" + rawuuid.substring(20);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * @param text to search a UUID in
     * @return true if the text might contain a UUID
     * @deprecated use {@link #getUUIDs(Sign)} instead
     */
    @Deprecated
    public static boolean isUsernameUuidLine(@NotNull String text) {
        if (text.contains("#")) {
            String[] splitted = text.split("#", 2);
            return splitted[1].length() == 36;
        }
        return false;
    }

    public static boolean isPrivateTimeLine(@NotNull String text) {
        if (text.contains("#")) {
            String[] splitted = text.split("#", 2);
            return splitted[1].startsWith("created:");
        }
        return false;
    }

    public static String StripSharpSign(String text) {
        if (text.contains("#")) {
            return text.split("#", 2)[0];
        } else {
            return text;
        }
    }

    public static @NotNull String getUsernameFromLine(@NotNull String text) {
        if (isUsernameUuidLine(text)) {
            return text.split("#", 2)[0];
        } else {
            return text;
        }
    }

    /**
     * @param text Text to try to get the UUID from
     * @return legacy UUID as string, might be null if not found
     * @deprecated use {@link #getUUIDs(Sign)} instead
     */
    @Deprecated
    public static @Nullable String getUuidFromLine(@NotNull String text) {
        if (isUsernameUuidLine(text)) {
            return text.split("#", 2)[1];
        } else {
            return null;
        }
    }

    public static long getCreatedFromLine(String text) {
        if (isPrivateTimeLine(text)) {
            return Long.parseLong(text.split("#created:", 2)[1]);
        } else {
            return Config.getLockDefaultCreateTimeUnix();
        }
    }

    public static boolean isPlayerOnLine(@NotNull Player player, @NotNull Sign sign, int i) {
        Map<Integer, String> uuidMap = getUUIDs(sign);

        if (uuidMap == null) {

            String text = sign.getSide(Side.FRONT).getLine(i);

            if (Utils.isUsernameUuidLine(text)) {
                return player.getUniqueId().toString().equals(getUuidFromLine(text));
            } else {
                return text.equals(player.getName());
            }
        } else {
            return player.getUniqueId().toString().equals(uuidMap.get(i));
        }
    }

    public static String getSignLineFromUnknown(WrappedChatComponent rawline) {
        String json = rawline.getJson();
        return getSignLineFromUnknown(json);
    }

    public static String getSignLineFromUnknown(String json) {
        JsonObject line = getJsonObjectOrNull(json);
        if (line == null) return json;
        StringBuilder result = new StringBuilder();
        if (line.has("text")) {
            result.append(line.get("text").getAsString());
        }
        if (line.has("extra")) {
            try {
                result.append(line.get("extra").getAsJsonArray().get(0).getAsJsonObject().get("text").getAsString());
            } catch (Exception ignored) {
            }
        }

        return result.toString();
    }

    @Nullable
    public static JsonObject getJsonObjectOrNull(String json) {
        int i = json.indexOf("{");
        if (i < 0) return null;
        if (json.indexOf("}", 1) < 0) return null;
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException e) {
            return null;
        }
    }

    private static class UUUIDListDataType implements PersistentDataType<String, Map<Integer, String>> {
        /**
         * Returns the primitive data type of this tag.
         *
         * @return the class
         */
        @NotNull
        @Override
        public Class<String> getPrimitiveType() {
            return String.class;
        }

        /**
         * Returns the complex object type the primitive value resembles.
         *
         * @return the class type
         */
        @NotNull
        @Override
        public Class<Map<Integer, String>> getComplexType() {
            return ((Class<Map<Integer, String>>) ((Class<?>) Map.class));
        }

        /**
         * Returns the primitive data that resembles the complex object passed to
         * this method.
         *
         * @param complex the complex object instance
         * @param context the context this operation is running in
         * @return the primitive value
         */
        @Override
        public @NotNull String toPrimitive(@NotNull Map<Integer, String> complex, @NotNull PersistentDataAdapterContext context) {
            Gson gson = new Gson();

            return gson.toJson(complex);
        }

        /**
         * Creates a complex object based of the passed primitive value
         *
         * @param primitive the primitive value
         * @param context   the context this operation is running in
         * @return the complex object instance
         */
        @NotNull
        @Override
        public Map<Integer, String> fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<Integer, String>>() {
            }.getType();

            return gson.fromJson(primitive, type);
        }
    }

}
