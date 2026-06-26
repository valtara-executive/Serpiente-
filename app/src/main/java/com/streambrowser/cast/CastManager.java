package com.streambrowser.cast;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

public class CastManager {

    private static final String TAG = "CastManager";
    private final CastContext castContext;
    private CastSession currentSession;
    private OnCastStatusListener statusListener;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    public interface OnCastStatusListener {
        void onConnected();
        void onDisconnected();
        void onError(String message);
    }

    public CastManager(Context context, OnCastStatusListener listener) {
        this.statusListener = listener;
        CastContext ctx = null;
        try {
            ctx = CastContext.getSharedInstance(context);
        } catch (Exception e) {
            Log.w(TAG, "Cast SDK not available: " + e.getMessage());
        }
        castContext = ctx;
        if (castContext != null) setupSessionListener();
    }

    private void setupSessionListener() {
        try {
            SessionManager sm = castContext.getSessionManager();
            // Recuperar sesión activa si existe
            CastSession existing = sm.getCurrentCastSession();
            if (existing != null && existing.isConnected()) {
                currentSession = existing;
                Log.d(TAG, "Recovered existing Cast session");
            }

            sm.addSessionManagerListener(
                new SessionManagerListener<CastSession>() {

                    @Override
                    public void onSessionStarted(CastSession session, String id) {
                        currentSession = session;
                        retryCount = 0;
                        Log.d(TAG, "Cast session started: " + id);
                        if (statusListener != null) statusListener.onConnected();
                    }

                    @Override
                    public void onSessionResumed(CastSession session, boolean wasSuspended) {
                        currentSession = session;
                        Log.d(TAG, "Cast session resumed");
                        if (statusListener != null) statusListener.onConnected();
                    }

                    @Override
                    public void onSessionEnded(CastSession session, int error) {
                        currentSession = null;
                        Log.d(TAG, "Cast session ended, error: " + error);
                        if (statusListener != null) statusListener.onDisconnected();
                    }

                    @Override
                    public void onSessionStartFailed(CastSession s, int error) {
                        Log.e(TAG, "Cast start failed: " + error);
                        if (retryCount < MAX_RETRIES) {
                            retryCount++;
                            Log.d(TAG, "Retrying cast... attempt " + retryCount);
                        } else {
                            if (statusListener != null)
                                statusListener.onError(
                                    "No se pudo conectar al dispositivo Cast. " +
                                    "Verifica que estén en la misma red WiFi.");
                        }
                    }

                    @Override public void onSessionResumeFailed(CastSession s, int e) {
                        if (statusListener != null)
                            statusListener.onError("No se pudo reconectar al dispositivo");
                    }
                    @Override public void onSessionStarting(CastSession s) {
                        Log.d(TAG, "Cast session starting...");
                    }
                    @Override public void onSessionEnding(CastSession s) {
                        Log.d(TAG, "Cast session ending...");
                    }
                    @Override public void onSessionSuspended(CastSession s, int r) {
                        Log.d(TAG, "Cast session suspended, reason: " + r);
                    }
                    @Override public void onSessionResuming(CastSession s, String id) {
                        Log.d(TAG, "Cast session resuming...");
                    }
                }, CastSession.class);

        } catch (Exception e) {
            Log.e(TAG, "SessionListener setup failed: " + e.getMessage());
        }
    }

    /**
     * Envía un video al dispositivo Cast conectado.
     * Soporta: mp4, m3u8 (HLS), mpd (DASH), webm, ogv
     */
    public void castVideo(String videoUrl, String title, String mimeType) {
        if (castContext == null) {
            if (statusListener != null)
                statusListener.onError("Cast SDK no disponible en este dispositivo");
            return;
        }
        if (currentSession == null || !currentSession.isConnected()) {
            if (statusListener != null)
                statusListener.onError(
                    "No hay TV conectada. Usa el botón 📡 para seleccionar tu dispositivo.");
            return;
        }

        RemoteMediaClient client = currentSession.getRemoteMediaClient();
        if (client == null) {
            if (statusListener != null)
                statusListener.onError("Error interno del cliente Cast");
            return;
        }

        // Detectar MIME si no se especificó
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = detectMimeType(videoUrl);
        }

        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        metadata.putString(MediaMetadata.KEY_TITLE,
            title != null && !title.isEmpty() ? title : "StreamBrowser");
        metadata.putString(MediaMetadata.KEY_SUBTITLE, "Grupo Gevizz · StreamBrowser");

        MediaInfo mediaInfo = new MediaInfo.Builder(videoUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType)
            .setMetadata(metadata)
            .build();

        MediaLoadRequestData loadRequest = new MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .setCurrentTime(0)
            .build();

        Log.d(TAG, "Casting: " + videoUrl + " [" + mimeType + "]");

        client.load(loadRequest).addStatusListener(result -> {
            if (result.isSuccess()) {
                Log.d(TAG, "Cast load SUCCESS");
            } else {
                Log.e(TAG, "Cast load FAILED: " + result);
                if (statusListener != null)
                    statusListener.onError(
                        "No se pudo cargar el video. " +
                        "El formato " + mimeType + " puede no ser compatible.");
            }
        });
    }

    /**
     * Detecta el MIME type según la URL.
     * Soporta 10+ formatos.
     */
    public static String detectMimeType(String url) {
        if (url == null) return "video/mp4";
        String lower = url.toLowerCase();

        // HLS
        if (lower.contains(".m3u8") || lower.contains("m3u8"))
            return "application/x-mpegURL";
        // MPEG-DASH
        if (lower.contains(".mpd") || lower.contains("/dash/"))
            return "application/dash+xml";
        // WebM
        if (lower.contains(".webm"))
            return "video/webm";
        // OGG
        if (lower.contains(".ogv") || lower.contains(".ogg"))
            return "video/ogg";
        // MKV
        if (lower.contains(".mkv"))
            return "video/x-matroska";
        // AVI
        if (lower.contains(".avi"))
            return "video/x-msvideo";
        // MOV
        if (lower.contains(".mov"))
            return "video/quicktime";
        // TS (MPEG Transport Stream)
        if (lower.contains(".ts") || lower.contains("segment") || lower.contains(".seg"))
            return "video/mp2t";
        // FLV
        if (lower.contains(".flv"))
            return "video/x-flv";

        // Default
        return "video/mp4";
    }

    public boolean isConnected() {
        return currentSession != null && currentSession.isConnected();
    }

    public void disconnect() {
        if (castContext != null) {
            try {
                castContext.getSessionManager().endCurrentSession(true);
            } catch (Exception e) {
                Log.e(TAG, "Disconnect error: " + e.getMessage());
            }
        }
    }
}
