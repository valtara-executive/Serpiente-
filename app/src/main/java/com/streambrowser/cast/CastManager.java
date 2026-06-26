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

    public interface OnCastStatusListener {
        void onConnected();
        void onDisconnected();
        void onError(String message);
    }

    public CastManager(Context context, OnCastStatusListener listener) {
        this.statusListener = listener;
        castContext = CastContext.getSharedInstance(context);
        setupSessionListener();
    }

    private void setupSessionListener() {
        SessionManager sessionManager = castContext.getSessionManager();
        sessionManager.addSessionManagerListener(new SessionManagerListener<CastSession>() {
            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                currentSession = session;
                if (statusListener != null) statusListener.onConnected();
                Log.d(TAG, "Cast session started");
            }

            @Override
            public void onSessionEnded(CastSession session, int error) {
                currentSession = null;
                if (statusListener != null) statusListener.onDisconnected();
                Log.d(TAG, "Cast session ended");
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                currentSession = session;
                if (statusListener != null) statusListener.onConnected();
            }

            @Override public void onSessionStartFailed(CastSession s, int e) {
                if (statusListener != null) statusListener.onError("No se pudo conectar al dispositivo");
            }
            @Override public void onSessionResumeFailed(CastSession s, int e) {}
            @Override public void onSessionStarting(CastSession s) {}
            @Override public void onSessionEnding(CastSession s) {}
            @Override public void onSessionSuspended(CastSession s, int r) {}
            @Override public void onSessionResuming(CastSession s, String id) {}
        }, CastSession.class);
    }

    /**
     * Cast a video URL to the connected Cast device.
     * @param videoUrl  Direct URL to the video (mp4, m3u8, webm, etc.)
     * @param title     Title shown on TV while casting
     * @param mimeType  e.g. "video/mp4" or "application/x-mpegURL" for HLS
     */
    public void castVideo(String videoUrl, String title, String mimeType) {
        if (currentSession == null) {
            if (statusListener != null) statusListener.onError("No hay dispositivo Cast conectado");
            return;
        }

        RemoteMediaClient remoteMediaClient = currentSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            if (statusListener != null) statusListener.onError("Error al obtener el cliente de medios");
            return;
        }

        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        metadata.putString(MediaMetadata.KEY_TITLE, title != null ? title : "StreamBrowser");

        MediaInfo mediaInfo = new MediaInfo.Builder(videoUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(mimeType != null ? mimeType : "video/mp4")
                .setMetadata(metadata)
                .build();

        MediaLoadRequestData loadRequest = new MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build();

        remoteMediaClient.load(loadRequest)
                .addStatusListener(result -> {
                    if (!result.isSuccess()) {
                        Log.e(TAG, "Cast load failed: " + result.toString());
                        if (statusListener != null)
                            statusListener.onError("No se pudo cargar el video en el dispositivo");
                    } else {
                        Log.d(TAG, "Cast load success");
                    }
                });
    }

    public boolean isConnected() {
        return currentSession != null && currentSession.isConnected();
    }

    /**
     * Detect MIME type from URL extension
     */
    public static String detectMimeType(String url) {
        if (url == null) return "video/mp4";
        String lower = url.toLowerCase();
        if (lower.contains(".m3u8")) return "application/x-mpegURL";
        if (lower.contains(".mpd")) return "application/dash+xml";
        if (lower.contains(".webm")) return "video/webm";
        if (lower.contains(".ogv")) return "video/ogg";
        return "video/mp4";
    }
}
