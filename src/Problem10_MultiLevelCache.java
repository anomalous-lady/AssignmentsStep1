import java.util.*;
import java.util.concurrent.atomic.*;

public class Problem10_MultiLevelCache {

    // ✅ Video data class
    static class VideoData {
        String videoId;
        String title;
        byte[] data; // simulated video content
        long   sizeKB;
        int    accessCount;

        public VideoData(String videoId, String title, long sizeKB) {
            this.videoId     = videoId;
            this.title       = title;
            this.sizeKB      = sizeKB;
            this.accessCount = 0;
        }
    }

    // ✅ Cache result
    static class CacheResult {
        String    videoId;
        VideoData data;
        String    source;    // L1, L2, L3
        double    timingMs;
        boolean   hit;

        public CacheResult(String videoId, VideoData data,
                           String source, double ms, boolean hit) {
            this.videoId  = videoId;
            this.data     = data;
            this.source   = source;
            this.timingMs = ms;
            this.hit      = hit;
        }
    }

    // ✅ L1: In-memory LRU (LinkedHashMap, max 10 entries for demo)
    private final int L1_MAX = 10;
    private LinkedHashMap<String, VideoData> l1Cache;

    // ✅ L2: SSD-backed (simulated HashMap, max 30 entries)
    private final int L2_MAX = 30;
    private HashMap<String, VideoData> l2Cache = new HashMap<>();

    // ✅ L3: Database (simulated, unlimited)
    private HashMap<String, VideoData> database = new HashMap<>();

    // ✅ Access counts for promotion decisions
    private HashMap<String, AtomicInteger> accessCounts = new HashMap<>();
    private final int PROMOTION_THRESHOLD = 3;

    // ✅ Stats
    private AtomicLong l1Hits   = new AtomicLong(0);
    private AtomicLong l2Hits   = new AtomicLong(0);
    private AtomicLong l3Hits   = new AtomicLong(0);
    private AtomicLong misses   = new AtomicLong(0);
    private AtomicLong l1Time   = new AtomicLong(0);
    private AtomicLong l2Time   = new AtomicLong(0);
    private AtomicLong l3Time   = new AtomicLong(0);
    private AtomicLong totalReq = new AtomicLong(0);

    public Problem10_MultiLevelCache() {
        // L1 with LRU eviction
        l1Cache = new LinkedHashMap<>(L1_MAX, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, VideoData> e) {
                if (size() > L1_MAX) {
                    // Demote to L2 before evicting
                    if (l2Cache.size() < L2_MAX)
                        l2Cache.put(e.getKey(), e.getValue());
                    return true;
                }
                return false;
            }
        };
        seedDatabase();
    }

    // ✅ Get video — checks L1 → L2 → L3
    public CacheResult getVideo(String videoId) {
        totalReq.incrementAndGet();
        long start = System.nanoTime();

        // Track access count
        accessCounts.computeIfAbsent(videoId, k -> new AtomicInteger(0))
                .incrementAndGet();

        // --- L1 Check ---
        if (l1Cache.containsKey(videoId)) {
            long   end  = System.nanoTime();
            double ms   = simulateLatency("L1", end - start);
            l1Hits.incrementAndGet();
            l1Time.addAndGet((long) ms);
            VideoData data = l1Cache.get(videoId);
            data.accessCount++;
            return new CacheResult(videoId, data, "L1", ms, true);
        }

        double l1Ms = simulateLatency("L1_MISS", 0);

        // --- L2 Check ---
        if (l2Cache.containsKey(videoId)) {
            long   end = System.nanoTime();
            double ms  = simulateLatency("L2", end - start);
            l2Hits.incrementAndGet();
            l2Time.addAndGet((long) ms);
            VideoData data = l2Cache.get(videoId);
            data.accessCount++;

            // Promote to L1 if access count exceeds threshold
            int count = accessCounts.get(videoId).get();
            if (count >= PROMOTION_THRESHOLD) {
                l1Cache.put(videoId, data);
                l2Cache.remove(videoId);
                return new CacheResult(videoId, data,
                        "L2 → promoted to L1", ms, true);
            }
            return new CacheResult(videoId, data, "L2", ms, true);
        }

        double l2Ms = simulateLatency("L2_MISS", 0);

        // --- L3 Check (Database) ---
        if (database.containsKey(videoId)) {
            long   end = System.nanoTime();
            double ms  = simulateLatency("L3", end - start);
            l3Hits.incrementAndGet();
            l3Time.addAndGet((long) ms);
            VideoData data = database.get(videoId);
            data.accessCount++;

            // Add to L2
            if (l2Cache.size() < L2_MAX) {
                l2Cache.put(videoId, data);
            }
            return new CacheResult(videoId, data,
                    "L3 Database → added to L2", ms, true);
        }

        misses.incrementAndGet();
        return new CacheResult(videoId, null, "NOT FOUND", 0, false);
    }

    // ✅ Simulate realistic latency per cache level
    private double simulateLatency(String level, long actualNs) {
        try {
            switch (level) {
                case "L1":      Thread.sleep(0, 500_000); return 0.5;
                case "L2":      Thread.sleep(5);          return 5.0;
                case "L3":      Thread.sleep(150);        return 150.0;
                default:        return 0.1;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return 0;
    }

    // ✅ Invalidate cache entry (content update)
    public void invalidate(String videoId) {
        l1Cache.remove(videoId);
        l2Cache.remove(videoId);
        System.out.println("🗑️  Invalidated: " + videoId + " from L1 & L2");
    }

    // ✅ Print detailed statistics
    public void getStatistics() {
        long total  = totalReq.get();
        long hits   = l1Hits.get() + l2Hits.get() + l3Hits.get();
        double overall = total == 0 ? 0 : (hits * 100.0 / total);

        System.out.println("\n📊 Multi-Level Cache Statistics:");
        System.out.println("═".repeat(55));
        System.out.printf("   %-8s | Hit Rate | Avg Time | Hits%n", "Level");
        System.out.println("─".repeat(55));

        long l1Count = l1Hits.get();
        long l2Count = l2Hits.get();
        long l3Count = l3Hits.get();

        System.out.printf("   %-8s | %6.1f%%  | %6.1f ms | %d%n",
                "L1", total==0?0:(l1Count*100.0/total), l1Count==0?0:0.5, l1Count);
        System.out.printf("   %-8s | %6.1f%%  | %6.1f ms | %d%n",
                "L2", total==0?0:(l2Count*100.0/total), l2Count==0?0:5.0, l2Count);
        System.out.printf("   %-8s | %6.1f%%  | %6.1f ms | %d%n",
                "L3", total==0?0:(l3Count*100.0/total), l3Count==0?0:150.0, l3Count);
        System.out.println("─".repeat(55));
        System.out.printf("   %-8s | %6.1f%%  | %n", "Overall", overall);
        System.out.printf("   Total Requests : %d%n", total);
        System.out.printf("   L1 Size        : %d / %d%n", l1Cache.size(), L1_MAX);
        System.out.printf("   L2 Size        : %d / %d%n", l2Cache.size(), L2_MAX);
        System.out.printf("   DB Size        : %d%n",      database.size());
        System.out.println("═".repeat(55));
    }

    // ✅ Print single access result
    public void printResult(CacheResult r) {
        if (r.hit) {
            System.out.printf("   getVideo(%-12s) → %-25s [%.1f ms]%n",
                    "\"" + r.videoId + "\"", r.source, r.timingMs);
        } else {
            System.out.printf("   getVideo(%-12s) → 🚫 NOT FOUND%n",
                    "\"" + r.videoId + "\"");
        }
    }

    // ✅ Seed database with sample videos
    private void seedDatabase() {
        for (int i = 1; i <= 50; i++) {
            String id = "video_" + String.format("%03d", i);
            database.put(id, new VideoData(id, "Movie Title " + i, 1024 * i));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Multi-Level Cache System ===\n");
        Problem10_MultiLevelCache cache = new Problem10_MultiLevelCache();

        // First access — L3 miss then DB hit
        System.out.println("🔍 First Accesses (cold cache):");
        cache.printResult(cache.getVideo("video_001")); // L3
        cache.printResult(cache.getVideo("video_002")); // L3
        cache.printResult(cache.getVideo("video_003")); // L3

        // Repeat — should hit L2
        System.out.println("\n🔍 Repeat Accesses (L2 hits):");
        cache.printResult(cache.getVideo("video_001")); // L2
        cache.printResult(cache.getVideo("video_002")); // L2

        // Access 3+ times to trigger promotion to L1
        System.out.println("\n🔍 Triggering L1 Promotion (3+ accesses):");
        cache.printResult(cache.getVideo("video_001")); // L2 → promoted to L1
        cache.printResult(cache.getVideo("video_001")); // L1 hit
        cache.printResult(cache.getVideo("video_001")); // L1 hit

        // Cache invalidation
        System.out.println("\n🗑️  Cache Invalidation:");
        cache.invalidate("video_001");
        cache.printResult(cache.getVideo("video_001")); // back to L3

        // Unknown video
        System.out.println("\n❓ Unknown Video:");
        cache.printResult(cache.getVideo("video_999"));

        // Final stats
        cache.getStatistics();
    }
}