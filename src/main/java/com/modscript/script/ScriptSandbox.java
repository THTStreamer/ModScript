package com.modscript.script;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptSandbox {
    private static final int MAX_EXECUTION_TIME_MS = 5000;
    private static final int MAX_MEMORY_BYTES = 10 * 1024 * 1024;
    private static final int MAX_RECURSION_DEPTH = 50;
    private static final Set<String> BLOCKED_PACKAGES = Set.of("java.io", "java.net", "java.nio", "sun.", "com.sun.");
    private static final Set<String> BLOCKED_CLASSES = Set.of("Runtime", "ProcessBuilder", "System");

    private static final Map<Thread, Integer> recursionDepth = new ConcurrentHashMap<>();
    private static final Map<Thread, Long> executionStart = new ConcurrentHashMap<>();

    public static void startExecution(Thread thread) {
        recursionDepth.put(thread, 0);
        executionStart.put(thread, System.currentTimeMillis());
    }

    public static void checkExecution(Thread thread) throws SecurityException {
        Long start = executionStart.get(thread);
        if (start != null && System.currentTimeMillis() - start > MAX_EXECUTION_TIME_MS) {
            throw new SecurityException("Script execution timed out (max " + MAX_EXECUTION_TIME_MS + "ms)");
        }
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        if (usedMemory > MAX_MEMORY_BYTES) {
            throw new SecurityException("Script exceeded memory limit (" + (MAX_MEMORY_BYTES / 1024 / 1024) + "MB)");
        }
    }

    public static void enterRecursion(Thread thread) throws SecurityException {
        int depth = recursionDepth.getOrDefault(thread, 0) + 1;
        if (depth > MAX_RECURSION_DEPTH) {
            throw new SecurityException("Max recursion depth exceeded (" + MAX_RECURSION_DEPTH + ")");
        }
        recursionDepth.put(thread, depth);
    }

    public static void exitRecursion(Thread thread) {
        int depth = recursionDepth.getOrDefault(thread, 1) - 1;
        recursionDepth.put(thread, Math.max(0, depth));
    }

    public static void endExecution(Thread thread) {
        recursionDepth.remove(thread);
        executionStart.remove(thread);
    }

    public static boolean isClassAllowed(String className) {
        for (String blocked : BLOCKED_PACKAGES) {
            if (className.startsWith(blocked)) return false;
        }
        for (String blocked : BLOCKED_CLASSES) {
            if (className.endsWith("." + blocked) || className.equals(blocked)) return false;
        }
        return true;
    }

    public static void validateIR(java.util.List<IRGenerator.IRInstruction> instructions) throws SecurityException {
        int jumpCount = 0;
        for (var instr : instructions) {
            if (instr.opcode().name().startsWith("CREATE_")) jumpCount++;
            if (jumpCount > 1000) throw new SecurityException("Too many definitions (max 1000)");
        }
    }
}
