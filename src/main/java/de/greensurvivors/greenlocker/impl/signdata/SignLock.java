package de.greensurvivors.greenlocker.impl.signdata;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.greensurvivors.greenlocker.GreenLocker;
import de.greensurvivors.greenlocker.config.MessageManager;
import net.kyori.adventure.text.Component;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.logging.Level;

//todo this is not timer/everyone compatible!
public class SignLock {
    private final static NamespacedKey storedMembersUUIDKey = new NamespacedKey(GreenLocker.getPlugin(), "MemberUUIDs");
    private final static NamespacedKey storedOwnersUUIDKey = new NamespacedKey(GreenLocker.getPlugin(), "OwnerUUIDs");
    private static final MemberUUUIDSetDataType uuidSetData = new MemberUUUIDSetDataType();
    private static final Gson gson = new Gson();
    private static final Type type = new TypeToken<ListOrderedSet<String>>() {
    }.getType();

    public static boolean isPlayerOnLine(@NotNull Player player, @NotNull Sign sign, boolean isOwner) {
        ListOrderedSet<String> uuidSet = getUUIDs(sign, isOwner);
        String uuidStringPlayer = player.getUniqueId().toString();

        if (!uuidSet.isEmpty()) {
            for (String uuidStringSign : uuidSet) {
                if (uuidStringPlayer.equals(uuidStringSign)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void setInvalid(@NotNull Sign sign) {
        sign.getSide(Side.FRONT).line(0, GreenLocker.getPlugin().getMessageManager().getLang(MessageManager.LangPath.INVALID_SIGN));
        sign.update();
    }

    public static boolean isLockSign(@NotNull Sign sign) {
        return GreenLocker.getPlugin().getMessageManager().isSignComp(sign.getSide(Side.FRONT).line(0), MessageManager.LangPath.PRIVATE_SIGN);
    }

    @Deprecated(forRemoval = true)
    public static boolean isAdditionalSign(@NotNull Sign sign) {
        return GreenLocker.getPlugin().getMessageManager().isSignComp(sign.getSide(Side.FRONT).line(0), MessageManager.LangPath.ADDITIONAL_SIGN);
    }

    public static boolean isOwner(@NotNull final Sign sign, UUID uuid) {
        return getUUIDs(sign, true).contains(uuid.toString());
    }

    public static boolean isMember(@NotNull final Sign sign, UUID uuid) {
        Boolean everyOneAccess = EveryoneSign.getAccessEveryone(sign);

        return (everyOneAccess != null && everyOneAccess) || getUUIDs(sign, false).contains(uuid.toString());
    }

    public static @NotNull ListOrderedSet<String> getUUIDs(@NotNull final Sign sign, boolean owners) {
        PersistentDataContainer container = sign.getPersistentDataContainer();

        ListOrderedSet<String> set = container.get(owners ? storedOwnersUUIDKey : storedMembersUUIDKey, uuidSetData);
        if (set == null) {
            set = new ListOrderedSet<>();
        }

        return set;
    }

    @Deprecated(forRemoval = true)
    public static void updateSignFromAdditional(@NotNull Sign main, @NotNull Sign additional) {
        for (int i = 1; i <= 3; i++) {
            String line = additional.getSide(Side.FRONT).getLine(i);

            if (line.contains("#")) {
                String[] splitted = line.split("#", 2);

                //todo this might be as well a #created
                if (splitted[1].length() == 36) { // uuid valid check
                    addPlayer(main, false, Bukkit.getOfflinePlayer(UUID.fromString(splitted[1])));
                }
            }
        }

        setInvalid(additional);
    }

    @Deprecated(forRemoval = true)
    public static void updateLegacyUUIDs(@NotNull Sign sign) {
        PersistentDataContainer container = sign.getPersistentDataContainer();

        ListOrderedSet<String> owners = container.get(storedOwnersUUIDKey, uuidSetData);
        if (owners == null) {
            owners = new ListOrderedSet<>();
        }
        ListOrderedSet<String> members = container.get(storedMembersUUIDKey, uuidSetData);
        if (members == null) {
            members = new ListOrderedSet<>();
        }

        for (int i = 1; i <= 3; i++) {
            String line = sign.getSide(Side.FRONT).getLine(i);

            if (line.contains("#")) {
                String[] splitted = line.split("#", 2);

                if (splitted[1].length() == 36) { // uuid valid check
                    if (i == 1) {
                        owners.add(splitted[1]);
                    } else {
                        members.add(splitted[1]);
                    }

                    sign.getSide(Side.FRONT).setLine(i, splitted[0]);
                }
            }
        }

        sign.getPersistentDataContainer().set(storedOwnersUUIDKey, uuidSetData, owners);
        sign.getPersistentDataContainer().set(storedMembersUUIDKey, uuidSetData, members);
        sign.update();
    }

    @Deprecated(forRemoval = true)
    public static boolean isLegacySign(@NotNull Sign sign) {
        return !sign.getPersistentDataContainer().has(storedOwnersUUIDKey, uuidSetData);
    }

    public static void updateNamesFromUUIDs(@NotNull Sign sign) {
        PersistentDataContainer container = sign.getPersistentDataContainer();

        ListOrderedSet<String> owners = container.get(storedOwnersUUIDKey, uuidSetData);
        ListOrderedSet<String> members = container.get(storedMembersUUIDKey, uuidSetData);

        if (owners != null && !owners.isEmpty()) {
            try {
                UUID uuid = UUID.fromString(owners.get(0));
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

                if (player.getName() != null) {
                    sign.getSide(Side.FRONT).line(1, Component.text(player.getName()));
                } else {
                    GreenLocker.getPlugin().getLogger().log(Level.WARNING, "broken UUID \"" + owners.get(0) + "\" (no Owner) in Lock-Sign at " + sign.getLocation());
                }
            } catch (IllegalArgumentException e) {
                GreenLocker.getPlugin().getLogger().log(Level.WARNING, "broken UUID \"" + owners.get(0) + "\" (invalid) in Lock-Sign at " + sign.getLocation(), e);
            }
        } else {
            GreenLocker.getPlugin().getLogger().log(Level.WARNING, "Lock sign without Owners at " + sign.getLocation());
        }

        if (members != null && !members.isEmpty()) {
            int size = Math.min(members.size(), 3);

            for (int i = 0; i < size; i++) {
                try {
                    UUID uuid = UUID.fromString(members.get(i));
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

                    if (player.getName() != null) {
                        sign.getSide(Side.FRONT).line(1, Component.text(player.getName()));
                    } else {
                        GreenLocker.getPlugin().getLogger().log(Level.WARNING, "broken UUID \"" + members.get(i) + "\" (no Owner) in Lock-Sign at " + sign.getLocation());
                    }
                } catch (IllegalArgumentException e) {
                    GreenLocker.getPlugin().getLogger().log(Level.WARNING, "broken UUID \"" + members.get(i) + "\" (invalid) in Lock-Sign at " + sign.getLocation(), e);
                }
            } //for loop
        } //empty members

        sign.update();
    }

    public static void addPlayer(@NotNull final Sign sign, boolean isOwner, @NotNull OfflinePlayer player) {
        PersistentDataContainer container = sign.getPersistentDataContainer();

        ListOrderedSet<String> set = container.get(isOwner ? storedOwnersUUIDKey : storedMembersUUIDKey, uuidSetData);
        if (set == null) {
            set = new ListOrderedSet<>();
        }

        set.add(player.getUniqueId().toString());

        sign.getPersistentDataContainer().set(isOwner ? storedOwnersUUIDKey : storedMembersUUIDKey, uuidSetData, set);
        updateNamesFromUUIDs(sign);// update the block to make the UUID change effective
    }

    public static boolean removePlayer(@NotNull final Sign sign, boolean isOwner, UUID uuid) {
        PersistentDataContainer container = sign.getPersistentDataContainer();

        ListOrderedSet<String> set = container.get(isOwner ? storedOwnersUUIDKey : storedMembersUUIDKey, uuidSetData);
        if (set == null) {
            return false;
        }

        boolean success = set.remove(uuid.toString());
        if (success) {
            sign.getPersistentDataContainer().set(isOwner ? storedOwnersUUIDKey : storedMembersUUIDKey, uuidSetData, set);
            updateNamesFromUUIDs(sign);// update the block to make the UUID change effective
        }

        return success;
    }

    public static void updateNamesByUuid(@NotNull Sign sign) {
        ListOrderedSet<String> owners = sign.getPersistentDataContainer().get(storedOwnersUUIDKey, uuidSetData);
        ListOrderedSet<String> members = sign.getPersistentDataContainer().get(storedMembersUUIDKey, uuidSetData);

        if (owners != null) {
            String firstOwner = owners.get(0);

            if (firstOwner != null) {
                Player player = Bukkit.getPlayer(UUID.fromString(firstOwner));

                if (player != null) {
                    sign.getSide(Side.FRONT).line(1, Component.text(player.getName()));
                }
            }
        }

        if (members != null) {
            int size = Math.min(3, members.size());

            for (int i = 0; i < size; i++) {
                String member = members.get(0);

                if (member != null) {
                    Player player = Bukkit.getPlayer(UUID.fromString(member));

                    if (player != null) {
                        sign.getSide(Side.FRONT).line(i + 1, Component.text(player.getName()));
                    }
                }
            }
        }

        sign.update();
    }

    private static class MemberUUUIDSetDataType implements PersistentDataType<String, ListOrderedSet<String>> {
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
        public Class<ListOrderedSet<String>> getComplexType() { //todo this cast
            return ((Class<ListOrderedSet<String>>) ((Class<?>) ListOrderedSet.class));
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
        public @NotNull String toPrimitive(@NotNull ListOrderedSet<String> complex, @NotNull PersistentDataAdapterContext context) {
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
        public ListOrderedSet<String> fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) {
            return gson.fromJson(primitive, type);
        }
    }
}
