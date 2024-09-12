package de.greensurvivors.padlock.impl.signdata;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.greensurvivors.padlock.Padlock;
import de.greensurvivors.padlock.PadlockAPI;
import de.greensurvivors.padlock.config.MessageManager;
import de.greensurvivors.padlock.impl.dataTypes.LazySignProperties;
import de.greensurvivors.padlock.impl.openabledata.Openables;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.UUID;

/**
 * This keeps track of whom is allowed to use a locked block and edit its sign.
 */
public class SignLock {
    // members can use and see /lock info but can't change anything
    private final static NamespacedKey storedMembersUUIDKey = new NamespacedKey(Padlock.getPlugin(), "MemberUUIDs");
    private final static NamespacedKey storedOwnersUUIDKey = new NamespacedKey(Padlock.getPlugin(), "OwnerUUIDs");
    private static final MemberUUUIDSetDataType uuidSetDataType = new MemberUUUIDSetDataType();
    private static final Gson gson = new Gson(); // cache Gson so it doesn't need to get recreated every time a sign gets read / written to
    private static final Type type = new TypeToken<ListOrderedSet<String>>() { // this is what Gson enables to (de)serialize the set
    }.getType();

    /**
     * Set a sign invalid. This is NOT an error and therefor does not share the Error line.
     * At the moment this marks legacy Lockette additional signs that where successfully read on
     * and their data was added onto the main lock sign.
     * An invalid sign will no longer recognized by the plugin.
     *
     * @param sign sign to set invalid
     */
    public static void setInvalid(@NotNull Sign sign) {
        sign.getSide(Side.FRONT).line(0, Padlock.getPlugin().getMessageManager().getLang(MessageManager.LangPath.SIGN_LINE_INVALID));
        sign.update();
    }

    /**
     * Check if the sign is a lock sign by checking it's first line against the line configured in the lang file.
     */
    public static boolean isLockSign(@NotNull Sign sign) {
        //todo first part is deprecated
        return (SignAccessType.getAccessTypeFromComp(sign.getSide(Side.FRONT).line(0)) != null) || SignAccessType.getAccessType(sign, true) != null;
    }

    /**
     * Check if a given uuid is a registered owner uuid of a lock sign.
     */
    public static boolean isOwner(final @NotNull Sign sign, final @NotNull UUID uuid) {
        if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            return Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(sign.getLocation()).getOwnerUUIDStrs().contains(uuid.toString());
        } else {
            return getUUIDs(sign, true, true).contains(uuid.toString());
        }
    }

    /**
     * Check if the sign grants everyone member access or if the uuid is a registered member of the lock sign.
     */
    public static boolean isMember(final @NotNull Sign sign, final @NotNull UUID uuid) {
        if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            LazySignProperties lazySignPropertys = Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(sign.getLocation());
            Set<String> memberUUIDStrs = lazySignPropertys.getMemberUUIDStrs();

            return (lazySignPropertys.getAccessType() == SignAccessType.AccessType.PUBLIC) ||
                SignPasswords.hasStillAccess(uuid, sign.getLocation()) ||
                (memberUUIDStrs != null && memberUUIDStrs.contains(uuid.toString()));
        } else {
            return (SignAccessType.getAccessType(sign, true) == SignAccessType.AccessType.PUBLIC) ||
                SignPasswords.hasStillAccess(uuid, sign.getLocation()) ||
                getUUIDs(sign, false, true).contains(uuid.toString());
        }
    }

    /**
     * get the list of all members / owners of a sign. might be empty.
     *
     * @param owners true if you want the set of owner uuids, false for members
     */
    public static @NotNull ListOrderedSet<String> getUUIDs(@NotNull final Sign sign, boolean owners, boolean ignoreCache) {
        ListOrderedSet<String> set;

        if (!ignoreCache && Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            LazySignProperties lazySignPropertys = Padlock.getPlugin().getLockCacheManager().getProtectedFromCache(sign.getLocation());
            set = owners ? lazySignPropertys.getOwnerUUIDStrs() : lazySignPropertys.getMemberUUIDStrs();
        } else {
            set = sign.getPersistentDataContainer().get(owners ? storedOwnersUUIDKey : storedMembersUUIDKey, uuidSetDataType);
        }

        if (set == null) {
            set = new ListOrderedSet<>();
        }

        return set;
    }

    /**
     * Add a player as owner (true) or member (false) of this lock sign.
     * <p>
     * If the player is null this just ensures the owner / member set was ever stored
     * in the getPersistentDataContainer, this is important
     * since we decide if a sign is legacy or not by checking if the sign has the owner list or not.
     *
     * @param sign    the sign to update
     * @param isOwner true if an owner should be added, false for a member
     * @param player  the offline player to add, might be null.
     */
    public static void addPlayer(@NotNull final Sign sign, boolean isOwner, @Nullable OfflinePlayer player) {
        if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
            Padlock.getPlugin().getLockCacheManager().removeFromCache(sign);
        }

        PersistentDataContainer container = sign.getPersistentDataContainer();

        ListOrderedSet<String> set = container.get(isOwner ? storedOwnersUUIDKey : storedMembersUUIDKey, uuidSetDataType);
        if (set == null) {
            set = new ListOrderedSet<>();
        }

        if (player != null) {
            set.add(player.getUniqueId().toString());
        }

        sign.getPersistentDataContainer().set(isOwner ? storedOwnersUUIDKey : storedMembersUUIDKey, uuidSetDataType, set);
        SignDisplay.updateDisplay(sign); // update the block to make the UUID change effective
    }

    /**
     * remove a player from the owner/member set
     *
     * @param sign    ths sign to update
     * @param isOwner if an owner (true) should get removed or a member (false)
     * @param uuid    the uuid of a player to remove
     * @return true if the player got removed, false otherwise
     */
    public static boolean removePlayer(@NotNull final Sign sign, boolean isOwner, UUID uuid) {
        PersistentDataContainer container = sign.getPersistentDataContainer();

        ListOrderedSet<String> set = container.get(isOwner ? storedOwnersUUIDKey : storedMembersUUIDKey, uuidSetDataType);
        if (set == null) {
            return false;
        }

        boolean success = set.remove(uuid.toString());
        if (success) {
            if (Padlock.getPlugin().getConfigManager().isCacheEnabled()) {
                Padlock.getPlugin().getLockCacheManager().removeFromCache(sign);
            }

            sign.getPersistentDataContainer().set(isOwner ? storedOwnersUUIDKey : storedMembersUUIDKey, uuidSetDataType, set);
            SignDisplay.updateDisplay(sign);// update the block to make the UUID change effective
        }

        return success;
    }

    /**
     * sets a new lock sign with the player as the owner
     * Does NOT apply physics!
     *
     * @param newsign   the block where the new sign should get created in (Note: does not check for any existing blocks at this place!)
     * @param blockface the direction the new sign should face
     * @param material  Material of the sign
     * @param player    new Owner
     */
    public static void putPrivateSignOn(@NotNull Block newsign, @NotNull BlockFace blockface, @NotNull Material material, @NotNull Player player) {
        // set type
        Material blockType = Material.matchMaterial(material.name().replace("_SIGN", "_WALL_SIGN"));
        if (blockType != null && Tag.WALL_SIGNS.isTagged(blockType)) {
            newsign.setType(blockType);
        } else {
            newsign.setType(Material.OAK_WALL_SIGN);
        }

        //set facing
        BlockData data = newsign.getBlockData();
        if (data instanceof Directional directional) {
            directional.setFacing(blockface);
            newsign.setBlockData(directional, false); // physics are NOT handled by this methode to make resets possible
        }

        // DANGER AHEAD! We don't use a snapshot here!
        Sign sign = (Sign) newsign.getState(false);

        // default color is hardly visible on dark signs
        if (sign.getType() == Material.DARK_OAK_WALL_SIGN) {
            sign.getSide(Side.FRONT).setColor(DyeColor.WHITE);
        }

        // set lock and it's owner
        SignAccessType.setAccessType(sign, SignAccessType.AccessType.PRIVATE, false);
        addPlayer(sign, true, player);

        // auto connect doors to stay in line with lockette
        Block attachedBlock = PadlockAPI.getAttachedBlock(newsign);
        if (attachedBlock != null && Openables.isUpDownDoor(attachedBlock)) {
            SignConnectedOpenable.setConnected(sign, true);
        }

        //set waxed and finally make all the changes happen by updating the state
        sign.setWaxed(true);
        sign.update();

        SignDisplay.updateDisplay(sign);
    }

    /**
     * Data type to store an ordered set of Strings.
     * The order is important to keep the names displayed by {@link SignDisplay} consistent,
     * and not display a new member / owner every time the sign was updated.
     * We will store the set as a string, as it's human understandable when read via /data get block.
     * Gson will translate the data into a json string and back into the set of string.
     * <p>
     * Why String and not strait up UUID you ask?
     * Most of the time, only comparison with another UUID is needed and it would be a waste of compute time to
     * translate the majority of UUID-Strings back into UUIDs.
     */
    private static class MemberUUUIDSetDataType implements PersistentDataType<String, ListOrderedSet<String>> {
        /**
         * Returns the primitive data type of this tag.
         *
         * @return the class of String
         */
        @NotNull
        @Override
        public Class<String> getPrimitiveType() {
            return String.class;
        }

        /**
         * Returns the complex object type the primitive value resembles.
         *
         * @return the ListOrderedSet class
         */
        @SuppressWarnings("unchecked")
        @Override
        public @NotNull Class<ListOrderedSet<String>> getComplexType() { //todo this cast
            return ((Class<ListOrderedSet<String>>) ((Class<?>) ListOrderedSet.class));
        }

        /**
         * Returns the primitive data that resembles the complex object passed to
         * this method.
         *
         * @param complex the complex object instance
         * @param context the context this operation is running in
         * @return the primitive String value
         */
        @Override
        public @NotNull String toPrimitive(@NotNull ListOrderedSet<String> complex, @NotNull PersistentDataAdapterContext context) {
            return gson.toJson(complex);
        }

        /**
         * Creates a set of strings based of the passed primitive json string value
         *
         * @param primitive the primitive value
         * @param context   the context this operation is running in
         * @return the complex object instance
         */
        @NotNull
        @Override
        public ListOrderedSet<String> fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) {
            return gson.fromJson(primitive, type);
        }
    }
}
