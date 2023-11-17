package de.greensurvivors.padlock.impl.dataTypes;

import com.github.benmanes.caffeine.cache.Cache;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CacheSet<E> extends AbstractSet<E> implements Set<E> {

    private final Cache<E, Object> cache;

    // Dummy value to associate with an Object in the backing Cache
    private static final Object PRESENT = new Object();

    public CacheSet(Cache<E, Object> newCache) {
        this.cache = newCache;
    }

    public CacheSet(Cache<E, Object> newCache, Collection<? extends E> c) {
        this.cache = newCache;
        addAll(c);
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        return cache.asMap().keySet().iterator();
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     * More formally, returns {@code true} if and only if this set
     * contains an element {@code e} such that
     * {@code Objects.equals(o, e)}.
     *
     * @param o element whose presence in this set is to be tested
     * @return {@code true} if this set contains the specified element
     */
    @Override
    public boolean contains(Object o) {
        cache.cleanUp();
        return cache.asMap().containsKey(o);
    }

    /**
     * Adds the specified element to this set if it is not already present.
     * More formally, adds the specified element {@code e} to this set if
     * this set contains no element {@code e2} such that
     * {@code Objects.equals(e, e2)}.
     * If this set already contains the element, the call leaves the set
     * unchanged and returns {@code false}.
     *
     * @param e element to be added to this set
     * @return {@code true} if this set did not already contain the specified
     * element
     */
    @Override
    public boolean add(E e) {
        cache.cleanUp();
        if (cache.getIfPresent(e) == null) {
            cache.put(e, PRESENT);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes the specified element from this set if it is present.
     * More formally, removes an element {@code e} such that
     * {@code Objects.equals(o, e)},
     * if this set contains such an element.  Returns {@code true} if
     * this set contained the element (or equivalently, if this set
     * changed as a result of the call).  (This set will not contain the
     * element once the call returns.)
     *
     * @param o object to be removed from this set, if present
     * @return {@code true} if the set contained the specified element
     */
    @Override
    public boolean remove(Object o) {
        return cache.asMap().remove(o) == PRESENT;
    }

    /**
     * Removes all of the elements from this set.
     * The set will be empty after this call returns.
     */
    public void clear() {
        cache.invalidateAll();
        cache.cleanUp();
        cache.asMap().clear();
    }

    @Override
    public int size() {
        cache.cleanUp();
        return (int) cache.estimatedSize();
    }
}
