import java.util.*;
import java.text.DecimalFormat;

public class Problem8_ParkingLot {

    // ✅ Spot status enum
    enum SpotStatus { EMPTY, OCCUPIED, DELETED }

    // ✅ Parking spot class
    static class ParkingSpot {
        int        spotNumber;
        SpotStatus status;
        String     licensePlate;
        long       entryTime;

        public ParkingSpot(int spotNumber) {
            this.spotNumber   = spotNumber;
            this.status       = SpotStatus.EMPTY;
            this.licensePlate = null;
            this.entryTime    = 0;
        }
    }

    // ✅ Parking record for billing
    static class ParkingRecord {
        String licensePlate;
        int    spotNumber;
        long   entryTime;
        long   exitTime;
        double fee;

        public ParkingRecord(String plate, int spot, long entry) {
            this.licensePlate = plate;
            this.spotNumber   = spot;
            this.entryTime    = entry;
        }
    }

    // ✅ Open addressing array
    private ParkingSpot[] slots;
    private final int     TOTAL_SPOTS;
    private final double  FEE_PER_HOUR = 5.0;

    // Stats
    private int occupiedCount = 0;
    private int totalProbes   = 0;
    private int totalParked   = 0;
    private HashMap<Integer, Integer> hourlyTraffic = new HashMap<>();
    private List<ParkingRecord>       history       = new ArrayList<>();

    // Active records: licensePlate → ParkingRecord
    private HashMap<String, ParkingRecord> activeRecords = new HashMap<>();

    public Problem8_ParkingLot(int totalSpots) {
        this.TOTAL_SPOTS = totalSpots;
        this.slots       = new ParkingSpot[totalSpots];
        for (int i = 0; i < totalSpots; i++)
            slots[i] = new ParkingSpot(i);
    }

    // ✅ Hash function: license plate → preferred spot
    private int hash(String licensePlate) {
        int hash = 0;
        for (char c : licensePlate.toCharArray())
            hash = (hash * 31 + c) % TOTAL_SPOTS;
        return Math.abs(hash);
    }

    // ✅ Park vehicle using linear probing
    public String parkVehicle(String licensePlate) {
        if (occupiedCount >= TOTAL_SPOTS)
            return "❌ Parking lot is FULL!";

        if (activeRecords.containsKey(licensePlate))
            return "⚠️  Vehicle already parked: " + licensePlate;

        int preferred = hash(licensePlate);
        int current   = preferred;
        int probes    = 0;

        // Linear probing
        while (slots[current].status == SpotStatus.OCCUPIED) {
            current = (current + 1) % TOTAL_SPOTS;
            probes++;
            if (probes >= TOTAL_SPOTS) return "❌ No spots available!";
        }

        // Assign spot
        slots[current].status       = SpotStatus.OCCUPIED;
        slots[current].licensePlate = licensePlate;
        slots[current].entryTime    = System.currentTimeMillis();

        ParkingRecord record = new ParkingRecord(
                licensePlate, current, slots[current].entryTime);
        activeRecords.put(licensePlate, record);

        // Track hourly traffic
        int hour = java.time.LocalTime.now().getHour();
        hourlyTraffic.merge(hour, 1, Integer::sum);

        occupiedCount++;
        totalProbes += probes;
        totalParked++;

        return String.format(
                "🚗 Parked: %-12s → Spot #%3d (preferred: #%3d, probes: %d)",
                licensePlate, current, preferred, probes);
    }

    // ✅ Exit vehicle and calculate fee
    public String exitVehicle(String licensePlate) {
        ParkingRecord record = activeRecords.get(licensePlate);
        if (record == null)
            return "❌ Vehicle not found: " + licensePlate;

        int  spot     = record.spotNumber;
        long exit     = System.currentTimeMillis();
        long durationMs   = exit - record.entryTime;
        double durationHr = durationMs / 3_600_000.0;
        double fee        = Math.max(FEE_PER_HOUR,
                Math.ceil(durationHr) * FEE_PER_HOUR);

        // Mark as DELETED (for open addressing)
        slots[spot].status       = SpotStatus.DELETED;
        slots[spot].licensePlate = null;

        record.exitTime = exit;
        record.fee      = fee;
        history.add(record);
        activeRecords.remove(licensePlate);
        occupiedCount--;

        long   mins = (durationMs / 1000) / 60;
        long   secs = (durationMs / 1000) % 60;

        return String.format(
                "🏁 Exited: %-12s | Spot #%3d freed | Duration: %dm %ds | Fee: $%.2f",
                licensePlate, spot, mins, secs, fee);
    }

    // ✅ Find nearest available spot
    public int findNearestSpot() {
        for (int i = 0; i < TOTAL_SPOTS; i++) {
            if (slots[i].status != SpotStatus.OCCUPIED) return i;
        }
        return -1;
    }

    // ✅ Statistics
    public void getStatistics() {
        double occupancy = (occupiedCount * 100.0) / TOTAL_SPOTS;
        double avgProbes = totalParked == 0 ? 0
                : (totalProbes * 1.0 / totalParked);

        // Find peak hour
        String peakHour = hourlyTraffic.isEmpty() ? "N/A"
                : hourlyTraffic.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + ":00-" + (e.getKey() + 1) + ":00")
                .orElse("N/A");

        double totalRevenue = history.stream()
                .mapToDouble(r -> r.fee)
                .sum();

        System.out.println("\n📊 Parking Lot Statistics:");
        System.out.println("─".repeat(45));
        System.out.printf("   Total Spots     : %d%n",      TOTAL_SPOTS);
        System.out.printf("   Occupied        : %d%n",      occupiedCount);
        System.out.printf("   Occupancy Rate  : %.1f%%%n",  occupancy);
        System.out.printf("   Avg Probes      : %.2f%n",    avgProbes);
        System.out.printf("   Total Parked    : %d%n",      totalParked);
        System.out.printf("   Peak Hour       : %s%n",      peakHour);
        System.out.printf("   Total Revenue   : $%.2f%n",   totalRevenue);
        System.out.printf("   Nearest Free    : #%d%n",     findNearestSpot());
        System.out.println("─".repeat(45));
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Parking Lot Management ===\n");
        Problem8_ParkingLot lot = new Problem8_ParkingLot(500);

        // Park vehicles
        System.out.println("🚗 Parking Vehicles:");
        System.out.println(lot.parkVehicle("ABC-1234"));
        System.out.println(lot.parkVehicle("ABC-1235"));
        System.out.println(lot.parkVehicle("XYZ-9999"));
        System.out.println(lot.parkVehicle("DEF-5678"));
        System.out.println(lot.parkVehicle("GHI-1111"));
        System.out.println(lot.parkVehicle("ABC-1234")); // duplicate

        // Wait briefly then exit
        Thread.sleep(1500);
        System.out.println("\n🏁 Exiting Vehicles:");
        System.out.println(lot.exitVehicle("ABC-1234"));
        System.out.println(lot.exitVehicle("XYZ-9999"));
        System.out.println(lot.exitVehicle("UNKNOWN-00")); // not found

        // Re-park after exit
        System.out.println("\n🔄 Re-Parking:");
        System.out.println(lot.parkVehicle("NEW-0001"));

        lot.getStatistics();
    }
}