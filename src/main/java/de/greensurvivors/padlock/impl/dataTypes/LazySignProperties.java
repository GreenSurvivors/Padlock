package de.greensurvivors.padlock.impl.dataTypes;

import de.greensurvivors.padlock.impl.signdata.SignAccessType;
import de.greensurvivors.padlock.impl.signdata.SignLock;
import de.greensurvivors.padlock.impl.signdata.SignTimer;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.Nullable;

public class LazySignProperties {
    private final @Nullable Sign lock;
    private final boolean isLock;

    private ListOrderedSet<String> ownerUUIDStrs;
    private ListOrderedSet<String> memberUUIDStrs;
    private Long timer;
    private SignAccessType.AccessType accessType;

    public LazySignProperties(@Nullable Sign lockSign) {
        this.lock = lockSign;

        isLock = lockSign != null && SignLock.isLockSign(lockSign);
    }

    public @Nullable Sign getLock() {
        return lock;
    }

    public boolean isLock() {
        return isLock;
    }

    public @Nullable ListOrderedSet<String> getOwnerUUIDStrs() {
        if (isLock && ownerUUIDStrs == null) {
            ownerUUIDStrs = SignLock.getUUIDs(lock, true, true);
        }

        return ownerUUIDStrs;
    }

    public @Nullable ListOrderedSet<String> getMemberUUIDStrs() {
        if (isLock && memberUUIDStrs == null) {
            memberUUIDStrs = SignLock.getUUIDs(lock, false, true);
        }

        return memberUUIDStrs;
    }

    public @Nullable Long getTimer() {
        if (isLock && timer == null) {
            timer = SignTimer.getTimer(lock, true);
        }

        return timer;
    }

    public @Nullable SignAccessType.AccessType getAccessType() {
        if (isLock && accessType == null) {
            accessType = SignAccessType.getAccessType(lock, true);
        }

        return accessType;
    }
}
