import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Problem6_RateLimiter {

    // ✅ Token Bucket class
    static class TokenBucket {
        String clientId;
        AtomicLong tokens;
        long maxTokens;
        double refillRate;        // tokens per second
        AtomicLong lastRefillTime;
        AtomicLong totalRequests;
        AtomicLong blockedRequests;

        public TokenBucket(String clientId, long maxTokens, double refillRate) {
            this.clientId       = clientId;
            this.maxTokens      = maxTokens;
            this.tokens         = new AtomicLong(maxTokens);
            this.refillRate     = refillRate;
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
            this.totalRequests  = new AtomicLong(0);
            this.blockedRequests= new AtomicLong(0);
        }

        // Refill tokens based on elapsed time
        public synchronized void refill() {
            long now     = System.currentTimeMillis();
            long elapsed = now - lastRefillTime.get();
            long newTokens = (long)(elapsed / 1000.0 * refillRate);
            if (newTokens > 0) {
                tokens.set(Math.min(maxTokens, tokens.get() + newTokens));
                lastRefillTime.set(now);
            }
        }

        public long getSecondsUntilReset() {
            long used    = maxTokens - tokens.get();
            return (long)(used / refillRate);
        }
    }

    // ✅ Core storage: clientId → TokenBucket
    private ConcurrentHashMap<String, TokenBucket> clients
            = new ConcurrentHashMap<>();

    private final long   MAX_TOKENS   = 1000;
    private final double REFILL_RATE  = 1000.0 / 3600.0; // per second

    // Stats
    private AtomicLong totalChecks   = new AtomicLong(0);
    private AtomicLong totalAllowed  = new AtomicLong(0);
    private AtomicLong totalDenied   = new AtomicLong(0);

    // ✅ Main rate limit check — O(1)
    public String checkRateLimit(String clientId) {
        long startTime = System.nanoTime();
        totalChecks.incrementAndGet();

        TokenBucket bucket = clients.computeIfAbsent(clientId,
                id -> new TokenBucket(id, MAX_TOKENS, REFILL_RATE));

        bucket.refill();
        bucket.totalRequests.incrementAndGet();

        long endTime = System.nanoTime();
        double ms    = (endTime - startTime) / 1_000_000.0;

        if (bucket.tokens.get() > 0) {
            long remaining = bucket.tokens.decrementAndGet();
            totalAllowed.incrementAndGet();
            return String.format("✅ ALLOWED  | Client: %-10s | Remaining: %4d | %.3f ms",
                    clientId, remaining, ms);
        } else {
            bucket.blockedRequests.incrementAndGet();
            totalDenied.incrementAndGet();
            long retryAfter = bucket.getSecondsUntilReset();
            return String.format("❌ DENIED   | Client: %-10s | Limit reached | Retry after: %ds | %.3f ms",
                    clientId, retryAfter, ms);
        }
    }

    // ✅ Get status for a client
    public void getRateLimitStatus(String clientId) {
        TokenBucket bucket = clients.get(clientId);
        if (bucket == null) {
            System.out.println("⚠️  Client not found: " + clientId);
            return;
        }
        bucket.refill();
        long used  = MAX_TOKENS - bucket.tokens.get();
        long reset = System.currentTimeMillis() / 1000 + bucket.getSecondsUntilReset();

        System.out.println("\n📊 Rate Limit Status: " + clientId);
        System.out.println("─".repeat(40));
        System.out.printf("   Used        : %d%n",   used);
        System.out.printf("   Remaining   : %d%n",   bucket.tokens.get());
        System.out.printf("   Limit       : %d%n",   MAX_TOKENS);
        System.out.printf("   Reset (unix): %d%n",   reset);
        System.out.printf("   Total Reqs  : %d%n",   bucket.totalRequests.get());
        System.out.printf("   Blocked     : %d%n",   bucket.blockedRequests.get());
        System.out.println("─".repeat(40));
    }

    // ✅ Simulate concurrent clients
    public void simulateConcurrentClients(int numClients, int reqPerClient)
            throws InterruptedException {
        System.out.println("\n⚡ Simulating " + numClients
                + " clients × " + reqPerClient + " requests...");
        ExecutorService executor = Executors.newFixedThreadPool(100);
        long startTime = System.currentTimeMillis();

        for (int c = 0; c < numClients; c++) {
            final String clientId = "client_" + c;
            for (int r = 0; r < reqPerClient; r++) {
                executor.submit(() -> checkRateLimit(clientId));
            }
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        System.out.printf("✅ Done in %d ms%n", elapsed);
        System.out.printf("   Total Checks : %,d%n", totalChecks.get());
        System.out.printf("   Allowed      : %,d%n", totalAllowed.get());
        System.out.printf("   Denied       : %,d%n", totalDenied.get());
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Distributed Rate Limiter ===\n");
        Problem6_RateLimiter limiter = new Problem6_RateLimiter();

        // Normal requests
        System.out.println("🔁 Normal Requests:");
        for (int i = 0; i < 5; i++)
            System.out.println(limiter.checkRateLimit("abc123"));

        // Exhaust limit
        System.out.println("\n💥 Exhausting limit for 'spammer'...");
        for (int i = 0; i < 1003; i++) {
            String result = limiter.checkRateLimit("spammer");
            if (i >= 998) System.out.println(result);
        }

        // Status check
        limiter.getRateLimitStatus("abc123");
        limiter.getRateLimitStatus("spammer");

        // Concurrent simulation
        limiter.simulateConcurrentClients(10, 50);
    }
}