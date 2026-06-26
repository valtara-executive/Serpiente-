package com.streambrowser.adblock;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight ad blocker that parses EasyList-format filter rules.
 * Bundled rules are loaded from assets/adblock_rules.txt
 * Supports:
 *   - Domain blocking (||example.com^)
 *   - Substring matching
 *   - Whitelist exceptions (@@)
 */
public class AdBlocker {

    private static final String TAG = "AdBlocker";
    private static AdBlocker instance;

    private final Set<String> blockedDomains = new HashSet<>();
    private final List<String> blockedPatterns = new ArrayList<>();
    private final Set<String> whitelistedDomains = new HashSet<>();
    private boolean initialized = false;

    private AdBlocker() {}

    public static synchronized AdBlocker getInstance() {
        if (instance == null) instance = new AdBlocker();
        return instance;
    }

    public synchronized void init(Context context) {
        if (initialized) return;
        try {
            InputStream is = context.getAssets().open("adblock_rules.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            int loaded = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) continue;

                // Whitelist rule
                if (line.startsWith("@@")) {
                    String domain = extractDomain(line.substring(2));
                    if (domain != null) whitelistedDomains.add(domain);
                    continue;
                }

                // Domain anchor: ||ads.example.com^
                if (line.startsWith("||")) {
                    String domain = extractDomain(line.substring(2));
                    if (domain != null) {
                        blockedDomains.add(domain);
                        loaded++;
                    }
                    continue;
                }

                // Plain pattern (substring match), skip complex regex
                if (!line.startsWith("/") && !line.contains("$") && line.length() > 4) {
                    blockedPatterns.add(line.replace("*", ""));
                    loaded++;
                }
            }
            reader.close();
            initialized = true;
            Log.d(TAG, "AdBlocker initialized: " + loaded + " rules loaded");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load adblock rules: " + e.getMessage());
            initialized = true; // Don't retry on failure
        }
    }

    /**
     * Returns true if the given URL should be blocked.
     */
    public boolean shouldBlock(String url) {
        if (!initialized || url == null) return false;

        // Never block the main page load (no extension heuristic)
        if (!url.contains(".")) return false;

        String urlLower = url.toLowerCase();
        String host = extractHost(url);

        // Whitelist check first
        if (host != null) {
            for (String white : whitelistedDomains) {
                if (host.endsWith(white)) return false;
            }
        }

        // Domain blocklist
        if (host != null) {
            for (String blocked : blockedDomains) {
                if (host.equals(blocked) || host.endsWith("." + blocked)) return true;
            }
        }

        // Pattern blocklist (common ad paths/params)
        for (String pattern : blockedPatterns) {
            if (!pattern.isEmpty() && urlLower.contains(pattern)) return true;
        }

        return false;
    }

    private String extractDomain(String rule) {
        // Remove trailing options like ^$third-party
        int caretIdx = rule.indexOf('^');
        if (caretIdx > 0) rule = rule.substring(0, caretIdx);
        int slashIdx = rule.indexOf('/');
        if (slashIdx > 0) rule = rule.substring(0, slashIdx);
        // Validate looks like a domain
        if (rule.contains(".") && !rule.contains(" ")) return rule.toLowerCase();
        return null;
    }

    private String extractHost(String url) {
        try {
            if (url.startsWith("http://")) url = url.substring(7);
            else if (url.startsWith("https://")) url = url.substring(8);
            int slash = url.indexOf('/');
            return slash > 0 ? url.substring(0, slash).toLowerCase() : url.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }
}
