import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

public class Problem5_AnalyticsDashboard {

    // ✅ Page View Event class
    static class PageViewEvent {
        String url;
        String userId;
        String source;
        String country;
        long timestamp;

        public PageViewEvent(String url, String userId,
                             String source, String country) {
            this.url       = url;
            this.userId    = userId;
            this.source    = source;
            this.country   = country;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // ✅ Page Stats class
    static class PageStats {
        String url;
        AtomicLong totalViews  = new AtomicLong(0);
        Set<String> uniqueUsers = ConcurrentHashMap.newKeySet();
        AtomicLong lastUpdated  = new AtomicLong(System.currentTimeMillis());

        public PageStats(String url) {
            this.url = url;
        }

        public void recordVisit(String userId) {
            totalViews.incrementAndGet();
            uniqueUsers.add(userId);
            lastUpdated.set(System.currentTimeMillis());
        }

        public long getUniqueVisitors() { return uniqueUsers.size(); }
        public long getTotalViews()     { return totalViews.get(); }
    }

    // ✅ Core HashMaps for analytics
    // Page URL → PageStats
    private ConcurrentHashMap<String, PageStats> pageViewMap
            = new ConcurrentHashMap<>();

    // Traffic source → count
    private ConcurrentHashMap<String, AtomicLong> sourceMap
            = new ConcurrentHashMap<>();

    // Country → count
    private ConcurrentHashMap<String, AtomicLong> countryMap
            = new ConcurrentHashMap<>();

    // Minute → event count (for timeline)
    private ConcurrentHashMap<String, AtomicLong> timelineMap
            = new ConcurrentHashMap<>();

    // ✅ Global counters
    private AtomicLong totalEvents      = new AtomicLong(0);
    private AtomicLong totalUniqueUsers = new AtomicLong(0);
    private Set<String> allUsers        = ConcurrentHashMap.newKeySet();

    // Dashboard refresh interval
    private final int DASHBOARD_INTERVAL_SEC = 5;

    // Constructor
    public Problem5_AnalyticsDashboard() {
        startDashboardRefresh();
    }

    // ✅ Process single page view event — O(1)
    public void processEvent(PageViewEvent event) {
        totalEvents.incrementAndGet();

        // Track page views and unique visitors
        pageViewMap.computeIfAbsent(event.url, PageStats::new)
                .recordVisit(event.userId);

        // Track traffic source
        sourceMap.computeIfAbsent(event.source, k -> new AtomicLong(0))
                .incrementAndGet();

        // Track country
        countryMap.computeIfAbsent(event.country, k -> new AtomicLong(0))
                .incrementAndGet();

        // Track timeline (per minute bucket)
        String minuteBucket = getMinuteBucket(event.timestamp);
        timelineMap.computeIfAbsent(minuteBucket, k -> new AtomicLong(0))
                .incrementAndGet();

        // Track global unique users
        allUsers.add(event.userId);
    }

    // ✅ Get top N pages by views — O(n log k)
    public List<PageStats> getTopPages(int n) {
        return pageViewMap.values()
                .stream()
                .sorted((a, b) -> Long.compare(b.getTotalViews(), a.getTotalViews()))
                .limit(n)
                .collect(Collectors.toList());
    }

    // ✅ Get top N traffic sources
    public List<Map.Entry<String, AtomicLong>> getTopSources(int n) {
        return sourceMap.entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .limit(n)
                .collect(Collectors.toList());
    }

    // ✅ Get top N countries
    public List<Map.Entry<String, AtomicLong>> getTopCountries(int n) {
        return countryMap.entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .limit(n)
                .collect(Collectors.toList());
    }

    // ✅ Print full dashboard
    public void getDashboard() {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("       📊 REAL-TIME ANALYTICS DASHBOARD");
        System.out.printf ("       🕐 Updated: %s%n",
                new java.util.Date().toString());
        System.out.println("═".repeat(60));

        // Global stats
        System.out.println("\n🌐 Global Overview:");
        System.out.println("─".repeat(40));
        System.out.printf("   Total Page Views   : %,d%n", totalEvents.get());
        System.out.printf("   Unique Visitors    : %,d%n", allUsers.size());
        System.out.printf("   Pages Tracked      : %,d%n", pageViewMap.size());
        System.out.printf("   Traffic Sources    : %,d%n", sourceMap.size());

        // Top 10 pages
        System.out.println("\n🔥 Top 10 Most Visited Pages:");
        System.out.println("─".repeat(60));
        List<PageStats> topPages = getTopPages(10);
        for (int i = 0; i < topPages.size(); i++) {
            PageStats p = topPages.get(i);
            long      pct = totalEvents.get() == 0 ? 0
                    : (p.getTotalViews() * 100 / totalEvents.get());
            System.out.printf("   %2d. %-30s %,6d views  (%,d unique) %d%%%n",
                    i + 1, p.url, p.getTotalViews(), p.getUniqueVisitors(), pct);
        }

        // Traffic sources
        System.out.println("\n📡 Traffic Sources:");
        System.out.println("─".repeat(40));
        getTopSources(6).forEach(e -> {
            long pct = totalEvents.get() == 0 ? 0
                    : (e.getValue().get() * 100 / totalEvents.get());
            System.out.printf("   %-15s %,8d visits  (%d%%)%n",
                    e.getKey(), e.getValue().get(), pct);
        });

        // Top countries
        System.out.println("\n🌍 Top Countries:");
        System.out.println("─".repeat(40));
        getTopCountries(5).forEach(e ->
                System.out.printf("   %-15s %,8d visits%n",
                        e.getKey(), e.getValue().get()));

        System.out.println("\n" + "═".repeat(60));
    }

    // ✅ Simulate bulk traffic (stress test)
    public void simulateTraffic(int totalEvents) throws InterruptedException {
        System.out.println("\n⚡ Simulating " + totalEvents + " page view events...");

        String[] pages = {
                "/article/breaking-news",  "/sports/championship",
                "/tech/ai-revolution",     "/politics/election-2024",
                "/entertainment/oscars",   "/business/stock-market",
                "/health/covid-update",    "/science/mars-mission",
                "/travel/top-destinations","/food/best-restaurants"
        };
        String[] sources  = {"google", "facebook", "twitter",
                "direct", "instagram", "linkedin"};
        String[] countries = {"USA", "India", "UK", "Canada",
                "Australia", "Germany", "France"};

        ExecutorService executor = Executors.newFixedThreadPool(50);
        Random rand              = new Random();
        long   startTime         = System.currentTimeMillis();
        AtomicInteger processed  = new AtomicInteger(0);

        for (int i = 0; i < totalEvents; i++) {
            final int userId = i;
            executor.submit(() -> {
                PageViewEvent event = new PageViewEvent(
                        pages[rand.nextInt(pages.length)],
                        "user_" + (userId % 5000), // 5000 unique users
                        sources[rand.nextInt(sources.length)],
                        countries[rand.nextInt(countries.length)]
                );
                processEvent(event);
                processed.incrementAndGet();
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        System.out.printf("✅ Processed %,d events in %d ms (%.0f events/sec)%n",
                processed.get(),
                (endTime - startTime),
                processed.get() * 1000.0 / (endTime - startTime));
    }

    // ✅ Performance benchmark
    public void benchmark() {
        System.out.println("\n⚡ Performance Benchmark:");
        System.out.println("─".repeat(45));

        // Measure processEvent speed
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            processEvent(new PageViewEvent(
                    "/test/page", "user_" + i, "google", "USA"));
        }
        long end = System.nanoTime();

        double msPerEvent = (end - start) / 1_000_000.0 / 10000;
        double eventsPerSec = 1000.0 / msPerEvent;

        System.out.printf("   10,000 events processed%n");
        System.out.printf("   Time per event  : %.4f ms%n",   msPerEvent);
        System.out.printf("   Events/second   : %,.0f%n",     eventsPerSec);
        System.out.printf("   getTopPages()   : O(n log k)%n");
        System.out.printf("   processEvent()  : O(1)%n");
        System.out.println("─".repeat(45));
    }

    // ✅ Auto-refresh dashboard every 5 seconds
    private void startDashboardRefresh() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        // Uncomment below to enable auto-refresh:
        // scheduler.scheduleAtFixedRate(this::getDashboard,
        //     5, DASHBOARD_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    // ✅ Helper: get minute bucket key for timeline
    private String getMinuteBucket(long timestamp) {
        long minuteMs = 60 * 1000L;
        long bucket   = (timestamp / minuteMs) * minuteMs;
        return new java.util.Date(bucket).toString()
                .substring(11, 16); // HH:MM
    }

    // ✅ Main method
    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Real-Time Analytics Dashboard ===");

        Problem5_AnalyticsDashboard dashboard =
                new Problem5_AnalyticsDashboard();

        // Manual events
        System.out.println("\n📥 Processing manual events...");
        dashboard.processEvent(new PageViewEvent(
                "/article/breaking-news", "user_123", "google",    "USA"));
        dashboard.processEvent(new PageViewEvent(
                "/article/breaking-news", "user_456", "facebook",  "India"));
        dashboard.processEvent(new PageViewEvent(
                "/article/breaking-news", "user_789", "google",    "UK"));
        dashboard.processEvent(new PageViewEvent(
                "/sports/championship",   "user_123", "direct",    "USA"));
        dashboard.processEvent(new PageViewEvent(
                "/tech/ai-revolution",    "user_999", "twitter",   "Canada"));
        dashboard.processEvent(new PageViewEvent(
                "/sports/championship",   "user_456", "instagram", "Germany"));

        // Show dashboard after manual events
        dashboard.getDashboard();

        // Simulate high traffic
        dashboard.simulateTraffic(100_000);

        // Show final dashboard
        dashboard.getDashboard();

        // Benchmark
        dashboard.benchmark();
    }
}