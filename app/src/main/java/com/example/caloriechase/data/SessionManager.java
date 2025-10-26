package com.example.caloriechase.data;

import android.content.Context;
import android.os.AsyncTask;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central coordinator for session lifecycle and state management
 */
public class SessionManager {
    
    private static SessionManager instance;
    private final TreasureHuntDatabase database;
    private final SessionDao sessionDao;
    private final TreasureDao treasureDao;
    private final ExecutorService executorService;
    
    // Callback interfaces
    public interface SessionCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
    
    public interface VoidCallback {
        void onSuccess();
        void onError(Exception error);
    }
    
    private SessionManager(Context context) {
        this.database = TreasureHuntDatabase.getInstance(context);
        this.sessionDao = database.sessionDao();
        this.treasureDao = database.treasureDao();
        this.executorService = Executors.newFixedThreadPool(2);
    }
    
    /**
     * Get singleton instance of SessionManager
     */
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Create a new session draft with basic parameters
     */
    public void createSessionDraft(double startLatitude, double startLongitude, 
                                 float distanceGoal, ActivityType activityType, 
                                 SessionCallback<SessionDraft> callback) {
        executorService.execute(() -> {
            try {
                // Validate input parameters
                if (distanceGoal <= 0 || distanceGoal > 5.0f) {
                    callback.onError(new IllegalArgumentException("Distance goal must be between 0.1 and 5.0 km"));
                    return;
                }
                if (activityType == null) {
                    callback.onError(new IllegalArgumentException("Activity type cannot be null"));
                    return;
                }
                
                SessionDraft draft = new SessionDraft(startLatitude, startLongitude, distanceGoal, activityType);
                
                // Validate the draft before persisting
                if (!draft.isValid()) {
                    callback.onError(new IllegalStateException("Created session draft is invalid"));
                    return;
                }
                
                sessionDao.insertSessionDraft(draft);
                callback.onSuccess(draft);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Update an existing session draft
     */
    public void updateSessionDraft(SessionDraft draft, VoidCallback callback) {
        executorService.execute(() -> {
            try {
                sessionDao.updateSessionDraft(draft);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Convert a session draft to an active session
     */
    public void upgradeToActiveSession(String sessionId, List<TreasureLocation> treasures, 
                                     SessionCallback<ActiveSession> callback) {
        executorService.execute(() -> {
            try {
                // Validate session ID
                if (sessionId == null || sessionId.isEmpty()) {
                    callback.onError(new IllegalArgumentException("Session ID cannot be null or empty"));
                    return;
                }
                
                // Check if there's already an active session
                int activeSessionCount = sessionDao.getActiveSessionCount();
                if (activeSessionCount > 0) {
                    callback.onError(new IllegalStateException("Cannot start new session: another session is already active"));
                    return;
                }
                
                // Get the draft
                SessionDraft draft = sessionDao.getSessionDraft(sessionId);
                if (draft == null) {
                    callback.onError(new IllegalArgumentException("Session draft not found: " + sessionId));
                    return;
                }
                
                // Validate draft before upgrading
                if (!draft.isValid()) {
                    callback.onError(new IllegalStateException("Session draft is invalid and cannot be upgraded"));
                    return;
                }
                
                // Create active session
                ActiveSession activeSession = ActiveSession.fromDraft(draft);
                sessionDao.insertActiveSession(activeSession);
                
                // Insert treasures with session ID validation
                if (treasures != null && !treasures.isEmpty()) {
                    for (TreasureLocation treasure : treasures) {
                        if (!sessionId.equals(treasure.sessionId)) {
                            treasure.sessionId = sessionId; // Ensure consistency
                        }
                    }
                    treasureDao.insertTreasures(treasures);
                }
                
                // Clean up draft
                sessionDao.deleteSessionDraft(draft);
                
                callback.onSuccess(activeSession);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Get the current active session
     */
    public void getCurrentActiveSession(SessionCallback<ActiveSession> callback) {
        executorService.execute(() -> {
            try {
                ActiveSession activeSession = sessionDao.getCurrentActiveSession();
                callback.onSuccess(activeSession);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Update an active session with new tracking data
     */
    public void updateActiveSession(ActiveSession activeSession, VoidCallback callback) {
        executorService.execute(() -> {
            try {
                sessionDao.updateActiveSession(activeSession);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Pause the current active session
     */
    public void pauseSession(String sessionId, VoidCallback callback) {
        executorService.execute(() -> {
            try {
                ActiveSession session = sessionDao.getActiveSession(sessionId);
                if (session != null) {
                    session.pause();
                    sessionDao.updateActiveSession(session);
                    callback.onSuccess();
                } else {
                    callback.onError(new IllegalArgumentException("Active session not found: " + sessionId));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Resume a paused session
     */
    public void resumeSession(String sessionId, VoidCallback callback) {
        executorService.execute(() -> {
            try {
                ActiveSession session = sessionDao.getActiveSession(sessionId);
                if (session != null) {
                    session.resume();
                    sessionDao.updateActiveSession(session);
                    callback.onSuccess();
                } else {
                    callback.onError(new IllegalArgumentException("Active session not found: " + sessionId));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Finalize an active session to a completed record
     */
    public void finalizeSession(String sessionId, SessionCallback<SessionRecord> callback) {
        executorService.execute(() -> {
            try {
                // Validate session ID
                if (sessionId == null || sessionId.isEmpty()) {
                    callback.onError(new IllegalArgumentException("Session ID cannot be null or empty"));
                    return;
                }
                
                // Get active session
                ActiveSession activeSession = sessionDao.getActiveSession(sessionId);
                if (activeSession == null) {
                    callback.onError(new IllegalArgumentException("Active session not found: " + sessionId));
                    return;
                }
                
                // Ensure session is not paused when finalizing
                if (activeSession.isPaused) {
                    activeSession.resume(); // Auto-resume to get accurate final duration
                }
                
                // Get treasure count
                int totalTreasures = treasureDao.getTreasureCountForSession(sessionId);
                
                // Create session record
                SessionRecord record = SessionRecord.fromActiveSession(activeSession, totalTreasures);
                
                // Validate the record before persisting
                if (record.sessionId == null || record.sessionId.isEmpty()) {
                    callback.onError(new IllegalStateException("Generated session record is invalid"));
                    return;
                }
                
                sessionDao.insertSessionRecord(record);
                
                // Clean up active session and associated treasures
                sessionDao.deleteActiveSession(activeSession);
                
                callback.onSuccess(record);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Mark a treasure as collected
     */
    public void collectTreasure(String sessionId, String treasureId, VoidCallback callback) {
        executorService.execute(() -> {
            try {
                // Mark treasure as collected
                treasureDao.markTreasureCollected(treasureId, System.currentTimeMillis());
                
                // Update active session
                ActiveSession session = sessionDao.getActiveSession(sessionId);
                if (session != null) {
                    session.addCollectedTreasure(treasureId);
                    sessionDao.updateActiveSession(session);
                }
                
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Handle treasure collection event (called by GeofenceReceiver and TreasureCollectionManager)
     */
    public void onTreasureCollected(TreasureLocation treasure) {
        executorService.execute(() -> {
            try {
                // Update active session with collected treasure
                ActiveSession session = sessionDao.getActiveSession(treasure.sessionId);
                if (session != null) {
                    session.addCollectedTreasure(treasure.treasureId);
                    sessionDao.updateActiveSession(session);
                    
                    android.util.Log.d("SessionManager", "Treasure " + treasure.treasureId + 
                                     " added to session " + treasure.sessionId);
                }
            } catch (Exception e) {
                android.util.Log.e("SessionManager", "Error handling treasure collection", e);
            }
        });
    }
    
    /**
     * Update session progress (called after treasure collection or location updates)
     */
    public void updateSessionProgress() {
        executorService.execute(() -> {
            try {
                ActiveSession session = sessionDao.getCurrentActiveSession();
                if (session != null) {
                    // Update collected treasure count
                    int collectedCount = treasureDao.getCollectedTreasureCountForSession(session.sessionId);
                    session.updateCollectedTreasureCount(collectedCount);
                    
                    sessionDao.updateActiveSession(session);
                    
                    android.util.Log.d("SessionManager", "Session progress updated for " + session.sessionId + 
                                     " - Collected treasures: " + collectedCount);
                }
            } catch (Exception e) {
                android.util.Log.e("SessionManager", "Error updating session progress", e);
            }
        });
    }
    
    /**
     * Get treasures for a session
     */
    public void getTreasuresForSession(String sessionId, SessionCallback<List<TreasureLocation>> callback) {
        executorService.execute(() -> {
            try {
                List<TreasureLocation> treasures = treasureDao.getTreasuresForSession(sessionId);
                callback.onSuccess(treasures);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Get session history
     */
    public void getSessionHistory(int limit, SessionCallback<List<SessionRecord>> callback) {
        executorService.execute(() -> {
            try {
                List<SessionRecord> records = sessionDao.getRecentSessionRecords(limit);
                callback.onSuccess(records);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Check if there's an active session
     */
    public void hasActiveSession(SessionCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                int count = sessionDao.getActiveSessionCount();
                callback.onSuccess(count > 0);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Validate session state transition
     */
    public void validateSessionState(String sessionId, SessionCallback<SessionState> callback) {
        executorService.execute(() -> {
            try {
                if (sessionId == null || sessionId.isEmpty()) {
                    callback.onError(new IllegalArgumentException("Session ID cannot be null or empty"));
                    return;
                }
                
                // Check if it's a draft
                SessionDraft draft = sessionDao.getSessionDraft(sessionId);
                if (draft != null) {
                    callback.onSuccess(SessionState.DRAFT);
                    return;
                }
                
                // Check if it's active
                ActiveSession activeSession = sessionDao.getActiveSession(sessionId);
                if (activeSession != null) {
                    SessionState state = activeSession.isPaused ? SessionState.PAUSED : SessionState.ACTIVE;
                    callback.onSuccess(state);
                    return;
                }
                
                // Check if it's completed
                SessionRecord record = sessionDao.getSessionRecord(sessionId);
                if (record != null) {
                    callback.onSuccess(SessionState.COMPLETED);
                    return;
                }
                
                // Session not found
                callback.onSuccess(SessionState.NOT_FOUND);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Force cleanup of any orphaned sessions (for error recovery)
     */
    public void cleanupOrphanedSessions(VoidCallback callback) {
        executorService.execute(() -> {
            try {
                // Get all active sessions
                List<ActiveSession> activeSessions = sessionDao.getAllActiveSessions();
                
                // For now, we'll just log the count - in a real implementation,
                // you might want to check timestamps and clean up very old sessions
                if (activeSessions.size() > 1) {
                    // Multiple active sessions shouldn't exist - this is an error state
                    // Keep the most recent one and finalize the others
                    ActiveSession mostRecent = null;
                    long latestTimestamp = 0;
                    
                    for (ActiveSession session : activeSessions) {
                        if (session.startTimestamp > latestTimestamp) {
                            latestTimestamp = session.startTimestamp;
                            mostRecent = session;
                        }
                    }
                    
                    // Finalize all except the most recent
                    for (ActiveSession session : activeSessions) {
                        if (!session.sessionId.equals(mostRecent.sessionId)) {
                            int treasureCount = treasureDao.getTreasureCountForSession(session.sessionId);
                            SessionRecord record = SessionRecord.fromActiveSession(session, treasureCount);
                            sessionDao.insertSessionRecord(record);
                            sessionDao.deleteActiveSession(session);
                        }
                    }
                }
                
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Get session statistics for dashboard
     */
    public void getSessionStatistics(SessionCallback<SessionStatistics> callback) {
        executorService.execute(() -> {
            try {
                int totalSessions = sessionDao.getTotalSessionCount();
                float totalDistance = sessionDao.getTotalDistanceTraveled();
                int totalSteps = sessionDao.getTotalStepsTaken();
                int totalCalories = sessionDao.getTotalCaloriesBurned();
                
                SessionStatistics stats = new SessionStatistics(totalSessions, totalDistance, totalSteps, totalCalories);
                callback.onSuccess(stats);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * Enum representing possible session states
     */
    public enum SessionState {
        DRAFT,
        ACTIVE,
        PAUSED,
        COMPLETED,
        NOT_FOUND
    }
    
    /**
     * Data class for session statistics
     */
    public static class SessionStatistics {
        public final int totalSessions;
        public final float totalDistance;
        public final int totalSteps;
        public final int totalCalories;
        
        public SessionStatistics(int totalSessions, float totalDistance, int totalSteps, int totalCalories) {
            this.totalSessions = totalSessions;
            this.totalDistance = totalDistance;
            this.totalSteps = totalSteps;
            this.totalCalories = totalCalories;
        }
    }
}