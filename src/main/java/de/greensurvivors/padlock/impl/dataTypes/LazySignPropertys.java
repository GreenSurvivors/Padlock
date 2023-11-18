package de.greensurvivors.padlock.impl.dataTypes;

import de.greensurvivors.padlock.impl.MiscUtils;
import de.greensurvivors.padlock.impl.signdata.SignAccessType;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import de.greensurvivors.padlock.impl.signdata.SignTimer;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class LazySignPropertys {
    private final @Nullable Sign lock;
    private final boolean isLock;

    private Set<String> ownerUUIDStrs;
    private Set<OfflinePlayer> owners;
    private Set<String> memberUUIDStrs;
    private Set<OfflinePlayer> members;
    private Long timer;
    private SignAccessType.AccessType accessType;


    public LazySignPropertys(@Nullable Sign lockSign) {
        this.lock = lockSign;

        isLock = lockSign != null && SignLock.isLockSign(lockSign);
    }

    public @Nullable Sign getLock() {
        return lock;
    }

    public boolean isLock() {
        return isLock;
    }

    public @Nullable Set<String> getOwnerUUIDStrs() {
        if (isLock && ownerUUIDStrs == null) {
            ownerUUIDStrs = SignLock.getUUIDs(lock, true);
        }

        return ownerUUIDStrs;
    }

    public @Nullable Set<OfflinePlayer> getOwners() {
        if (isLock && owners == null) {
            if (ownerUUIDStrs == null) {
                ownerUUIDStrs = SignLock.getUUIDs(lock, true);
            }

            owners = MiscUtils.getPlayersFromUUIDStrings(ownerUUIDStrs);
        }

        return owners;
    }

    public @Nullable Set<String> getMemberUUIDStrs() {
        if (isLock && memberUUIDStrs == null) {
            memberUUIDStrs = SignLock.getUUIDs(lock, false);
        }

        return ownerUUIDStrs;
    }

    public @Nullable Set<OfflinePlayer> getMembers() {
        if (isLock && members == null) {
            if (memberUUIDStrs == null) {
                memberUUIDStrs = SignLock.getUUIDs(lock, false);
            }

            members = MiscUtils.getPlayersFromUUIDStrings(ownerUUIDStrs);
        }

        return members;
    }

    public @Nullable Long getTimer() {
        if (isLock && timer == null) {
            timer = SignTimer.getTimer(lock);
        }

        return timer;
    }

    public @Nullable SignAccessType.AccessType getAccessType() {
        if (isLock && accessType == null) {
            accessType = SignAccessType.getAccessType(lock);
        }

        return accessType;
    }
}
