package com.streambrowser.cast;

import android.content.Context;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.NotificationOptions;
import java.util.List;

public class CastOptionsProvider implements OptionsProvider {

    // Default media receiver works for direct mp4/m3u8 URLs
    // For sites with protected streams, a custom receiver may be needed
    public static final String CAST_APP_ID = "CC1AD845"; // Default Media Receiver

    @Override
    public CastOptions getCastOptions(Context context) {
        NotificationOptions notificationOptions = new NotificationOptions.Builder()
                .setTargetActivityClassName("com.streambrowser.ui.MainActivity")
                .build();

        CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .build();

        return new CastOptions.Builder()
                .setReceiverApplicationId(CAST_APP_ID)
                .setCastMediaOptions(mediaOptions)
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
