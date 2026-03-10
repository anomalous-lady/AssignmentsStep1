import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Problem1_UsernameChecker {

    // ✅ HashMap for O(1) lookup: username -> userId
    private HashMap<String, Integer> registeredUsers = new HashMap<>();

    // ✅ Track attempt frequency: username -> count
    private HashMap<String, Integer> attemptFrequency = new HashMap<>();

    // ✅ Track registration timestamps
    private HashMap<String, Long> registrationTime = new HashMap<>();

    // Constructor: pre-load existing users
    public Problem1_UsernameChecker() {
        registeredUsers.put("john_doe",   1001);
        registeredUsers.put("admin",      1002);
        registeredUsers.put("jane_smith", 1003);
        registeredUsers.put("user123",    1004);
        registeredUsers.put("superuser",  1005);
    }

    // ✅ Check availability in O(1)
    public boolean checkAvailability(String username) {
        // Track attempt frequency
        attemptFrequency.put(username,
                attemptFrequency.getOrDefault(username, 0) + 1);
        return !registeredUsers.containsKey(username);
    }

    // ✅ Suggest alternatives if username is taken
    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();

        // Strategy 1: append numbers 1–3
        for (int i = 1; i <= 3; i++) {
            String s = username + i;
            if (!registeredUsers.containsKey(s)) suggestions.add(s);
        }

        // Strategy 2: underscore → dot
        String dotVersion = username.replace("_", ".");
        if (!registeredUsers.containsKey(dotVersion))
            suggestions.add(dotVersion);

        // Strategy 3: append underscore
        String trailUnderscore = username + "_";
        if (!registeredUsers.containsKey(trailUnderscore))
            suggestions.add(trailUnderscore);

        // Strategy 4: prepend "the"
        String theVersion = "the_" + username;
        if (!registeredUsers.containsKey(theVersion))
            suggestions.add(theVersion);

        // Strategy 5: append random 2-digit number
        Random rand = new Random();
        String randVersion = username + (10 + rand.nextInt(90));
        if (!registeredUsers.containsKey(randVersion))
            suggestions.add(randVersion);

        return suggestions;
    }

    // ✅ Register a new username
    public boolean registerUsername(String username, int userId) {
        if (checkAvailability(username)) {
            registeredUsers.put(username, userId);
            registrationTime.put(username, System.currentTimeMillis());
            System.out.println("✅ Registered  : '" + username + "'");
            return true;
        }
        System.out.println("❌ Taken       : '" + username + "'");
        return false;
    }

    // ✅ Get most attempted username
    public String getMostAttempted() {
        return attemptFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> "\"" + e.getKey() + "\" (" + e.getValue() + " attempts)")
                .orElse("No attempts yet");
    }

    // ✅ Get top N most attempted usernames
    public void showTopAttempted(int n) {
        System.out.println("\n📊 Top " + n + " Most Attempted Usernames:");
        System.out.println("─".repeat(40));
        attemptFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(n)
                .forEach(e -> System.out.printf(
                        "   %-20s → %d attempts%n", e.getKey(), e.getValue()));
        System.out.println("─".repeat(40));
    }

    // ✅ Simulate 1000 concurrent checks per second
    public void simulateConcurrentChecks(int totalChecks)
            throws InterruptedException {

        System.out.println("\n⚡ Simulating " + totalChecks
                + " concurrent username checks...");

        ExecutorService executor = Executors.newFixedThreadPool(100);
        AtomicInteger available = new AtomicInteger(0);
        AtomicInteger taken     = new AtomicInteger(0);

        String[] testNames = {
                "admin", "john_doe", "new_user", "gamer_x",
                "alice", "bob", "superuser", "dev_99"
        };
        Random rand = new Random();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalChecks; i++) {
            final String name = testNames[rand.nextInt(testNames.length)];
            executor.submit(() -> {
                if (checkAvailability(name)) available.incrementAndGet();
                else                         taken.incrementAndGet();
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        System.out.printf("✅ Done in     : %d ms%n",      elapsed);
        System.out.printf("   Available   : %d%n",         available.get());
        System.out.printf("   Taken       : %d%n",         taken.get());
        System.out.printf("   Throughput  : %.0f checks/sec%n",
                totalChecks * 1000.0 / elapsed);
    }

    // ✅ Benchmark: HashMap O(1) vs Linear O(n)
    public void benchmark() {
        System.out.println("\n⚡ Performance Benchmark:");
        System.out.println("─".repeat(45));

        // Build a large set for linear search
        List<String> linearList = new ArrayList<>(registeredUsers.keySet());

        int checks = 100_000;

        // HashMap lookup
        long hashStart = System.nanoTime();
        for (int i = 0; i < checks; i++)
            registeredUsers.containsKey("john_doe");
        long hashEnd = System.nanoTime();

        // Linear search
        long linearStart = System.nanoTime();
        for (int i = 0; i < checks; i++)
            linearList.contains("john_doe");
        long linearEnd = System.nanoTime();

        double hashMs   = (hashEnd   - hashStart)   / 1_000_000.0;
        double linearMs = (linearEnd - linearStart) / 1_000_000.0;

        System.out.printf("   HashMap  O(1) : %.3f ms  (%d checks)%n",
                hashMs,   checks);
        System.out.printf("   Linear   O(n) : %.3f ms  (%d checks)%n",
                linearMs, checks);
        System.out.printf("   Speedup       : %.1fx faster%n",
                linearMs / hashMs);
        System.out.println("─".repeat(45));
    }

    // ✅ Show full system stats
    public void showSystemStats() {
        System.out.println("\n📦 System Statistics:");
        System.out.println("─".repeat(40));
        System.out.printf("   Registered Users  : %d%n",
                registeredUsers.size());
        System.out.printf("   Unique Attempts   : %d%n",
                attemptFrequency.size());
        System.out.printf("   Total Attempts    : %d%n",
                attemptFrequency.values().stream()
                        .mapToInt(Integer::intValue).sum());
        System.out.printf("   Most Attempted    : %s%n", getMostAttempted());
        System.out.println("─".repeat(40));
    }

    // ✅ Main method
    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Social Media Username Availability Checker ===\n");

        Problem1_UsernameChecker checker = new Problem1_UsernameChecker();

        // Check availability
        System.out.println("🔍 Availability Checks:");
        System.out.printf("   checkAvailability(\"john_doe\")   → %b (taken)%n",
                checker.checkAvailability("john_doe"));
        System.out.printf("   checkAvailability(\"jane_smith\") → %b (taken)%n",
                checker.checkAvailability("jane_smith"));
        System.out.printf("   checkAvailability(\"new_user\")   → %b (available)%n",
                checker.checkAvailability("new_user"));

        // Simulate many admin attempts
        for (int i = 0; i < 10_543; i++) checker.checkAvailability("admin");

        // Suggestions
        System.out.println("\n💡 Suggestions for taken usernames:");
        checker.suggestAlternatives("john_doe")
                .forEach(s -> System.out.println("   → " + s));

        // Register
        System.out.println("\n📝 Registration:");
        checker.registerUsername("john_doe",   2001); // fail
        checker.registerUsername("new_user99", 2002); // success

        // Most attempted
        System.out.println("\n🔥 Most Attempted: " + checker.getMostAttempted());

        // Top attempted
        checker.showTopAttempted(5);

        // Concurrent simulation
        checker.simulateConcurrentChecks(1000);

        // Benchmark
        checker.benchmark();

        // System stats
        checker.showSystemStats();
    }
}