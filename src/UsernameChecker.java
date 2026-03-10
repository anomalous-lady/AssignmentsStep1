import java.util.*;

public class UsernameChecker {

    // HashMap for O(1) username lookup: username -> userId
    private HashMap<String, Integer> registeredUsers = new HashMap<>();

    // Track how many times each username was attempted
    private HashMap<String, Integer> attemptFrequency = new HashMap<>();

    // Constructor: pre-load some existing users
    public UsernameChecker() {
        registeredUsers.put("john_doe", 1001);
        registeredUsers.put("admin", 1002);
        registeredUsers.put("jane_smith", 1003);
        registeredUsers.put("user123", 1004);
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

        // Suggestion 1: append numbers
        for (int i = 1; i <= 3; i++) {
            String suggestion = username + i;
            if (!registeredUsers.containsKey(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        // Suggestion 2: replace underscore with dot
        String dotVersion = username.replace("_", ".");
        if (!registeredUsers.containsKey(dotVersion)) {
            suggestions.add(dotVersion);
        }

        // Suggestion 3: add underscore at end
        String underscoreVersion = username + "_";
        if (!registeredUsers.containsKey(underscoreVersion)) {
            suggestions.add(underscoreVersion);
        }

        // Suggestion 4: add random 2-digit number
        Random rand = new Random();
        String randomVersion = username + rand.nextInt(90 + 10);
        if (!registeredUsers.containsKey(randomVersion)) {
            suggestions.add(randomVersion);
        }

        return suggestions;
    }

    // ✅ Register a new username
    public boolean registerUsername(String username, int userId) {
        if (checkAvailability(username)) {
            registeredUsers.put(username, userId);
            System.out.println("✅ Username '" + username + "' registered successfully!");
            return true;
        } else {
            System.out.println("❌ Username '" + username + "' is already taken.");
            return false;
        }
    }

    // ✅ Get most attempted username
    public String getMostAttempted() {
        String mostAttempted = null;
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : attemptFrequency.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostAttempted = entry.getKey();
            }
        }

        return mostAttempted + " (" + maxCount + " attempts)";
    }

    // ✅ Display attempt statistics
    public void showAttemptStats() {
        System.out.println("\n📊 Username Attempt Statistics:");
        attemptFrequency.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.println("  " + e.getKey() + " → " + e.getValue() + " attempts"));
    }

    // ✅ Main method to test everything
    public static void main(String[] args) {
        UsernameChecker checker = new UsernameChecker();

        System.out.println("=== Social Media Username Availability Checker ===\n");

        // Test checkAvailability
        System.out.println("Checking 'john_doe'     → Available: " + checker.checkAvailability("john_doe"));
        System.out.println("Checking 'jane_smith'   → Available: " + checker.checkAvailability("jane_smith"));
        System.out.println("Checking 'new_user'     → Available: " + checker.checkAvailability("new_user"));

        // Simulate multiple attempts on popular names
        checker.checkAvailability("admin");
        checker.checkAvailability("admin");
        checker.checkAvailability("admin");
        checker.checkAvailability("admin");

        // Test suggestAlternatives
        System.out.println("\n💡 Suggestions for 'john_doe':");
        List<String> suggestions = checker.suggestAlternatives("john_doe");
        suggestions.forEach(s -> System.out.println("   → " + s));

        // Test registerUsername
        System.out.println();
        checker.registerUsername("john_doe", 2001);   // should fail
        checker.registerUsername("new_user_99", 2002); // should succeed

        // Test getMostAttempted
        System.out.println("\n🔥 Most Attempted Username: " + checker.getMostAttempted());

        // Show stats
        checker.showAttemptStats();
    }
}

