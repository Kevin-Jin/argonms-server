/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.common.tools.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <code>LockableMap</code> methods implemented from <code>Map</code> directly
 * call those methods in the provided <code>Map</code> (<code>HashMap</code> by
 * default). None of these inherited methods are altered to become thread- safe.
 * For this reason, the methods inherited from <code>Map</code> must only be
 * performed when the map is read or write locked, depending on the method's
 * behavior. This class is mainly provided to organize and prevent the pollution
 * of a name-space when it is required to provide a lock exclusively for a
 * single <code>Map</code> (i.e. a variable named map of a type that extends
 * <code>Map</code> and a variable named mapLock and of a type that extends
 * <code>ReadWriteLock</code> can be replaced with a single
 * <code>LockableMap</code>). In the scenario as previously described, the
 * method {@link #lockRead()} is equivalent to
 * <code>mapLock.readLock().lock()</code>, the method {@link #lockWrite()} is
 * equivalent to <code>mapLock.writeLock().lock()</code>, the method
 * {@link #unlockRead()} is equivalent to
 * <code>mapLock.readLock().unlock()</code>, and the method
 * {@link #unlockWrite()} is equivalent to
 * <code>mapLock.writeLock().unlock()</code>
 *
 * Convenience methods {@link #putWhenSafe(K, V)}, {@link #getWhenSafe(K)},
 * {@link #removeWhenSafe(K)}, and {@link #getSizeWhenSafe()} are provided in
 * the case that you only need to lock this map for one statement. If you find
 * that you exclusively use these methods and do not ever explicitly (un)lock
 * the <code>Map</code> through {@link #lockRead()}, {@link #lockWrite()},
 * {@link #unlockRead()}, or {@link #unlockWrite()} then you would probably be
 * better off using <code>ConcurrentHashMap</code>.
 *
 * <code>LockableMap</code> uses composition instead of inheritance when calling
 * <code>Map</code> operations so that any type of map, not only
 * <code>HashMap</code>, can be used, unlike <code>LockableHashMap</code>.
 *
 * @see argonms.tools.collections.LockableHashMap
 * @author GoldenKevin
 */
public class LockableMap<K, V> implements Map<K, V> {
	private Map<K, V> map;
	private Lock readLock, writeLock;

	/**
	 * Create a new instance of <code>LockableMap</code>. The underlying map
	 * field will be assigned to the provided instance of <code>Map</code>, and
	 * the underlying read lock field and write lock field will be assigned by
	 * calling readLock() and writeLock() respectively on the provided
	 * ReadWriteLock.
	 * @param map the <code>Map</code> that will be used for <code>Map</code>
	 * operations.
	 * @param rwLock the <code>ReadWriteLock</code> that will be used to lock
	 * the underlying <code>Map</code>.
	 */
	public LockableMap(Map<K, V> map, ReadWriteLock rwLock) {
		this.map = map;
		this.readLock = rwLock.readLock();
		this.writeLock = rwLock.writeLock();
	}

	/**
	 * Create a new instance of <code>LockableMap</code>. The underlying map
	 * field will be assigned to the provided instance of <code>Map</code>, and
	 * the underlying lock fields will be assigned from a new instance of
	 * <code>ReentrantReadWriteLock</code>.
	 * @param map the <code>Map</code> that will be used for <code>Map</code>
	 * operations.
	 */
	public LockableMap(Map<K, V> map) {
		this(map, new ReentrantReadWriteLock());
	}

	/**
	 * Create a new instance of <code>LockableMap</code>. The underlying map
	 * field will be assigned with a new instance of <code>HashMap</code>, and
	 * the underlying read lock field and write lock field will be assigned by
	 * calling readLock() and writeLock() respectively on the provided
	 * ReadWriteLock.
	 * @param rwLock the <code>ReadWriteLock</code> that will be used to lock
	 * the underlying <code>Map</code>.
	 */
	public LockableMap(ReadWriteLock rwLock) {
		this(new HashMap<K, V>(), rwLock);
	}

	/**
	 * Create a new instance of <code>LockableMap</code>. The underlying map
	 * field will be assigned with a new instance of <code>HashMap</code>, and
	 * the underlying lock fields will be assigned from a new instance of
	 * <code>ReentrantReadWriteLock</code>.
	 */
	public LockableMap() {
		this(new HashMap<K, V>(), new ReentrantReadWriteLock());
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return map.get(key);
	}

	@Override
	public V put(K key, V value) {
		return map.put(key, value);
	}

	@Override
	public V remove(Object key) {
		return map.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		map.putAll(m);
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	/**
	 * Acquires a lock from the underlying read lock. This will not block as
	 * long as there are no writer locks acquired. Be sure to return the lock
	 * after the critical section is executed, through {@link #unlockRead()}.
	 *
	 * If any values are returned in the critical section before calling
	 * {@link #unlockRead()} or if there is a potential for a portion of the
	 * critical section to throw an exception, surround the critical section
	 * with a try block and call  {@link #unlockRead()} in the finally block to
	 * make sure that the read lock is returned in any situation so that there
	 * is not a potential for deadlock.
	 */
	public void lockRead() {
		readLock.lock();
	}

	/**
	 * Releases a lock from the underlying read lock.
	 *
	 * If any values are returned in the critical section before calling
	 * {@link #unlockRead()} or if there is a potential for a portion of the
	 * critical section to throw an exception, surround the critical section
	 * with a try block and call  {@link #unlockRead()} in the finally block to
	 * make sure that the read lock is returned in any situation so that there
	 * is not a potential for deadlock.
	 */
	public void unlockRead() {
		readLock.unlock();
	}

	/**
	 * Acquires a lock from the underlying write lock. This will block if there
	 * are read locks acquired. Be sure to return the lock after the critical
	 * section is executed, through {@link #unlockWrite()}.
	 *
	 * If any values are returned in the critical section before calling
	 * {@link #unlockWrite()} or if there is a potential for a portion of the
	 * critical section to throw an exception, surround the critical section
	 * with a try block and call  {@link #unlockWrite()} in the finally block to
	 * make sure that the write lock is returned in any situation so that there
	 * is not a potential for deadlock.
	 */
	public void lockWrite() {
		writeLock.lock();
	}

	/**
	 * Releases a lock from the underlying write lock.
	 *
	 * If any values are returned in the critical section before calling
	 * {@link #unlockWrite()} or if there is a potential for a portion of the
	 * critical section to throw an exception, surround the critical section
	 * with a try block and call  {@link #unlockWrite()} in the finally block to
	 * make sure that the write lock is returned in any situation so that there
	 * is not a potential for deadlock.
	 */
	public void unlockWrite() {
		writeLock.unlock();
	}

	/**
	 * Acquires a write lock before calling <code>put(key, value)</code>, and
	 * releases the write lock immediately afterwards. Read {@link #lockWrite()}
	 * to find the situations that this method will block on.
	 *
	 * @param key key with which the specified value is to be associated.
	 * @param value value to be associated with the specified key.
	 */
	public void putWhenSafe(K key, V value) {
		lockWrite();
		try {
			put(key, value);
		} finally {
			unlockWrite();
		}
	}

	/**
	 * Acquires a write lock before calling <code>remove(key)</code>, and
	 * releases the write lock immediately afterwards. Read {@link #lockWrite()}
	 * to find the situations that this method will block on.
	 *
	 * @param key key whose mapping is to be removed from the map.
	 */
	public void removeWhenSafe(K key) {
		lockWrite();
		try {
			remove(key);
		} finally {
			unlockWrite();
		}
	}

	/**
	 * Acquires a read lock before calling <code>get(key)</code>, and releases
	 * the read lock immediately afterwards. Read {@link #lockRead()} to find
	 * the situations that this method will block on.
	 *
	 * @param key the key whose associated value is to be returned.
	 * @return the value to which this map maps the specified key, or
	 * <code>null</code> if the map contains no mapping for this key.
	 */
	public V getWhenSafe(K key) {
		lockRead();
		try {
			return get(key);
		} finally {
			unlockRead();
		}
	}

	/**
	 * Acquires a read lock before calling <code>size()</code>, and releases the
	 * read lock immediately afterwards. Read {@link #lockRead()} to find the
	 * situations that this method will block on.
	 *
	 * @return the number of key-value mappings in this map.
	 */
	public int getSizeWhenSafe() {
		lockRead();
		try {
			return size();
		} finally {
			unlockRead();
		}
	}
}
