package com.streambrowser.adblock;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.*;

public class AdBlocker {

    private static final String TAG = "AdBlocker";
    private static AdBlocker instance;

    // Dominios bloqueados (red publicitaria, trackers, malware)
    private static final Set<String> BLOCKED_DOMAINS = new HashSet<>(Arrays.asList(
        // === REDES PUBLICITARIAS ===
        "doubleclick.net","googlesyndication.com","googleadservices.com",
        "adservice.google.com","adservice.google.es","adservice.google.com.mx",
        "pagead2.googlesyndication.com","tpc.googlesyndication.com",
        "ads.youtube.com","ad.doubleclick.net","stats.g.doubleclick.net",
        "cm.g.doubleclick.net","pubads.g.doubleclick.net","securepubads.g.doubleclick.net",
        "amazon-adsystem.com","s.amazon-adsystem.com","aax.amazon-adsystem.com",
        "c.amazon-adsystem.com","z-na.amazon-adsystem.com",
        "ads.facebook.com","an.facebook.com","connect.facebook.net",
        "advertising.com","aol.com","adtech.de","adtechus.com",
        "appnexus.com","ib.adnxs.com","secure.adnxs.com","cdn.adnxs.com",
        "rubiconproject.com","pixel.rubiconproject.com","fastlane.rubiconproject.com",
        "openx.net","servedby.openx.net","delivery.openx.net",
        "pubmatic.com","ads.pubmatic.com","image.pubmatic.com",
        "outbrain.com","widgets.outbrain.com","log.outbrain.com",
        "taboola.com","cdn.taboola.com","trc.taboola.com","nr-data.net",
        "criteo.com","dis.criteo.com","static.criteo.net","sslwidget.criteo.com",
        "casalemedia.com","simg.casalemedia.com",
        "smartadserver.com","diff.smartadserver.com",
        "spotxchange.com","cdn.spotxchange.com","search.spotxchange.com",
        "sharethrough.com","btloader.com","bidswitch.net","lijit.com",
        "yieldmo.com","sovrn.com","zergnet.com","revcontent.com",
        "mgid.com","a.mgid.com","servicer.mgid.com",
        "adroll.com","s.adroll.com","d.adroll.com",
        "media.net","adserver.adtech.de","adsrvr.org",
        "moatads.com","moat.com","msocdn.com","rlcdn.com",
        "serving-sys.com","bs.serving-sys.com","pixel.advertising.com",
        "ads.twitter.com","t.co","syndication.twitter.com",
        "ads.linkedin.com","px.ads.linkedin.com","snap.licdn.com",
        "ads.tiktok.com","analytics.tiktok.com",
        // === TRACKERS & ANALYTICS ===
        "google-analytics.com","ssl.google-analytics.com","www.google-analytics.com",
        "analytics.google.com","googletagmanager.com","www.googletagmanager.com",
        "googletagservices.com","www.googletagservices.com",
        "hotjar.com","static.hotjar.com","script.hotjar.com","vars.hotjar.com",
        "facebook.com","pixel.facebook.com","an.facebook.com",
        "segment.com","cdn.segment.com","api.segment.io",
        "mixpanel.com","cdn.mxpnl.com","api.mixpanel.com",
        "amplitude.com","api.amplitude.com","cdn.amplitude.com",
        "fullstory.com","rs.fullstory.com","edge.fullstory.com",
        "heap.io","cdn.heapanalytics.com","heapanalytics.com",
        "intercom.io","static.intercomcdn.com","js.intercomcdn.com",
        "mouseflow.com","cdn.mouseflow.com","a.mouseflow.com",
        "crazyegg.com","script.crazyegg.com","dnn506yrbagrg.cloudfront.net",
        "loggly.com","logs.loggly.com",
        "newrelic.com","js-agent.newrelic.com","bam.nr-data.net",
        "quantserve.com","pixel.quantserve.com",
        "scorecardresearch.com","sb.scorecardresearch.com",
        "comscore.com","beacon.krxd.net","krxd.net",
        "adsymptotic.com","adnimation.com","adacado.com",
        "turn.com","mathtag.com","pixel.mathtag.com",
        "bluekai.com","tags.bluekai.com","stags.bluekai.com",
        "demdex.net","cm.everesttech.net","everestads.net",
        "mookie1.com","b.mookie1.com",
        "crwdcntrl.net","tags.crwdcntrl.net",
        "tealiumiq.com","tags.tiqcdn.com","collect.tealiumiq.com",
        "bizrate.com","log.bizrate.com",
        "clicktale.net","s.clicktale.net",
        "inspectlet.com","cdn.inspectlet.com","hn.inspectlet.com",
        "pingdom.net","rum-static.pingdom.net",
        "yandex.ru","mc.yandex.ru","an.yandex.ru",
        // === MINEROS CRYPTO ===
        "coin-hive.com","coinhive.com","minero.pw","jsecoin.com",
        "cryptoloot.pro","webmine.pro","papoto.com","reasedoper.pw",
        "gus.host","minecrunch.co","minemytraffic.com","crypto-loot.com",
        "coinblind.com","coinlab.biz","adblockerprotector.nl.eu.org",
        // === MALWARE & PHISHING ===
        "track.webgains.com","malvertising.com","pop.trafficjunky.net",
        "trafficjunky.net","traffichaus.com","traffic.com",
        "popads.net","popcash.net","propellerads.com","propeller-ads.com",
        "exoclick.com","juicyads.com","plugrush.com","trafficstars.com",
        "adspyglass.com","hilltopads.net","datsmyad.net","etargetnet.com",
        // === POP-UPS & REDIRECTS ===
        "redirect.disqus.com","tracking.pandora.com","tracker.marinsoftware.com",
        "tracking.searchmarketing.com","pxl.connexity.net",
        "imp.tradedoubler.com","clkuk.tradedoubler.com",
        "w55c.net","p.rfihub.com","rfihub.com",
        "adblade.com","servedby.adblade.com",
        "legolas-media.com","adserver.juicyads.com",
        "exosrv.com","liveadexchanger.com","trafficshop.com",
        // === STREAMING ADS ESPECÍFICOS ===
        "ads.stickyadstv.com","ads.spotx.tv","ads.adaptv.advertising.com",
        "adaptv.advertising.com","ads.adaptv.advertising.com",
        "adsserv.vistream.online","adserver.vidazoo.com",
        "vid.springserve.com","ads.springserve.com",
        "player.anyclip.com","ads.anyclip.com",
        "jwplayer.com","cdn.jwplayer.com",
        "player.ooyala.com","player.brightcove.net",
        "ads.brightcove.com","solutions.brightcove.com"
    ));

    // Patrones de URL bloqueados
    private static final List<String> BLOCKED_PATTERNS = Arrays.asList(
        "/ads/", "/ad/", "/adserver/", "/adservice/",
        "/advertisement/", "/advertising/", "/banner/",
        "/popup/", "/popunder/", "/overlay/",
        "/tracker/", "/tracking/", "/track/",
        "/analytics/", "/pixel/", "/beacon/",
        "/telemetry/", "/metrics/", "/stats/",
        "/syndication/", "/sponsored/",
        "googlesyndication", "doubleclick",
        "adclick", "adview", "adshow",
        "ad_iframe", "ad_unit", "adsense",
        "preroll", "midroll", "postroll",
        "vast.xml", "vmap.xml", "/vast/",
        "coinminer", "cryptominer", "coinhive",
        "pop.php", "popup.php", "popunder.php"
    );

    // Scripts maliciosos bloqueados por nombre
    private static final List<String> BLOCKED_SCRIPTS = Arrays.asList(
        "ads.js", "ad.js", "adblock.js", "adserver.js",
        "analytics.js", "tracker.js", "tracking.js",
        "pixel.js", "beacon.js", "stats.js",
        "popup.js", "popunder.js", "overlay.js",
        "coinminer.js", "miner.js", "crypto.js",
        "gtag.js", "gtm.js", "fbevents.js",
        "hotjar.js", "hjar.js", "mouseflow.js"
    );

    // Dominios siempre permitidos (whitelist)
    private static final Set<String> WHITELIST = new HashSet<>(Arrays.asList(
        "valtaraexecutive.com","gevizz.com",
        "youtube.com","youtu.be",
        "google.com","googleapis.com","gstatic.com",
        "netflix.com","spotify.com","twitch.tv",
        "cloudflare.com","cloudflare-dns.com",
        "cdn.jsdelivr.net","unpkg.com"
    ));

    private final Set<String> extraBlockedDomains = new HashSet<>();
    private int blockedCount = 0;
    private boolean initialized = false;
    private static AdBlocker instance2;

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
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) continue;
                if (line.startsWith("@@")) continue; // skip whitelists from file
                if (line.startsWith("||")) {
                    String domain = extractDomain(line.substring(2));
                    if (domain != null) extraBlockedDomains.add(domain);
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Rules file error: " + e.getMessage());
        }
        initialized = true;
        Log.d(TAG, "AdBlocker ready. Extra rules: " + extraBlockedDomains.size());
    }

    public boolean shouldBlock(String url) {
        if (url == null || url.isEmpty()) return false;

        // Nunca bloquear datos o recursos del sistema
        if (url.startsWith("data:") || url.startsWith("blob:") ||
            url.startsWith("file:") || url.startsWith("about:")) return false;

        String host = extractHost(url);
        String urlLower = url.toLowerCase();

        // Whitelist check primero
        if (host != null) {
            for (String white : WHITELIST) {
                if (host.equals(white) || host.endsWith("." + white)) return false;
            }
        }

        // Bloqueo por dominio
        if (host != null) {
            if (BLOCKED_DOMAINS.contains(host)) {
                blockedCount++;
                Log.d(TAG, "BLOCKED domain: " + host);
                return true;
            }
            for (String blocked : BLOCKED_DOMAINS) {
                if (host.endsWith("." + blocked)) {
                    blockedCount++;
                    return true;
                }
            }
            // Extra rules from file
            for (String blocked : extraBlockedDomains) {
                if (host.equals(blocked) || host.endsWith("." + blocked)) {
                    blockedCount++;
                    return true;
                }
            }
        }

        // Bloqueo por patrón de URL
        for (String pattern : BLOCKED_PATTERNS) {
            if (urlLower.contains(pattern)) {
                blockedCount++;
                Log.d(TAG, "BLOCKED pattern: " + pattern + " in " + url);
                return true;
            }
        }

        // Bloqueo por nombre de script
        for (String script : BLOCKED_SCRIPTS) {
            if (urlLower.endsWith(script) || urlLower.contains("/" + script)) {
                blockedCount++;
                Log.d(TAG, "BLOCKED script: " + script);
                return true;
            }
        }

        return false;
    }

    public int getBlockedCount() { return blockedCount; }
    public void resetCount() { blockedCount = 0; }

    // CSS cosmético para inyectar y ocultar residuos de anuncios
    public static String getCosmeticCSS() {
        return
            "[id*='ad-'],[id*='-ad'],[id*='_ad'],[id*='ad_']," +
            "[class*='ad-'],[class*='-ad'],[class*='_ad'],[class*='ad_']," +
            "[id*='banner'],[class*='banner'],[id*='popup'],[class*='popup']," +
            "[id*='overlay'],[class*='overlay'],[id*='sponsor'],[class*='sponsor']," +
            "[id*='promo'],[class*='promo'],[id*='advertisement'],[class*='advertisement']," +
            "iframe[src*='doubleclick'],iframe[src*='googlesyndication']," +
            "iframe[src*='adtech'],iframe[src*='advertising']," +
            "div[aria-label='Advertisements'],div[data-ad-slot]," +
            "ins.adsbygoogle,.adsbygoogle,.ad-container,.ads-container," +
            ".google-ad,.googlead,.dfp-ad,.dfp-slot " +
            "{ display:none!important; visibility:hidden!important; " +
            "height:0!important; width:0!important; opacity:0!important; }";
    }

    private String extractDomain(String rule) {
        int caretIdx = rule.indexOf('^');
        if (caretIdx > 0) rule = rule.substring(0, caretIdx);
        int slashIdx = rule.indexOf('/');
        if (slashIdx > 0) rule = rule.substring(0, slashIdx);
        if (rule.contains(".") && !rule.contains(" ") && rule.length() > 3)
            return rule.toLowerCase();
        return null;
    }

    private String extractHost(String url) {
        try {
            if (url.startsWith("https://")) url = url.substring(8);
            else if (url.startsWith("http://")) url = url.substring(7);
            int slash = url.indexOf('/');
            int query = url.indexOf('?');
            int end = -1;
            if (slash > 0 && query > 0) end = Math.min(slash, query);
            else if (slash > 0) end = slash;
            else if (query > 0) end = query;
            return (end > 0 ? url.substring(0, end) : url).toLowerCase();
        } catch (Exception e) { return null; }
    }
}
