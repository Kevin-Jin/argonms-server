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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public class LockableList<T> implements List<T> {
	private List<T> list;
	private Lock readLock, writeLock;

	public LockableList(List<T> list, ReadWriteLock rwLock) {
		this.list = list;
		this.readLock = rwLock.readLock();
		this.writeLock = rwLock.writeLock();
	}

	public LockableList(List<T> list) {
		this(list, new ReentrantReadWriteLock());
	}

	public LockableList(ReadWriteLock rwLock) {
		this(new ArrayList<T>(), rwLock);
	}

	public LockableList() {
		this(new ArrayList<T>(), new ReentrantReadWriteLock());
	}

	public int size() {
		return list.size();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	//blargh, stupid wording on List doesn't allow us to generic type...
	@SuppressWarnings("unchecked")
	public boolean contains(Object o) {
		return list.contains((T) o);
	}

	public Iterator<T> iterator() {
		return list.iterator();
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <E> E[] toArray(E[] a) {
		return list.toArray(a);
	}

	public boolean add(T e) {
		return list.add(e);
	}

	//blargh, stupid wording on List doesn't allow us to generic type...
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		return list.remove((T) o);
	}

	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	public boolean addAll(Collection<? extends T> c) {
		return list.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends T> c) {
		return list.addAll(index, c);
	}

	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	public void clear() {
		list.clear();
	}

	public T get(int index) {
		return list.get(index);
	}

	public T set(int index, T element) {
		return list.set(index, element);
	}

	public void add(int index, T element) {
		list.add(index, element);
	}

	public T remove(int index) {
		return list.remove(index);
	}

	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	public ListIterator<T> listIterator() {
		return list.listIterator();
	}

	public ListIterator<T> listIterator(int index) {
		return list.listIterator(index);
	}

	public List<T> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
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
