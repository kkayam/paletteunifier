package com.koraykaya.paletteunifier.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Counters for one reload, kept for the /paletteunifier stats command. */
public final class ReloadStats {
	public static final class Namespace {
		public final String ns;
		public final AtomicInteger scanned = new AtomicInteger();
		public final AtomicInteger matched = new AtomicInteger();
		public final AtomicInteger excluded = new AtomicInteger();
		public final AtomicInteger animated = new AtomicInteger();
		public final AtomicInteger recolored = new AtomicInteger();
		public final AtomicInteger cached = new AtomicInteger();
		public final AtomicInteger skippedNoPalette = new AtomicInteger();
		public final AtomicInteger errors = new AtomicInteger();
		public final List<String> errorSamples = new ArrayList<>();
		public volatile long millis;

		Namespace(String ns) {
			this.ns = ns;
		}

		public void error(Throwable t, String where) {
			errors.incrementAndGet();
			synchronized (errorSamples) {
				if (errorSamples.size() < 3) errorSamples.add(where + ": " + t);
			}
		}

		public String line() {
			return String.format("ns=%s scanned=%d matched=%d excluded=%d animated=%d recolored=%d cached=%d noPalette=%d errors=%d in %d ms",
					ns, scanned.get(), matched.get(), excluded.get(), animated.get(), recolored.get(), cached.get(),
					skippedNoPalette.get(), errors.get(), millis);
		}
	}

	public final Map<String, Namespace> namespaces = new ConcurrentHashMap<>();
	public final AtomicLong startedNanos = new AtomicLong(System.nanoTime());
	public volatile long totalMillis;

	public Namespace of(String ns) {
		return namespaces.computeIfAbsent(ns, Namespace::new);
	}

	public int total(java.util.function.ToIntFunction<Namespace> f) {
		int s = 0;
		for (Namespace n : namespaces.values()) s += f.applyAsInt(n);
		return s;
	}

	public List<String> lines() {
		List<String> out = new ArrayList<>();
		List<Namespace> list = new ArrayList<>(namespaces.values());
		list.sort((a, b) -> Integer.compare(b.recolored.get() + b.cached.get(), a.recolored.get() + a.cached.get()));
		for (Namespace n : list) {
			if (n.matched.get() == 0 && n.errors.get() == 0) continue;
			out.add(n.line());
		}
		out.add(String.format("total: recolored=%d cached=%d matched=%d errors=%d in %d ms",
				total(n -> n.recolored.get()), total(n -> n.cached.get()), total(n -> n.matched.get()),
				total(n -> n.errors.get()), totalMillis));
		return out;
	}
}
