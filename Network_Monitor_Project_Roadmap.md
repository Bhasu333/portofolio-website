# Project Roadmap: Build Your Own Network Health Monitor

**A self-guided project for learning Java, multithreading, and network programming from scratch.**

---

## Why This Project?

This project touches four skills that show up constantly in real software engineering: talking to a network, running work concurrently, handling data safely across threads, and building something that runs continuously rather than just once. It's hard on purpose — that's what makes it worth building yourself instead of copying someone else's code.

**Ground rule for using this doc:** every phase below tells you *what* to build and *what concepts* you'll need, but never *how* to write the code. When you get stuck (you will, a lot), that's the signal to go read documentation or ask a specific question — not to look for the answer key. There isn't one here on purpose.

---

## Prerequisites — What You Need to Know First

Before starting, make sure you're comfortable with:
- **Java basics**: classes, objects, methods, loops, arrays/ArrayLists
- **Basic OOP**: what a class vs. an object is, why we use them

If any of those feel shaky, spend a few days on them first. This project will be miserable if you're fighting basic Java syntax *and* new concepts at the same time.

**You do NOT need to know yet (you'll learn these during the project):**
- Multithreading
- Socket programming / networking
- Concurrent data structures
- File I/O

---

## Product Requirements (What You're Building)

**One-sentence goal:** A Java program that runs in the background, continuously checks whether a list of external servers is reachable, measures how long each check takes, and logs the results — without the checking process ever freezing or blocking the rest of the program.

**Core requirements (must-have):**
1. The program can check if a given server/host is reachable and measure how long the check took (latency).
2. The program can measure **packet loss** to a server — not just "is it up," but "out of N attempts, how many failed?" (This is a different measurement than reachability — reachability tells you up/down; packet loss tells you *how unreliable* the connection is even when it's technically "up.")
3. The program checks multiple servers on a repeating schedule (e.g., every 10 seconds) without waiting for one check to finish before starting the next.
4. Results get saved to a local CSV log so you can look back at them later.
5. If a server is unreachable, the program logs that as a failure — it doesn't crash.
6. An automated reporting feature that scans the logged data and surfaces patterns — e.g., "Server X had elevated latency between 2-3pm" or "Server Y failed 8% of checks in the last hour." This is what makes the tool actually useful instead of just a raw data dump — a human shouldn't have to manually scroll a CSV to find problems.

**Stretch requirements (nice-to-have, do these only after core works):**
7. Configurable settings (which servers to check, how often) without editing code — e.g., a config file.
8. A basic terminal dashboard that updates live instead of just writing to a file.

---

## Suggested Tech Stack

| Purpose | Tool | Why |
|---|---|---|
| Language | Java | You already know the basics |
| Network checks | `java.net.Socket` or `InetAddress.isReachable()` | Built into Java, no extra libraries needed |
| Concurrency | `java.lang.Thread` → later `ExecutorService` | Start simple, upgrade once it works |
| Thread-safe data handling | `ConcurrentLinkedQueue` or `synchronized` blocks | You'll learn *why* these exist by breaking things without them first |
| Logging | Plain file I/O (`FileWriter`/`BufferedWriter`) to a `.csv` | Simple, human-readable output |
| Build tool | None needed at first — just `javac`/`java` from the command line | Don't add Maven/Gradle complexity until the core project works |

---

## Roadmap (Do These in Order)

### Phase 0 — Environment Setup
- Install a JDK, confirm you can compile and run a "Hello World" from the command line (not just an IDE button — you want to understand what's actually happening).
- **Concept to research:** what `javac` vs `java` each do.

### Phase 1 — Single Server Check (No Threads Yet)
- Write a program that checks if ONE server (e.g., `google.com`) is reachable, and prints how long the check took.
- **Concepts to research:** `InetAddress`, what "reachability" actually means over a network, what a timeout is and why you need one.
- **Success criteria:** running the program prints something like `google.com reachable in 42ms`.

### Phase 2 — Multiple Servers, Still Sequential
- Extend Phase 1 to check a *list* of servers, one after another, in a loop.
- **Notice the problem yourself:** if one server takes 5 seconds to time out, your whole program pauses for 5 seconds before checking the next one. Sit with that problem before moving to Phase 3 — understanding *why* it's bad is the whole point.

### Phase 3 — Introduce Threads
- Rewrite Phase 2 so each server is checked on its own thread, so a slow/unreachable server doesn't block the others.
- **Concepts to research:** `Thread`, `Runnable`, what "blocking" means, race conditions.
- **Warning going in:** you will likely hit a bug where two threads try to write to the same file or list at the same time and things get corrupted or crash. Let this happen — it's the best way to actually understand why thread-safety matters.

### Phase 3.5 — Add Packet Loss Tracking
- Reachability tells you up/down. It does NOT tell you *how reliable* a connection is. Add a check that sends multiple probes (e.g., 10 quick connection attempts) to a server and calculates what percentage failed — that's your packet loss metric.
- **Concepts to research:** the difference between a single reachability check and a packet-loss measurement, why real-world monitoring tools (like the one you're building) care about loss percentage and not just uptime.
- **Success criteria:** your program can report something like "server X: 2/10 probes failed (20% loss)" alongside its latency number.

### Phase 4 — Thread-Safe Data Collection
- Fix the bug(s) from Phase 3 using a thread-safe structure like `ConcurrentLinkedQueue`, or `synchronized`.
- **Concepts to research:** what "thread-safe" actually means, why normal `ArrayList` isn't safe across threads.

### Phase 5 — Continuous Monitoring (Not Just One Pass)
- Make the program run forever (or until stopped), re-checking all servers on a repeating interval, instead of checking once and exiting.
- **Concepts to research:** `ScheduledExecutorService`, or a simple `while(true)` + `Thread.sleep()` loop (start with the simple version, it's fine).

### Phase 6 — Logging to File
- Write each check's result (timestamp, server, latency, packet loss %, success/fail) to a `.csv` file instead of just printing to console.
- **Concepts to research:** `FileWriter`/`BufferedWriter`, why you should flush/close file writers properly, what happens to open file handles if your program crashes.

### Phase 7 — Automated Reporting Module (Required, Not Optional)
- Write a *separate* program (or a mode within the same program) that reads back the CSV log and summarizes it — don't try to do this live/in-memory, reading the finished log file is simpler and a better learning step.
- At minimum, it should be able to answer: which server had the worst average latency? which server had the most failures/packet loss? were there specific time windows where things got worse?
- **Concepts to research:** reading a CSV file back into a program, basic aggregation (averages, counts, grouping by server or time bucket).
- **Why this phase matters:** this is the difference between "a script that logs numbers" and "a tool that tells a human where the problem is" — it's the most valuable part of the whole project, so don't skip it or leave it half-done.

### Phase 8 — Polish & Stretch Goals
- Pick from the stretch requirements above. Don't do all of them — pick 1-2 that sound interesting.

---

## How to Get Unstuck (Without Being Handed the Answer)

When stuck, in order:
1. **Re-read the error message slowly.** Java's errors are usually more informative than they look at first glance.
2. **Search the specific error or concept**, not "how to build a network monitor" — e.g., "java ConcurrentModificationException" not "java monitoring tool code."
3. **Read official Java docs** for the class/method you're using — they usually have a short example.
4. **Ask a specific question** to a person or an AI: not "how do I do Phase 3" but "I'm getting X error when two threads write to this ArrayList, what's going on?" — specific questions get you understanding; vague questions get you copy-paste code.

---

## A Note From Your Brother (well, from me, on his behalf)

This is going to be frustrating at multiple points — that's normal and expected, not a sign you're doing it wrong. The version of this project that "just works" because you copied it from somewhere teaches you almost nothing. The version where you fight with a race condition for two hours and finally understand *why* it happened will stick with you for years. Take the frustration as a sign you're actually learning.
