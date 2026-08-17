package com.modscript.debugger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ScriptProfiler {
    private static final Map<String, ProfilerSession> sessions = new ConcurrentHashMap<>();

    public static ProfilerSession startProfiling(String project) {
        String sessionId = project + "_prof_" + System.currentTimeMillis();
        ProfilerSession session = new ProfilerSession(sessionId, project);
        sessions.put(sessionId, session);
        return session;
    }

    public static void recordExecution(String sessionId, String operation, long timeNs) {
        ProfilerSession session = sessions.get(sessionId);
        if (session == null) return;
        session.totalTime.addAndGet(timeNs);
        session.operationCount.incrementAndGet();
        session.operations.merge(operation, 1L, Long::sum);
        session.operationTime.merge(operation, timeNs, Long::sum);
        if (timeNs > session.slowestOperationTime) {
            session.slowestOperationTime = timeNs;
            session.slowestOperation = operation;
        }
    }

    public static void endProfiling(String sessionId) {
        ProfilerSession session = sessions.get(sessionId);
        if (session != null) session.endTime = System.nanoTime();
    }

    public static ProfilerReport getReport(String sessionId) {
        ProfilerSession session = sessions.get(sessionId);
        if (session == null) return null;

        long totalMs = session.totalTime.get() / 1_000_000;
        long ops = session.operationCount.get();
        double avgUs = ops > 0 ? (session.totalTime.get() / 1000.0) / ops : 0;

        List<OperationStats> topOps = new ArrayList<>();
        for (var entry : session.operationTime.entrySet()) {
            long count = session.operations.getOrDefault(entry.getKey(), 0L);
            double avg = count > 0 ? (entry.getValue() / 1000.0) / count : 0;
            topOps.add(new OperationStats(entry.getKey(), count, entry.getValue() / 1000, avg));
        }
        topOps.sort((a, b) -> Long.compare(b.totalTimeUs, a.totalTimeUs));

        String memoryBefore = formatMemory(session.memoryBefore);
        String memoryAfter = formatMemory(session.memoryAfter);

        return new ProfilerReport(session.project, totalMs, ops, avgUs,
                session.slowestOperation, session.slowestOperationTime / 1000,
                topOps.size() > 10 ? topOps.subList(0, 10) : topOps,
                memoryBefore, memoryAfter);
    }

    private static String formatMemory(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    public static void captureMemory(String sessionId) {
        ProfilerSession session = sessions.get(sessionId);
        if (session == null) return;
        Runtime rt = Runtime.getRuntime();
        if (session.memoryBefore == 0) session.memoryBefore = rt.totalMemory() - rt.freeMemory();
        session.memoryAfter = rt.totalMemory() - rt.freeMemory();
    }

    public static class ProfilerSession {
        public final String id;
        public final String project;
        public final AtomicLong totalTime = new AtomicLong();
        public final AtomicLong operationCount = new AtomicLong();
        public final Map<String, Long> operations = new ConcurrentHashMap<>();
        public final Map<String, Long> operationTime = new ConcurrentHashMap<>();
        public long slowestOperationTime = 0;
        public String slowestOperation = "";
        public long startTime = System.nanoTime();
        public long endTime = 0;
        public long memoryBefore = 0;
        public long memoryAfter = 0;

        ProfilerSession(String id, String project) { this.id = id; this.project = project; }
    }

    public record ProfilerReport(String project, long totalMs, long operationCount, double avgMicroseconds,
                                  String slowestOp, long slowestTimeUs, List<OperationStats> topOperations,
                                  String memoryBefore, String memoryAfter) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Profiler Report: ").append(project).append(" ===\n");
            sb.append("Total time: ").append(totalMs).append("ms\n");
            sb.append("Operations: ").append(operationCount).append("\n");
            sb.append("Avg per op: ").append(String.format("%.2f", avgMicroseconds)).append("us\n");
            sb.append("Slowest: ").append(slowestOp).append(" (").append(slowestTimeUs).append("us)\n");
            sb.append("Memory: ").append(memoryBefore).append(" -> ").append(memoryAfter).append("\n\n");
            sb.append("Top Operations:\n");
            for (var op : topOperations) {
                sb.append(String.format("  %-20s %6d calls  %10d us  %8.2f us/op%n",
                        op.name, op.count, op.totalTimeUs, op.avgTimeUs));
            }
            return sb.toString();
        }
    }

    public record OperationStats(String name, long count, long totalTimeUs, double avgTimeUs) {}
}
