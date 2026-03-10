import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Problem2_FlashSaleInventory {

    // ✅ HashMap for O(1) stock lookup: productId -> stockCount
    private HashMap<String, AtomicInteger> inventory = new HashMap<>();

    // ✅ Waiting list per product (FIFO using LinkedHashMap/Queue)
    private HashMap<String, Queue<Integer>> waitingList = new HashMap<>();

    // ✅ Track purchase history: productId -> list of userIds
    private HashMap<String, List<Integer>> purchaseHistory = new HashMap<>();

    // Constructor: initialize products with stock
    public Problem2_FlashSaleInventory() {
        inventory.put("IPHONE15_256GB", new AtomicInteger(100));
        inventory.put("PS5_CONSOLE",    new AtomicInteger(50));
        inventory.put("NIKE_AIRMAX",    new AtomicInteger(30));

        // Initialize waiting lists and purchase history
        for (String productId : inventory.keySet()) {
            waitingList.put(productId, new LinkedList<>());
            purchaseHistory.put(productId, new ArrayList<>());
        }
    }

    // ✅ Check stock in O(1)
    public String checkStock(String productId) {
        if (!inventory.containsKey(productId)) {
            return "❌ Product not found!";
        }
        int stock = inventory.get(productId).get();
        return stock > 0
                ? stock + " units available"
                : "Out of stock! Waiting list: "
                + waitingList.get(productId).size() + " people";
    }

    // ✅ Purchase item with thread-safe atomic operation
    public synchronized String purchaseItem(String productId, int userId) {
        if (!inventory.containsKey(productId)) {
            return "❌ Product not found!";
        }

        AtomicInteger stock = inventory.get(productId);

        // Check if already purchased
        if (purchaseHistory.get(productId).contains(userId)) {
            return "⚠️  User " + userId + " already purchased this item!";
        }

        // Try to purchase
        if (stock.get() > 0) {
            int remaining = stock.decrementAndGet(); // Atomic decrement
            purchaseHistory.get(productId).add(userId);
            return "✅ Success! User " + userId
                    + " purchased. " + remaining + " units remaining.";
        } else {
            // Add to waiting list
            Queue<Integer> wList = waitingList.get(productId);
            if (!wList.contains(userId)) {
                wList.add(userId);
            }
            int position = new ArrayList<>(wList).indexOf(userId) + 1;
            return "⏳ Out of stock! User " + userId
                    + " added to waiting list. Position #" + position;
        }
    }

    // ✅ Cancel purchase and notify next in waiting list
    public String cancelPurchase(String productId, int userId) {
        if (!purchaseHistory.get(productId).contains(userId)) {
            return "❌ No purchase found for User " + userId;
        }

        // Restore stock
        inventory.get(productId).incrementAndGet();
        purchaseHistory.get(productId).remove(Integer.valueOf(userId));

        // Notify next person in waiting list
        Queue<Integer> wList = waitingList.get(productId);
        if (!wList.isEmpty()) {
            int nextUser = wList.poll();
            return "🔔 User " + userId + " cancelled. "
                    + "User " + nextUser + " notified from waiting list!";
        }

        return "✅ User " + userId + " cancelled. Stock restored.";
    }

    // ✅ Simulate 50,000 concurrent users (stress test)
    public void simulateConcurrentPurchases(String productId, int totalUsers)
            throws InterruptedException {

        System.out.println("\n⚡ Simulating " + totalUsers
                + " concurrent users for: " + productId);

        ExecutorService executor = Executors.newFixedThreadPool(100); // 100 threads
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger waitlistCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= totalUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                String result = purchaseItem(productId, userId);
                if (result.contains("✅")) successCount.incrementAndGet();
                if (result.contains("⏳")) waitlistCount.incrementAndGet();
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();

        System.out.println("✅ Successful purchases : " + successCount.get());
        System.out.println("⏳ Waiting list         : " + waitlistCount.get());
        System.out.println("⏱️  Time taken           : " + (endTime - startTime) + "ms");
    }

    // ✅ Show full inventory status
    public void showInventoryStatus() {
        System.out.println("\n📦 Current Inventory Status:");
        System.out.println("─".repeat(50));
        for (String productId : inventory.keySet()) {
            int stock    = inventory.get(productId).get();
            int sold     = purchaseHistory.get(productId).size();
            int waiting  = waitingList.get(productId).size();
            System.out.printf("  %-20s | Stock: %3d | Sold: %3d | Waiting: %3d%n",
                    productId, stock, sold, waiting);
        }
        System.out.println("─".repeat(50));
    }

    // ✅ Main method
    public static void main(String[] args) throws InterruptedException {

        Problem2_FlashSaleInventory manager = new Problem2_FlashSaleInventory();

        System.out.println("=== E-Commerce Flash Sale Inventory Manager ===");

        // Check stock
        System.out.println("\n📦 Stock Check:");
        System.out.println("IPHONE15_256GB → " + manager.checkStock("IPHONE15_256GB"));
        System.out.println("PS5_CONSOLE    → " + manager.checkStock("PS5_CONSOLE"));

        // Normal purchases
        System.out.println("\n🛒 Processing Purchases:");
        System.out.println(manager.purchaseItem("IPHONE15_256GB", 12345));
        System.out.println(manager.purchaseItem("IPHONE15_256GB", 67890));
        System.out.println(manager.purchaseItem("IPHONE15_256GB", 12345)); // duplicate

        // Show inventory
        manager.showInventoryStatus();

        // Simulate flash sale (100 stock, 200 users)
        // Reset stock for clean simulation
        manager.inventory.put("IPHONE15_256GB", new AtomicInteger(100));
        manager.waitingList.put("IPHONE15_256GB", new LinkedList<>());
        manager.purchaseHistory.put("IPHONE15_256GB", new ArrayList<>());

        manager.simulateConcurrentPurchases("IPHONE15_256GB", 200);

        // Check after flash sale
        System.out.println("\n📦 After Flash Sale:");
        System.out.println("IPHONE15_256GB → " + manager.checkStock("IPHONE15_256GB"));

        // Cancel a purchase
        System.out.println("\n❌ Cancel Purchase:");
        System.out.println(manager.cancelPurchase("IPHONE15_256GB", 1));

        // Final inventory
        manager.showInventoryStatus();
    }
}
