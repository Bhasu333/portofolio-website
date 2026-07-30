/*
 * STARTER SKELETON — Network Health Monitor
 * ------------------------------------------
 * This file gives you the STRUCTURE of the project: class names, method
 * signatures, and comments describing what each method needs to do.
 *
 * It does NOT contain the actual logic. Every method below either throws
 * an UnsupportedOperationException or has a // TODO comment — that's
 * intentional. Filling those in IS the project.
 *
 * Suggested order to implement these (matches the roadmap doc):
 *   1. ServerCheckResult   (just a data holder, do this first — it's easy)
 *   2. NetworkChecker.checkLatency()
 *   3. NetworkChecker.checkPacketLoss()
 *   4. Main — sequential loop version (Phase 2)
 *   5. Main — threaded version (Phase 3)
 *   6. TelemetryQueue      (Phase 4)
 *   7. CsvLogger           (Phase 6)
 *   8. ReportGenerator     (Phase 7)
 */

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;

// ============================================================
// 1. Data holder for a single check result.
//    This one's simple on purpose — get a quick win before the hard parts.
// ============================================================
class ServerCheckResult {
    String serverAddress;
    long timestampMillis;
    long latencyMillis;
    double packetLossPercent;
    boolean reachable;

    ServerCheckResult(String serverAddress, long timestampMillis, long latencyMillis,
                       double packetLossPercent, boolean reachable) {
        this.serverAddress = serverAddress;
        this.timestampMillis = timestampMillis;
        this.latencyMillis = latencyMillis;
        this.packetLossPercent = packetLossPercent;
        this.reachable = reachable;
    }

    // TODO: add a toString() or toCsvRow() method — you'll want this
    // for Phase 6 (logging) so you're not formatting strings all over
    // your codebase.
    public String toCsvRow() {
        return timestampMillis + "," + serverAddress + "," + latencyMillis + "," + String.format("%.2f", packetLossPercent) + "," + reachable;
    }

    @Override
    public String toString() {
        if (!reachable) {
            return "[" + serverAddress + "] UNREACHABLE (loss=" + String.format("%.1f", packetLossPercent) + "%)";
        }
        return "[" + serverAddress + "] reachable in " + latencyMillis + "ms (loss=" + String.format("%.1f", packetLossPercent) + "%)";
    }
}


// ============================================================
// 2. Does the actual network probing.
//    This is where java.net.Socket (or InetAddress) comes in.
// ============================================================
class NetworkChecker {

    /**
     * Attempts a single connection to the given host/port and measures
     * how long it took.
     *
     * Hints:
     * - Look at java.net.Socket's constructor that takes a timeout.
     * - You'll want to record a start time before connecting and an end
     *   time after — the difference is your latency.
     * - Think about what should happen if the connection fails or times
     *   out. Don't let an exception crash your whole program — one dead
     *   server shouldn't take down the monitor for the rest.
     */
    public static long checkLatency(String host, int port, int timeoutMillis) {
        // TODO: implement using java.net.Socket
        long start = System.currentTimeMillis();
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), timeoutMillis);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Sends multiple connection attempts to a host and calculates what
     * percentage failed.
     *
     * Hints:
     * - "Packet loss" here just means: out of N attempts, how many did
     *   NOT succeed?
     * - Reuse checkLatency() in a loop rather than writing new
     *   connection logic — each attempt either succeeds or throws/fails.
     * - Decide: is a single slow-but-successful attempt a "loss," or
     *   only a fully failed one? There's no single right answer — pick
     *   one and be able to explain why.
     */
    public static double checkPacketLoss(String host, int port, int attempts, int timeoutMillis) {
        // TODO: implement — loop `attempts` times, count failures, return percentage
        if (attempts <= 0) return 0.0;
        int failed = 0;
        for (int i = 0; i < attempts; i++) {
            long lat = checkLatency(host, port, timeoutMillis);
            if (lat < 0) {
                failed++;
            }
        }
        return ((double) failed / attempts) * 100.0;
    }
}


// ============================================================
// 3. Thread-safe holding area for results collected across multiple
//    threads before they get written to disk.
// ============================================================
class TelemetryQueue {
    // TODO: back this with a ConcurrentLinkedQueue<ServerCheckResult>
    // Think about: why can't you just use a regular ArrayList here once
    // multiple threads are adding to it at the same time? (You don't
    // need to know the answer yet — you're meant to discover it by
    // breaking something in Phase 3, then coming back to fix it here.)
    private final ConcurrentLinkedQueue<ServerCheckResult> queue = new ConcurrentLinkedQueue<>();

    public void add(ServerCheckResult result) {
        // TODO
        if (result != null) {
            queue.add(result);
        }
    }

    public ServerCheckResult poll() {
        // TODO — should return null if the queue is empty, not throw
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}


// ============================================================
// 4. Writes results out to a CSV file on disk.
// ============================================================
class CsvLogger {
    private String filePath;

    public CsvLogger(String filePath) {
        this.filePath = filePath;
        initHeader();
    }

    private void initHeader() {
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            try (java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(file, true))) {
                bw.write("timestamp,server,latency_ms,packet_loss_pct,reachable");
                bw.newLine();
                bw.flush();
            } catch (java.io.IOException e) {
            }
        }
    }

    /**
     * Appends a single result as a new row in the CSV file.
     *
     * Hints:
     * - Look into FileWriter with the "append" constructor flag, or
     *   BufferedWriter wrapping a FileWriter.
     * - Think about what happens if this method is called from multiple
     *   threads at once — do you need to synchronize this method?
     * - Don't forget to close/flush the writer, or you may lose data
     *   when the program exits.
     */
    public synchronized void logResult(ServerCheckResult result) {
        // TODO: implement
        if (result == null) return;
        try (java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(filePath, true))) {
            bw.write(result.toCsvRow());
            bw.newLine();
            bw.flush();
        } catch (java.io.IOException e) {
        }
    }
}


// ============================================================
// 5. Reads the CSV log back and produces a human-readable summary.
//    This is Phase 7 — don't skip it, it's the most valuable part.
// ============================================================
class ReportGenerator {

    /**
     * Reads the given CSV log file and prints/returns a summary such as:
     * - which server had the worst average latency
     * - which server had the highest packet loss
     * - any notable time windows where things got worse
     *
     * Hints:
     * - Read the file back in with a BufferedReader, parse each line
     *   back into fields.
     * - You'll want some kind of grouping — e.g., a Map<String, List<ServerCheckResult>>
     *   keyed by server address — to calculate per-server stats.
     * - Keep this method's first version simple: just averages and
     *   counts per server. Time-window analysis can come after that
     *   works.
     */
    public static void generateReport(String csvFilePath) {
        // TODO: implement
        java.io.File file = new java.io.File(csvFilePath);
        if (!file.exists()) {
            System.out.println("Log file not found: " + csvFilePath);
            return;
        }

        java.util.Map<String, java.util.List<Long>> latencyMap = new java.util.HashMap<>();
        java.util.Map<String, java.util.List<Double>> lossMap = new java.util.HashMap<>();
        java.util.Map<String, Integer> failCount = new java.util.HashMap<>();
        java.util.Map<String, Integer> totalCount = new java.util.HashMap<>();

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                if (line == null || line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                String server = parts[1].trim();
                long lat = Long.parseLong(parts[2].trim());
                double loss = Double.parseDouble(parts[3].trim());
                boolean reachable = Boolean.parseBoolean(parts[4].trim());

                latencyMap.putIfAbsent(server, new java.util.ArrayList<>());
                lossMap.putIfAbsent(server, new java.util.ArrayList<>());
                failCount.putIfAbsent(server, 0);
                totalCount.putIfAbsent(server, 0);

                totalCount.put(server, totalCount.get(server) + 1);
                if (reachable && lat >= 0) {
                    latencyMap.get(server).add(lat);
                } else {
                    failCount.put(server, failCount.get(server) + 1);
                }
                lossMap.get(server).add(loss);
            }
        } catch (Exception e) {
            System.out.println("Error reading CSV log: " + e.getMessage());
            return;
        }

        System.out.println("\n==============================================");
        System.out.println("     NETWORK TELEMETRY & HEALTH REPORT        ");
        System.out.println("==============================================");

        String worstServer = null;
        double maxAvgLat = -1;

        String highestLossServer = null;
        double maxAvgLoss = -1;

        for (String s : totalCount.keySet()) {
            java.util.List<Long> lats = latencyMap.get(s);
            long sum = 0;
            for (long l : lats) sum += l;
            double avgLat = lats.isEmpty() ? 0 : (double) sum / lats.size();

            java.util.List<Double> losses = lossMap.get(s);
            double lossSum = 0;
            for (double d : losses) lossSum += d;
            double avgLoss = losses.isEmpty() ? 0 : lossSum / losses.size();

            int total = totalCount.get(s);
            int fails = failCount.get(s);

            System.out.printf("Server: %-25s | Checks: %3d | Avg Latency: %6.1f ms | Loss: %5.1f%% | Fails: %d%n",
                    s, total, avgLat, avgLoss, fails);

            if (avgLat > maxAvgLat) {
                maxAvgLat = avgLat;
                worstServer = s;
            }
            if (avgLoss > maxAvgLoss) {
                maxAvgLoss = avgLoss;
                highestLossServer = s;
            }
        }

        System.out.println("----------------------------------------------");
        if (worstServer != null) {
            System.out.printf("Worst Avg Latency Host : %s (%.1f ms)%n", worstServer, maxAvgLat);
        }
        if (highestLossServer != null) {
            System.out.printf("Highest Packet Loss Host: %s (%.1f%%)%n", highestLossServer, maxAvgLoss);
        }
        System.out.println("==============================================\n");
    }
}


// ============================================================
// 6. Entry point. Wire everything together here.
//    Build this LAST, incrementally, following the roadmap phases —
//    don't try to write the full threaded version on your first attempt.
// ============================================================
class Main {
    public static void main(String[] args) {
        // TODO Phase 2: start with a simple sequential loop over a
        // hardcoded list of servers, calling NetworkChecker directly
        // and printing results. Get that working before touching threads.

        // TODO Phase 3: once Phase 2 works, convert the loop so each
        // server is checked on its own Thread.

        // TODO Phase 4+: route results through TelemetryQueue instead
        // of printing directly.

        // TODO Phase 6: wire up CsvLogger so results get written to disk.

        // TODO Phase 7: after you've let it run and collected some data,
        // call ReportGenerator.generateReport() on the log file.

        String[] targetHosts = {"google.com", "1.1.1.1", "8.8.8.8", "127.0.0.1", "unreachable.test.invalid"};
        int targetPort = 80;
        int timeoutMillis = 1000;
        int packetLossProbes = 5;
        String logFile = "network_telemetry.csv";

        System.out.println("=== Starting Network Health Monitor Service ===");

        TelemetryQueue queue = new TelemetryQueue();
        CsvLogger logger = new CsvLogger(logFile);

        // Phase 3: Multithreaded execution across worker threads
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(targetHosts.length);
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();

        for (String host : targetHosts) {
            futures.add(executor.submit(() -> {
                long lat = NetworkChecker.checkLatency(host, targetPort, timeoutMillis);
                double loss = NetworkChecker.checkPacketLoss(host, targetPort, packetLossProbes, timeoutMillis);
                boolean reachable = (lat >= 0);
                long now = System.currentTimeMillis();

                ServerCheckResult res = new ServerCheckResult(host, now, lat, loss, reachable);
                System.out.println("Probed: " + res);
                queue.add(res);
            }));
        }

        // Wait for all probing tasks to complete
        for (java.util.concurrent.Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
            }
        }
        executor.shutdown();

        // Drain TelemetryQueue to CsvLogger (Phase 4, 5, 6)
        System.out.println("\nFlushing telemetry buffer to " + logFile + "...");
        int loggedCount = 0;
        while (!queue.isEmpty()) {
            ServerCheckResult res = queue.poll();
            if (res != null) {
                logger.logResult(res);
                loggedCount++;
            }
        }
        System.out.println("Logged " + loggedCount + " telemetry records successfully.");

        // Phase 7: Automated Report Generation
        System.out.println("\nGenerating summary report...");
        ReportGenerator.generateReport(logFile);
    }
}
