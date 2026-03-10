import java.util.*;
import java.util.stream.*;

public class Problem9_FraudDetection {

    // ✅ Transaction class
    static class Transaction {
        int    id;
        double amount;
        String merchant;
        String account;
        long   timestamp; // minutes from start

        public Transaction(int id, double amount, String merchant,
                           String account, long timestamp) {
            this.id        = id;
            this.amount    = amount;
            this.merchant  = merchant;
            this.account   = account;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("{id:%d, $%.0f, %s, %s}",
                    id, amount, merchant, account);
        }
    }

    private List<Transaction> transactions = new ArrayList<>();

    // ✅ Add transaction
    public void addTransaction(int id, double amount, String merchant,
                               String account, long timestamp) {
        transactions.add(
                new Transaction(id, amount, merchant, account, timestamp));
    }

    // ✅ Two-Sum: find pairs summing to target — O(n)
    public List<int[]> findTwoSum(double target) {
        List<int[]> results = new HashMap<Double, Transaction>() {{}} // just for scoping
                == null ? null : new ArrayList<>();
        results = new ArrayList<>();

        HashMap<Double, Transaction> complementMap = new HashMap<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;
            if (complementMap.containsKey(t.amount)) {
                Transaction match = complementMap.get(t.amount);
                results.add(new int[]{match.id, t.id});
            }
            complementMap.put(complement, t);
        }
        return results;
    }

    // ✅ Two-Sum with time window (within X minutes)
    public List<int[]> findTwoSumWithWindow(double target, long windowMinutes) {
        List<int[]> results = new ArrayList<>();

        for (int i = 0; i < transactions.size(); i++) {
            Transaction t1 = transactions.get(i);
            HashMap<Double, Transaction> map = new HashMap<>();

            for (int j = i; j < transactions.size(); j++) {
                Transaction t2 = transactions.get(j);
                if (Math.abs(t2.timestamp - t1.timestamp) > windowMinutes) continue;

                double complement = target - t2.amount;
                if (map.containsKey(t2.amount)) {
                    results.add(new int[]{map.get(t2.amount).id, t2.id});
                }
                map.put(complement, t2);
            }
        }
        return results;
    }

    // ✅ K-Sum: find K transactions summing to target
    public List<List<Integer>> findKSum(int k, double target) {
        List<List<Integer>> results = new ArrayList<>();
        kSumHelper(transactions, k, target, 0,
                new ArrayList<>(), results);
        return results;
    }

    private void kSumHelper(List<Transaction> txns, int k, double remaining,
                            int start, List<Integer> current,
                            List<List<Integer>> results) {
        if (k == 2) {
            HashMap<Double, Integer> map = new HashMap<>();
            for (int i = start; i < txns.size(); i++) {
                double comp = remaining - txns.get(i).amount;
                if (map.containsKey(txns.get(i).amount)) {
                    List<Integer> combo = new ArrayList<>(current);
                    combo.add(map.get(txns.get(i).amount));
                    combo.add(txns.get(i).id);
                    results.add(combo);
                }
                map.put(comp, txns.get(i).id);
            }
            return;
        }
        for (int i = start; i < txns.size(); i++) {
            current.add(txns.get(i).id);
            kSumHelper(txns, k - 1, remaining - txns.get(i).amount,
                    i + 1, current, results);
            current.remove(current.size() - 1);
        }
    }

    // ✅ Detect duplicate payments
    public void detectDuplicates() {
        // Key: amount_merchant → list of transactions
        HashMap<String, List<Transaction>> dupMap = new HashMap<>();

        for (Transaction t : transactions) {
            String key = t.amount + "_" + t.merchant;
            dupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        System.out.println("\n🔍 Duplicate Payment Detection:");
        System.out.println("─".repeat(50));
        boolean found = false;
        for (Map.Entry<String, List<Transaction>> entry : dupMap.entrySet()) {
            List<Transaction> dups = entry.getValue();
            if (dups.size() > 1) {
                Set<String> accounts = dups.stream()
                        .map(t -> t.account).collect(Collectors.toSet());
                if (accounts.size() > 1) {
                    found = true;
                    System.out.printf("🚨 Suspicious: $%.0f at %s%n",
                            dups.get(0).amount, dups.get(0).merchant);
                    System.out.println("   Accounts  : " + accounts);
                    System.out.println("   Transactions: " + dups);
                }
            }
        }
        if (!found) System.out.println("   ✅ No duplicates found.");
        System.out.println("─".repeat(50));
    }

    public static void main(String[] args) {
        System.out.println("=== Fraud Detection System ===\n");
        Problem9_FraudDetection fd = new Problem9_FraudDetection();

        // Add transactions
        fd.addTransaction(1, 500, "Store A", "acc1",  0);
        fd.addTransaction(2, 300, "Store B", "acc2", 15);
        fd.addTransaction(3, 200, "Store C", "acc3", 30);
        fd.addTransaction(4, 150, "Store D", "acc1", 45);
        fd.addTransaction(5, 350, "Store E", "acc2", 50);
        fd.addTransaction(6, 500, "Store A", "acc4", 60); // duplicate amount+merchant
        fd.addTransaction(7, 100, "Store F", "acc3", 70);
        fd.addTransaction(8, 400, "Store G", "acc1", 80);

        // Two-Sum
        System.out.println("🔎 findTwoSum(target=500):");
        List<int[]> pairs = fd.findTwoSum(500);
        if (pairs.isEmpty()) System.out.println("   No pairs found.");
        else pairs.forEach(p ->
                System.out.println("   → (id:" + p[0] + ", id:" + p[1] + ")"));

        // Two-Sum with time window
        System.out.println("\n🔎 findTwoSumWithWindow(500, 60 min):");
        List<int[]> windowPairs = fd.findTwoSumWithWindow(500, 60);
        if (windowPairs.isEmpty()) System.out.println("   No pairs found.");
        else windowPairs.forEach(p ->
                System.out.println("   → (id:" + p[0] + ", id:" + p[1] + ")"));

        // K-Sum
        System.out.println("\n🔎 findKSum(k=3, target=1000):");
        List<List<Integer>> kSums = fd.findKSum(3, 1000);
        if (kSums.isEmpty()) System.out.println("   No combinations found.");
        else kSums.forEach(combo ->
                System.out.println("   → ids: " + combo));

        // Duplicates
        fd.detectDuplicates();
    }
}