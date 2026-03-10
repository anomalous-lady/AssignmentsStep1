import java.util.*;
import java.util.concurrent.*;

public class Problem3_DNSCache {

    // ✅ Custom DNS Entry class
    static class DNSEntry {
        String domain;
        String ipAddress;
        long timestamp;       // when it was cached
        long expiryTime;      // when it expires (in ms)
        long lastAccessed;    // for LRU eviction

        public DNSEntry(String domain, String ipAddress, int ttlSeconds) {
            this.domain      = domain;
            this.ipAddress   = ipAddress;
            this.timestamp   = System.currentTimeMillis();
            this.expiryTime  = this.timestamp + (ttlSeconds * 1000L);
            this.lastAccessed = this.timestamp;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }

        public long getRemainingTTL() {
            long remaining = (expiryTime - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        }
    }

    // ✅ Cache storage: domain -> DNSEntry
    private LinkedHashMap<String, DNSEntry> cache;
    private final int MAX_CACHE_SIZE;

    // ✅ Stats tracking
    private int cacheHits   = 0;
    private int cacheMisses = 0;
    private int cacheExpired = 0;
    private long totalLookupTime = 0;
    private int totalLookups     = 0;

    // ✅ Simulated upstream DNS database
    private HashMap<String, String> upstreamDNS = new HashMap<>();

    // Constructor
    public Problem3_DNSCache(int maxSize) {
        this.MAX_CACHE_SIZE = maxSize;

        // LRU eviction using LinkedHashMap (accessOrder = true)
        this.cache = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };

        // Populate upstream DNS
        upstreamDNS.put("google.com",     "172.217.14.206");
        upstreamDNS.put("facebook.com",   "157.240.241.35");
        upstreamDNS.put("youtube.com",    "142.250.80.46");
        upstreamDNS.put("twitter.com",    "104.244.42.65");
        upstreamDNS.put("amazon.com",     "205.251.242.103");
        upstreamDNS.put("github.com",     "140.82.121.4");
        upstreamDNS.put("netflix.com",    "52.6.19.11");
        upstreamDNS.put("instagram.com",  "157.240.241.174");

        // ✅ Background thread to clean expired entries every 5 seconds
        startCleanupThread();
    }

    // ✅ Resolve domain (main method)
    public String resolve(String domain) {
        long startTime = System.nanoTime();
        totalLookups++;
        String result;

        // Check if in cache
        if (cache.containsKey(domain)) {
            DNSEntry entry = cache.get(domain);

            // Check if expired
            if (entry.isExpired()) {
                cache.remove(domain);
                cacheExpired++;
                String ip = queryUpstream(domain);
                result = "🔄 Cache EXPIRED → Queried upstream → "
                        + ip + " (TTL: 300s)";
            } else {
                // Cache HIT ✅
                entry.lastAccessed = System.currentTimeMillis();
                cacheHits++;
                result = "✅ Cache HIT  → " + entry.ipAddress
                        + " (TTL remaining: " + entry.getRemainingTTL() + "s)";
            }
        } else {
            // Cache MISS
            cacheMisses++;
            String ip = queryUpstream(domain);
            if (ip != null) {
                result = "❌ Cache MISS → Queried upstream → "
                        + ip + " (TTL: 300s)";
            } else {
                result = "🚫 Domain not found: " + domain;
            }
        }

        long endTime    = System.nanoTime();
        double timingMs = (endTime - startTime) / 1_000_000.0;
        totalLookupTime += (endTime - startTime);

        return String.format("%-60s [%.3f ms]", result, timingMs);
    }

    // ✅ Query upstream DNS (simulated 100ms delay)
    private String queryUpstream(String domain) {
        try {
            Thread.sleep(100); // simulate real DNS lookup delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String ip = upstreamDNS.get(domain);
        if (ip != null) {
            cache.put(domain, new DNSEntry(domain, ip, 300)); // cache with 300s TTL
        }
        return ip;
    }

    // ✅ Manually add entry with custom TTL
    public void addEntry(String domain, String ip, int ttlSeconds) {
        cache.put(domain, new DNSEntry(domain, ip, ttlSeconds));
        System.out.println("📌 Manually added: " + domain
                + " → " + ip + " (TTL: " + ttlSeconds + "s)");
    }

    // ✅ Force expire an entry (for testing)
    public void forceExpire(String domain) {
        if (cache.containsKey(domain)) {
            DNSEntry entry = cache.get(domain);
            entry.expiryTime = System.currentTimeMillis() - 1000; // set to past
            System.out.println("⏰ Force expired: " + domain);
        }
    }

    // ✅ Check cache stats
    public void getCacheStats() {
        double hitRate      = totalLookups == 0 ? 0
                : (cacheHits * 100.0 / totalLookups);
        double avgLookupMs  = totalLookups == 0 ? 0
                : (totalLookupTime / 1_000_000.0 / totalLookups);

        System.out.println("\n📊 Cache Statistics:");
        System.out.println("─".repeat(45));
        System.out.printf("  Total Lookups   : %d%n",   totalLookups);
        System.out.printf("  Cache Hits      : %d%n",   cacheHits);
        System.out.printf("  Cache Misses    : %d%n",   cacheMisses);
        System.out.printf("  Expired Entries : %d%n",   cacheExpired);
        System.out.printf("  Hit Rate        : %.1f%%%n", hitRate);
        System.out.printf("  Avg Lookup Time : %.3f ms%n", avgLookupMs);
        System.out.printf("  Cache Size      : %d / %d%n",
                cache.size(), MAX_CACHE_SIZE);
        System.out.println("─".repeat(45));
    }

    // ✅ Show current cache contents
    public void showCache() {
        System.out.println("\n🗂️  Current Cache Contents:");
        System.out.println("─".repeat(60));
        if (cache.isEmpty()) {
            System.out.println("  Cache is empty!");
        } else {
            cache.forEach((domain, entry) -> {
                String status = entry.isExpired() ? "❌ EXPIRED" : "✅ VALID";
                System.out.printf("  %-20s → %-18s TTL: %4ds  %s%n",
                        domain, entry.ipAddress, entry.getRemainingTTL(), status);
            });
        }
        System.out.println("─".repeat(60));
    }

    // ✅ Background cleanup thread
    private void startCleanupThread() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            int removed = 0;
            Iterator<Map.Entry<String, DNSEntry>> it =
                    cache.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue().isExpired()) {
                    it.remove();
                    removed++;
                }
            }
            if (removed > 0) {
                System.out.println("\n🧹 Cleanup: removed "
                        + removed + " expired entries.");
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    // ✅ Main method
    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== DNS Cache with TTL ===\n");

        Problem3_DNSCache dns = new Problem3_DNSCache(5); // max 5 entries (LRU test)

        // First lookups → Cache MISS
        System.out.println("🔍 Initial Lookups:");
        System.out.println(dns.resolve("google.com"));
        System.out.println(dns.resolve("facebook.com"));
        System.out.println(dns.resolve("youtube.com"));

        // Repeat lookups → Cache HIT
        System.out.println("\n🔍 Repeat Lookups (should be cache hits):");
        System.out.println(dns.resolve("google.com"));
        System.out.println(dns.resolve("facebook.com"));

        // Show cache
        dns.showCache();

        // Test TTL expiry
        System.out.println("\n⏰ Testing TTL Expiry:");
        dns.addEntry("test.com", "192.168.1.1", 2); // 2 second TTL
        System.out.println(dns.resolve("test.com")); // HIT
        System.out.println("⏳ Waiting 3 seconds for TTL to expire...");
        Thread.sleep(3000);
        System.out.println(dns.resolve("test.com")); // EXPIRED

        // Test LRU eviction (cache max = 5)
        System.out.println("\n🔄 Testing LRU Eviction (max cache = 5):");
        System.out.println(dns.resolve("twitter.com"));
        System.out.println(dns.resolve("amazon.com"));
        System.out.println(dns.resolve("github.com"));  // should evict oldest
        dns.showCache();

        // Final stats
        dns.getCacheStats();
    }
}