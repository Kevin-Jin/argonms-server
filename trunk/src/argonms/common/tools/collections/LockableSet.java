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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public class LockableSet<T> implements Set<T> {
	private Set<T> set;
	private Lock readLock, writeLock;

	public LockableSet(Set<T> set, ReadWriteLock rwLock) {
		this.set = set;
		this.readLock = rwLock.readLock();
		this.writeLock = rwLock.writeLock();
	}

	public LockableSet(Set<T> set) {
		this(set, new ReentrantReadWriteLock());
	}

	public LockableSet(ReadWriteLock rwLock) {
		this(new HashSet<T>(), rwLock);
	}

	public LockableSet() {
		this(new HashSet<T>(), new ReentrantReadWriteLock());
	}

	public int size() {
		return set.size();
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

	//blargh, stupid wording on Set doesn't allow us to generic keys...
	@SuppressWarnings("unchecked")
	public boolean contains(Object o) {
		return set.contains((T) o);
	}

	public Iterator<T> iterator() {
		return set.iterator();
	}

	public Object[] toArray() {
		return set.toArray();
	}

	public <E> E[] toArray(E[] a) {
		return set.toArray(a);
	}

	public boolean add(T e) {
		return set.add(e);
	}

	//blargh, stupid wording on Set doesn't allow us to generic keys...
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		return set.remove((T) o);
	}

	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	public boolean addAll(Collection<? extends T> c) {
		return set.addAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}

	public boolean removeAll(Collection<?> c) {
		return set.removeAll(c);
	}

	public void clear() {
		set.clear();
	}

	public void lockRead() {
		readLock.lock();
	}

	public void unlockRead() {
		readLock.unlock();
	}

	public void lockWrite() {
		writeLock.lock();
	}

	public void unlockWrite() {
		writeLock.unlock();
	}

	public void addWhenSafe(T e) {
		lockWrite();
		try {
			add(e);
		} finally {
			unlockWrite();
		}
	}

	public void removeWhenSafe(T e) {
		lockWrite();
		try {
			remove(e);
		} finally {
			unlockWrite();
		}
	}

	public int getSizeWhenSafe() {
		lockRead();
		try {
			return size();
		} finally {
			unlockRead();
		}
	}
}
