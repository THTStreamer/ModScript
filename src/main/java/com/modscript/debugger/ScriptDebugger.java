package com.modscript.debugger;

import com.modscript.script.IRGenerator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptDebugger {
    private static final Map<String, DebugSession> activeSessions = new ConcurrentHashMap<>();
    private static final int MAX_BREAKPOINTS = 20;

    public static DebugSession startDebug(String project, String script) {
        String sessionId = project + "_" + System.currentTimeMillis();
        List<IRGenerator.IRInstruction> instructions = new ArrayList<>();
        try {
            var lexer = new com.modscript.script.ModScriptLexer(script);
            var tokens = lexer.tokenize();
            var parser = new com.modscript.script.ModScriptParser(tokens);
            var ast = parser.parse();
            var irGen = new IRGenerator();
            instructions = irGen.generate(ast);
        } catch (Exception e) {
            instructions = List.of();
        }
        DebugSession session = new DebugSession(sessionId, project, script, instructions);
        activeSessions.put(sessionId, session);
        return session;
    }

    public static void stopDebug(String sessionId) { activeSessions.remove(sessionId); }
    public static DebugSession getSession(String sessionId) { return activeSessions.get(sessionId); }

    public static List<DebugEvent> step(String sessionId) {
        DebugSession session = activeSessions.get(sessionId);
        if (session == null || session.isFinished()) return List.of();
        List<DebugEvent> events = new ArrayList<>();
        if (session.getCurrentInstruction() < session.getInstructions().size()) {
            var instr = session.getInstructions().get(session.getCurrentInstruction());
            events.add(new DebugEvent("step", session.getCurrentInstruction(), instr.toString(), Map.of()));
            session.advance();
            if (session.getBreakpoints().contains(session.getCurrentInstruction())) {
                events.add(new DebugEvent("breakpoint", session.getCurrentInstruction(), "Breakpoint hit", Map.of()));
                session.pause();
            }
        } else {
            events.add(new DebugEvent("end", session.getCurrentInstruction(), "Script finished", Map.of()));
            session.finish();
        }
        return events;
    }

    public static List<DebugEvent> continueExecution(String sessionId) {
        List<DebugEvent> allEvents = new ArrayList<>();
        DebugSession session = activeSessions.get(sessionId);
        if (session == null) return allEvents;
        session.resume();
        while (!session.isFinished() && session.isRunning()) {
            allEvents.addAll(step(sessionId));
        }
        return allEvents;
    }

    public static boolean addBreakpoint(String sessionId, int line) {
        DebugSession session = activeSessions.get(sessionId);
        if (session == null || session.getBreakpoints().size() >= MAX_BREAKPOINTS) return false;
        session.getBreakpoints().add(line);
        return true;
    }

    public static boolean removeBreakpoint(String sessionId, int line) {
        DebugSession session = activeSessions.get(sessionId);
        if (session == null) return false;
        return session.getBreakpoints().remove(line);
    }

    public static Map<String, Object> getVariables(String sessionId) {
        DebugSession session = activeSessions.get(sessionId);
        if (session == null) return Map.of();
        Map<String, Object> vars = new HashMap<>();
        vars.put("project", session.getProject());
        vars.put("instruction", session.getCurrentInstruction());
        vars.put("total", session.getInstructions().size());
        vars.put("status", session.isFinished() ? "finished" : session.isPaused() ? "paused" : "running");
        return vars;
    }

    public static class DebugSession {
        private final String id;
        private final String project;
        private final String script;
        private final List<IRGenerator.IRInstruction> instructions;
        private int currentInstruction = 0;
        private boolean finished = false;
        private boolean paused = false;
        private boolean running = false;
        private final Set<Integer> breakpoints = new HashSet<>();

        DebugSession(String id, String project, String script, List<IRGenerator.IRInstruction> instructions) {
            this.id = id; this.project = project; this.script = script; this.instructions = instructions;
        }

        public String getId() { return id; }
        public String getProject() { return project; }
        public String getScript() { return script; }
        public List<IRGenerator.IRInstruction> getInstructions() { return instructions; }
        public int getCurrentInstruction() { return currentInstruction; }
        public boolean isFinished() { return finished; }
        public boolean isPaused() { return paused; }
        public boolean isRunning() { return running; }
        public Set<Integer> getBreakpoints() { return breakpoints; }
        public void advance() { currentInstruction++; }
        public void pause() { paused = true; running = false; }
        public void resume() { paused = false; running = true; }
        public void finish() { finished = true; running = false; }
    }

    public record DebugEvent(String type, int line, String message, Map<String, Object> data) {}
}
