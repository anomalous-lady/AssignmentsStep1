import java.util.*;
import java.util.stream.*;

public class Problem4_PlagiarismDetector {

    // ✅ Custom Document class
    static class Document {
        String docId;
        String content;
        Set<String> ngrams;
        Map<String, Integer> ngramFrequency;

        public Document(String docId, String content) {
            this.docId          = docId;
            this.content        = content.toLowerCase().trim();
            this.ngrams         = new HashSet<>();
            this.ngramFrequency = new HashMap<>();
        }
    }

    // ✅ Plagiarism result class
    static class PlagiarismResult {
        String sourceDoc;
        String matchedDoc;
        int matchingNgrams;
        int totalNgrams;
        double similarityPercent;
        String verdict;

        public PlagiarismResult(String sourceDoc, String matchedDoc,
                                int matchingNgrams, int totalNgrams) {
            this.sourceDoc        = sourceDoc;
            this.matchedDoc       = matchedDoc;
            this.matchingNgrams   = matchingNgrams;
            this.totalNgrams      = totalNgrams;
            this.similarityPercent = (matchingNgrams * 100.0) / totalNgrams;
            this.verdict          = getVerdict(similarityPercent);
        }

        private String getVerdict(double similarity) {
            if (similarity >= 70) return "🚨 PLAGIARISM DETECTED";
            if (similarity >= 40) return "⚠️  HIGHLY SUSPICIOUS";
            if (similarity >= 15) return "🟡 SUSPICIOUS";
            return "✅ ORIGINAL";
        }
    }

    // ✅ Core storage
    // ngram → set of docIds that contain it
    private HashMap<String, Set<String>> ngramIndex = new HashMap<>();

    // docId → Document object
    private HashMap<String, Document> documentDatabase = new HashMap<>();

    private final int N_GRAM_SIZE;

    // Stats
    private int totalNgramsIndexed = 0;

    // Constructor
    public Problem4_PlagiarismDetector(int ngramSize) {
        this.N_GRAM_SIZE = ngramSize;
        seedDatabase(); // pre-load sample documents
    }

    // ✅ Generate n-grams from text
    private Set<String> generateNgrams(String text) {
        Set<String> ngrams      = new HashSet<>();
        String[] words          = text.replaceAll("[^a-zA-Z0-9 ]", "")
                .toLowerCase()
                .split("\\s+");

        if (words.length < N_GRAM_SIZE) return ngrams;

        for (int i = 0; i <= words.length - N_GRAM_SIZE; i++) {
            StringBuilder ngram = new StringBuilder();
            for (int j = i; j < i + N_GRAM_SIZE; j++) {
                if (j > i) ngram.append(" ");
                ngram.append(words[j]);
            }
            ngrams.add(ngram.toString());
        }
        return ngrams;
    }

    // ✅ Add document to database
    public void addDocument(String docId, String content) {
        Document doc   = new Document(docId, content);
        doc.ngrams     = generateNgrams(doc.content);

        // Count frequency of each ngram
        String[] words = doc.content.replaceAll("[^a-zA-Z0-9 ]", "")
                .split("\\s+");
        for (String ngram : doc.ngrams) {
            doc.ngramFrequency.put(ngram,
                    doc.ngramFrequency.getOrDefault(ngram, 0) + 1);
        }

        // Index ngrams
        for (String ngram : doc.ngrams) {
            ngramIndex.computeIfAbsent(ngram, k -> new HashSet<>()).add(docId);
            totalNgramsIndexed++;
        }

        documentDatabase.put(docId, doc);
        System.out.println("📄 Indexed: " + docId
                + " → " + doc.ngrams.size() + " n-grams extracted");
    }

    // ✅ Analyze document for plagiarism (main method)
    public List<PlagiarismResult> analyzeDocument(String docId) {
        Document sourceDoc = documentDatabase.get(docId);
        if (sourceDoc == null) {
            System.out.println("❌ Document not found: " + docId);
            return new ArrayList<>();
        }

        System.out.println("\n🔍 Analyzing: " + docId);
        System.out.println("   Total n-grams: " + sourceDoc.ngrams.size());

        // Count matching ngrams per document
        Map<String, Integer> matchCounts = new HashMap<>();

        long startTime = System.nanoTime();

        for (String ngram : sourceDoc.ngrams) {
            Set<String> matchingDocs = ngramIndex.getOrDefault(ngram,
                    Collections.emptySet());
            for (String matchDocId : matchingDocs) {
                if (!matchDocId.equals(docId)) {
                    matchCounts.put(matchDocId,
                            matchCounts.getOrDefault(matchDocId, 0) + 1);
                }
            }
        }

        long endTime = System.nanoTime();

        // Build results
        List<PlagiarismResult> results = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : matchCounts.entrySet()) {
            results.add(new PlagiarismResult(
                    docId,
                    entry.getKey(),
                    entry.getValue(),
                    sourceDoc.ngrams.size()
            ));
        }

        // Sort by similarity descending
        results.sort((a, b) ->
                Double.compare(b.similarityPercent, a.similarityPercent));

        // Print results
        System.out.printf("   Analysis time : %.3f ms%n",
                (endTime - startTime) / 1_000_000.0);
        System.out.println("\n   📊 Results:");
        System.out.println("   " + "─".repeat(65));
        for (PlagiarismResult r : results) {
            System.out.printf(
                    "   %-20s | Matches: %4d | Similarity: %5.1f%% | %s%n",
                    r.matchedDoc, r.matchingNgrams,
                    r.similarityPercent, r.verdict);
        }
        System.out.println("   " + "─".repeat(65));

        return results;
    }

    // ✅ Compare two specific documents directly
    public void compareDocuments(String docId1, String docId2) {
        Document doc1 = documentDatabase.get(docId1);
        Document doc2 = documentDatabase.get(docId2);

        if (doc1 == null || doc2 == null) {
            System.out.println("❌ One or both documents not found!");
            return;
        }

        // Find common ngrams
        Set<String> common = new HashSet<>(doc1.ngrams);
        common.retainAll(doc2.ngrams);

        // Union for Jaccard similarity
        Set<String> union = new HashSet<>(doc1.ngrams);
        union.addAll(doc2.ngrams);

        double jaccardSimilarity = (common.size() * 100.0) / union.size();
        double doc1Similarity    = (common.size() * 100.0) / doc1.ngrams.size();

        System.out.println("\n🔎 Direct Comparison: " + docId1 + " vs " + docId2);
        System.out.println("─".repeat(50));
        System.out.println("   Doc1 n-grams     : " + doc1.ngrams.size());
        System.out.println("   Doc2 n-grams     : " + doc2.ngrams.size());
        System.out.println("   Common n-grams   : " + common.size());
        System.out.printf ("   Jaccard Sim      : %.1f%%%n", jaccardSimilarity);
        System.out.printf ("   Doc1 Coverage    : %.1f%%%n", doc1Similarity);

        // Show some matching phrases
        System.out.println("\n   📝 Sample Matching Phrases:");
        common.stream().limit(3).forEach(ng ->
                System.out.println("   → \"" + ng + "\""));
        System.out.println("─".repeat(50));
    }

    // ✅ Performance benchmark: hash vs linear search
    public void benchmark(String docId) {
        Document sourceDoc = documentDatabase.get(docId);
        if (sourceDoc == null) return;

        System.out.println("\n⚡ Performance Benchmark:");
        System.out.println("─".repeat(45));

        // Hash-based lookup
        long hashStart = System.nanoTime();
        Map<String, Integer> hashMatches = new HashMap<>();
        for (String ngram : sourceDoc.ngrams) {
            Set<String> docs = ngramIndex.getOrDefault(ngram,
                    Collections.emptySet());
            for (String d : docs) {
                if (!d.equals(docId))
                    hashMatches.merge(d, 1, Integer::sum);
            }
        }
        long hashEnd = System.nanoTime();

        // Linear search simulation
        long linearStart = System.nanoTime();
        Map<String, Integer> linearMatches = new HashMap<>();
        for (Document otherDoc : documentDatabase.values()) {
            if (otherDoc.docId.equals(docId)) continue;
            int matches = 0;
            for (String ngram : sourceDoc.ngrams) {
                if (otherDoc.ngrams.contains(ngram)) matches++;
            }
            if (matches > 0) linearMatches.put(otherDoc.docId, matches);
        }
        long linearEnd = System.nanoTime();

        double hashMs   = (hashEnd - hashStart)     / 1_000_000.0;
        double linearMs = (linearEnd - linearStart) / 1_000_000.0;

        System.out.printf("   Hash-based search  : %.3f ms%n", hashMs);
        System.out.printf("   Linear search      : %.3f ms%n", linearMs);
        System.out.printf("   Speedup            : %.1fx faster%n",
                linearMs / hashMs);
        System.out.println("─".repeat(45));
    }

    // ✅ Database stats
    public void showDatabaseStats() {
        System.out.println("\n📊 Database Statistics:");
        System.out.println("─".repeat(40));
        System.out.println("   Total Documents  : " + documentDatabase.size());
        System.out.println("   Total N-grams    : " + totalNgramsIndexed);
        System.out.println("   Unique N-grams   : " + ngramIndex.size());
        System.out.println("   N-gram Size      : " + N_GRAM_SIZE + " words");
        System.out.println("─".repeat(40));
    }

    // ✅ Seed with sample documents
    private void seedDatabase() {
        addDocument("essay_001.txt",
                "The impact of climate change on global ecosystems has been " +
                        "profound and far reaching. Rising temperatures have caused " +
                        "significant shifts in biodiversity patterns across the world. " +
                        "Scientists warn that immediate action is needed to prevent " +
                        "irreversible damage to our planet's natural systems.");

        addDocument("essay_089.txt",
                "Climate change affects global ecosystems in many ways. " +
                        "The impact of rising temperatures on biodiversity is significant. " +
                        "Scientists warn that action is needed to prevent damage. " +
                        "The far reaching effects of global warming continue to grow.");

        addDocument("essay_092.txt",
                "The impact of climate change on global ecosystems has been " +
                        "profound and far reaching. Rising temperatures have caused " +
                        "significant shifts in biodiversity patterns across the world. " +
                        "Experts agree that the situation demands urgent global response " +
                        "to protect natural habitats and endangered species.");

        addDocument("essay_123.txt",
                "Artificial intelligence is revolutionizing industries worldwide. " +
                        "Machine learning algorithms process vast amounts of data to " +
                        "identify patterns and make predictions. Deep learning neural " +
                        "networks have achieved superhuman performance in various tasks " +
                        "including image recognition and natural language processing.");

        addDocument("essay_200.txt",
                "The impact of climate change on global ecosystems has been " +
                        "profound and far reaching. Rising temperatures have caused " +
                        "significant shifts in biodiversity patterns across the world. " +
                        "Scientists warn that immediate action is needed to prevent " +
                        "irreversible damage to our planet's natural systems.");
    }

    // ✅ Main method
    public static void main(String[] args) {

        System.out.println("=== Plagiarism Detection System ===\n");

        Problem4_PlagiarismDetector detector =
                new Problem4_PlagiarismDetector(5); // 5-grams

        // Show database stats
        detector.showDatabaseStats();

        // Analyze essay_001 against all others
        detector.analyzeDocument("essay_001.txt");

        // Analyze essay_123 (AI essay - should be original)
        detector.analyzeDocument("essay_123.txt");

        // Direct comparison
        detector.compareDocuments("essay_001.txt", "essay_092.txt");

        // Performance benchmark
        detector.benchmark("essay_001.txt");

        // Add and test a new suspicious document
        System.out.println("\n📥 Adding new suspicious document...");
        detector.addDocument("new_submission.txt",
                "The impact of climate change on global ecosystems has been " +
                        "profound and far reaching. Rising temperatures have caused " +
                        "significant shifts in biodiversity. I personally believe that " +
                        "scientists warn that immediate action is needed to prevent " +
                        "irreversible damage to our planet's natural systems.");

        detector.analyzeDocument("new_submission.txt");
    }
}

