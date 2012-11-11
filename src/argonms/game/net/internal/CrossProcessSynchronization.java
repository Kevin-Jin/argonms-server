/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package argonms.game.net.internal;

import argonms.common.util.collections.Pair;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Kevin
 */
public abstract class CrossProcessSynchronization {
	protected static class WeakValueMap<K, V> {
		private final Map<K, WeakValue<K, V>> backingMap;
		private final ReferenceQueue<V> queue;

		public WeakValueMap(Map<K, WeakValue<K, V>> backingMap) {
			this.backingMap = backingMap;
			queue = new ReferenceQueue<V>();
		}

		@SuppressWarnings("unchecked")
		private void processQueue() {
			WeakValue<K, V> wv;
			while ((wv = (WeakValue<K, V>) queue.poll()) != null)
				backingMap.remove(wv.key);
		}

		private V getReferenceObject(WeakReference<V> ref) {
			return ref == null ? null : ref.get();
		}

		public V get(K key) {
			return getReferenceObject(backingMap.get(key));
		}

		public V put(K key, V value) {
			processQueue();
			WeakValue<K, V> oldValue = backingMap.put(key, WeakValue.<K, V>create(key, value, queue));
			return getReferenceObject(oldValue);
		}

		public V remove(K key) {
			return getReferenceObject(backingMap.remove(key));
		}

		private static class WeakValue<K, V> extends WeakReference<V> {
			private K key;

			private WeakValue(K key, V value, ReferenceQueue<V> queue) {
				super(value, queue);
				this.key = key;
			}

			private static <K, V> WeakValue<K, V> create(K key, V value, ReferenceQueue<V> queue) {
				return (value == null ? null : new WeakValue<K, V>(key, value, queue));
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (!(obj instanceof WeakValue))
					return false;
				Object ref1 = this.get();
				Object ref2 = ((WeakValue) obj).get();
				if (ref1 == ref2)
					return true;
				if ((ref1 == null) || (ref2 == null))
					return false;
				return ref1.equals(ref2);
			}

			@Override
			public int hashCode() {
				Object ref = this.get();
				return (ref == null) ? 0 : ref.hashCode();
			}
		}
	}

	protected final WeakValueMap<Integer, BlockingQueue<Pair<Byte, Object>>> blockingCalls;
	protected final AtomicInteger nextResponseId;

	protected CrossProcessSynchronization() {
		//prevents memory leaks in case responses time out and never reach us
		this.blockingCalls = new WeakValueMap<Integer, BlockingQueue<Pair<Byte, Object>>>
				(new ConcurrentHashMap<Integer, WeakValueMap.WeakValue<Integer, BlockingQueue<Pair<Byte, Object>>>>());
		this.nextResponseId = new AtomicInteger(0);
	}
}
