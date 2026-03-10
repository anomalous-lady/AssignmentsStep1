import java.util.*;
import java.util.stream.*;

public class Problem7_Autocomplete {

    // ✅ Trie Node with HashMap children
    static class TrieNode {
        HashMap<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
        String  fullQuery   = null;
        int     frequency   = 0;
    }

    // ✅ Query suggestion result
    static class Suggestion {
        String query;
        int    frequency;
        Suggestion(String query, int frequency) {
            this.query     = query;
            this.frequency = frequency;
        }
    }

    // ✅ Core storage
    private TrieNode root = new TrieNode();

    // Global frequency map: query → count
    private HashMap<String, Integer> frequencyMap = new HashMap<>();

    // Prefix cache: prefix → top suggestions
    private HashMap<String, List<Suggestion>> prefixCache = new HashMap<>();

    // Stats
    private int totalQueries = 0;
    private int cacheHits    = 0;

    // ✅ Insert query into trie
    public void insertQuery(String query, int frequency) {
        query = query.toLowerCase().trim();
        frequencyMap.put(query,
                frequencyMap.getOrDefault(query, 0) + frequency);

        TrieNode node = root;
        for (char c : query.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEndOfWord = true;
        node.fullQuery   = query;
        node.frequency   = frequencyMap.get(query);

        // Invalidate prefix cache for this query
        for (int i = 1; i <= query.length(); i++) {
            prefixCache.remove(query.substring(0, i));
        }
    }

    // ✅ Search autocomplete for prefix — O(prefix_len + results)
    public List<Suggestion> search(String prefix) {
        prefix = prefix.toLowerCase().trim();
        totalQueries++;

        // Check prefix cache
        if (prefixCache.containsKey(prefix)) {
            cacheHits++;
            return prefixCache.get(prefix);
        }

        // Navigate to prefix node
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            if (!node.children.containsKey(c)) return new ArrayList<>();
            node = node.children.get(c);
        }

        // Collect all words under this prefix
        List<Suggestion> results = new ArrayList<>();
        collectSuggestions(node, results);

        // Sort by frequency descending, take top 10
        results.sort((a, b) -> b.frequency - a.frequency);
        List<Suggestion> top10 = results.stream()
                .limit(10)
                .collect(Collectors.toList());

        // Cache result
        prefixCache.put(prefix, top10);
        return top10;
    }

    // ✅ Collect all suggestions from a trie node (DFS)
    private void collectSuggestions(TrieNode node, List<Suggestion> results) {
        if (node.isEndOfWord && node.fullQuery != null) {
            results.add(new Suggestion(node.fullQuery, node.frequency));
        }
        for (TrieNode child : node.children.values()) {
            collectSuggestions(child, results);
        }
    }

    // ✅ Update frequency when user makes a search
    public void updateFrequency(String query) {
        query = query.toLowerCase().trim();
        int newFreq = frequencyMap.getOrDefault(query, 0) + 1;
        insertQuery(query, 1);
        System.out.printf("📈 Updated: \"%s\" → frequency: %d%n",
                query, newFreq);
    }

    // ✅ Typo correction — find closest match
    public List<String> correctTypo(String query) {
        query = query.toLowerCase().trim();
        List<String> corrections = new ArrayList<>();

        for (String known : frequencyMap.keySet()) {
            if (editDistance(query, known) <= 2 && frequencyMap.get(known) > 100) {
                corrections.add(known);
            }
        }
        corrections.sort((a, b) ->
                frequencyMap.getOrDefault(b, 0) - frequencyMap.getOrDefault(a, 0));
        return corrections.stream().limit(3).collect(Collectors.toList());
    }

    // ✅ Edit distance for typo detection
    private int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                dp[i][j] = a.charAt(i-1) == b.charAt(j-1)
                        ? dp[i-1][j-1]
                        : 1 + Math.min(dp[i-1][j-1],
                        Math.min(dp[i-1][j], dp[i][j-1]));
            }
        }
        return dp[a.length()][b.length()];
    }

    // ✅ Print suggestions nicely
    public void printSuggestions(String prefix) {
        long start = System.nanoTime();
        List<Suggestion> suggestions = search(prefix);
        long end   = System.nanoTime();
        double ms  = (end - start) / 1_000_000.0;

        System.out.printf("%nsearch(\"%s\") → %d suggestions [%.3f ms]%n",
                prefix, suggestions.size(), ms);
        System.out.println("─".repeat(50));
        for (int i = 0; i < suggestions.size(); i++) {
            System.out.printf("   %2d. %-35s (%,d searches)%n",
                    i + 1, suggestions.get(i).query, suggestions.get(i).frequency);
        }
    }

    // ✅ Stats
    public void showStats() {
        double hitRate = totalQueries == 0 ? 0
                : (cacheHits * 100.0 / totalQueries);
        System.out.println("\n📊 Autocomplete Stats:");
        System.out.println("─".repeat(35));
        System.out.printf("   Total Queries : %d%n",    totalQueries);
        System.out.printf("   Cache Hits    : %d%n",    cacheHits);
        System.out.printf("   Cache Rate    : %.1f%%%n", hitRate);
        System.out.printf("   Indexed Words : %d%n",    frequencyMap.size());
        System.out.println("─".repeat(35));
    }

    public static void main(String[] args) {
        System.out.println("=== Autocomplete System ===\n");
        Problem7_Autocomplete ac = new Problem7_Autocomplete();

        // Seed data
        System.out.println("📥 Seeding search queries...");
        ac.insertQuery("java tutorial",          1_234_567);
        ac.insertQuery("javascript",               987_654);
        ac.insertQuery("java download",            456_789);
        ac.insertQuery("java 21 features",         234_567);
        ac.insertQuery("java spring boot",         198_432);
        ac.insertQuery("javascript frameworks",    176_543);
        ac.insertQuery("java interview questions", 165_432);
        ac.insertQuery("javascript vs python",     154_321);
        ac.insertQuery("java collections",         143_210);
        ac.insertQuery("javascript promises",      132_109);
        ac.insertQuery("python tutorial",          876_543);
        ac.insertQuery("python django",            345_678);
        ac.insertQuery("programming basics",       654_321);

        // Autocomplete searches
        ac.printSuggestions("jav");
        ac.printSuggestions("java");
        ac.printSuggestions("javascript");
        ac.printSuggestions("py");

        // Update frequency (trending)
        System.out.println();
        ac.updateFrequency("java 21 features");
        ac.updateFrequency("java 21 features");
        ac.updateFrequency("java 21 features");

        // Typo correction
        System.out.println("\n🔤 Typo Corrections:");
        List<String> corrections = ac.correctTypo("jaav");
        corrections.forEach(c -> System.out.println("   Did you mean: \"" + c + "\"?"));

        // Cache hit test
        ac.printSuggestions("jav"); // should be cached
        ac.showStats();
    }
}