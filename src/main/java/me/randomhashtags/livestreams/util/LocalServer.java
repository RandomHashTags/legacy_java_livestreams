package me.randomhashtags.livestreams.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public interface LocalServer {
    HashMap<String, Float> CURRENT_INTERVAL = new HashMap<>();

    default void start(String serverName, float newRequestInterval, float updateRequestInterval, CompletionHandler handler) {
        startAutoUpdates(serverName, newRequestInterval, updateRequestInterval, !serverName.equalsIgnoreCase("youtube"), handler);
    }

    void requestNewStreams(CompletionHandler handler);
    void updateExistingStreams(CompletionHandler handler);
    void requestNewClips(CompletionHandler handler);

    default String getDate() {
        return new Date(System.currentTimeMillis()).toString();
    }

    static Livestream fixStream(Livestream stream, StreamStatistic...stats) {
        for(StreamStatistic stat : stats) {
            String value = stream.get(stat);
            if(value != null) {
                value = value.replace("\\", "\\\\")
                        .replace("\t", "\\t")
                        .replace("\b", "\\b")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\f", "\\f")
                        //.replace("\'", "\'")
                        .replace("\"", "\\u0022");

                //final String before = value;
                //value = StringEscapeUtils.escapeJava(value);
                stream.setValue(stat, value);
            }
        }
        return stream;
    }
    static Clip fixClip(Clip clip, ClipStatistic...stats) {
        for(ClipStatistic stat : stats) {
            String value = clip.get(stat);
            if(value != null) {
                value = value.replace("\\", "\\\\")
                        .replace("\t", "\\t")
                        .replace("\b", "\\b")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\f", "\\f")
                        //.replace("\'", "\'")
                        .replace("\"", "\\u0022");

                //final String before = value;
                //value = StringEscapeUtils.escapeJava(value);
                clip.setValue(stat, value);
            }
        }
        return clip;
    }

    // intervals measured in minutes
    private void startAutoUpdates(String serverName, float newRequestInterval, float updateRequestInterval, boolean autoUpdates, CompletionHandler handler) {
        if(autoUpdates) {
            startAutoUpdates(serverName, newRequestInterval, handler);
        } else {
            startManualAutoUpdates(serverName, newRequestInterval, updateRequestInterval, handler);
        }
    }
    private void startAutoUpdates(String serverName, float newRequestInterval, CompletionHandler handler) {
        requestNewStreams(serverName);
        requestNewClips(serverName);
        final TimerTask requestNew = new TimerTask() {
            @Override
            public void run() {
            requestNewStreams(serverName);
            requestNewClips(serverName);
            }
        };

        final int newRequestDelayRounded = Math.round(60*(newRequestInterval-((int) newRequestInterval)));
        final long newRequestDelaySec = TimeUnit.SECONDS.toMillis(newRequestDelayRounded);
        final long newRequestDelayMin = TimeUnit.MINUTES.toMillis((long) newRequestInterval);
        final long newRequestDelay = newRequestDelayMin+newRequestDelaySec;

        out.println(getDate() + " - " + serverName + " - Will start refreshing new top streams every " + newRequestDelay + " milliseconds (" + TimeUnit.MILLISECONDS.toSeconds(newRequestDelay) + " seconds)");

        new Timer().scheduleAtFixedRate(requestNew, newRequestDelay, newRequestDelay);
        handler.handle(null);
    }
    private void startManualAutoUpdates(String serverName, float newRequestInterval, float updateRequestInterval, CompletionHandler handler) {
        startAutoUpdates(serverName, newRequestInterval, object -> {
        });
        CURRENT_INTERVAL.put(serverName, 0f);

        final TimerTask requestUpdate = new TimerTask() {
            @Override
            public void run() {
                final float interval = CURRENT_INTERVAL.get(serverName)+updateRequestInterval;
                CURRENT_INTERVAL.put(serverName, interval);
                if(interval >= newRequestInterval) {
                    CURRENT_INTERVAL.put(serverName, 0f);
                } else {
                    requestUpdateTopStreams(serverName);
                }
            }
        };
        final int updateRequestDelayRounded = Math.round(60*(updateRequestInterval-((int) updateRequestInterval)));
        final long updateRequestDelaySec = TimeUnit.SECONDS.toMillis(updateRequestDelayRounded);
        final long updateRequestDelayMin = TimeUnit.MINUTES.toMillis((long) updateRequestInterval);
        final long updateRequestDelay = updateRequestDelayMin+updateRequestDelaySec;

        out.println(getDate() + " - " + serverName + " - Will start updating existing top streams every " + updateRequestDelay + " milliseconds (" + (TimeUnit.MILLISECONDS.toSeconds(updateRequestDelay)) + " seconds)");

        new Timer().scheduleAtFixedRate(requestUpdate, updateRequestDelay, updateRequestDelay);
        handler.handle(null);
    }

    private void requestNewStreams(String serverName) {
        final long started = System.currentTimeMillis();
        requestNewStreams(object -> {
            out.println(getDate() + " - " + serverName + " - Successfully requested new streams (took " + (System.currentTimeMillis()-started) + "ms)");
        });
    }
    private void requestNewClips(String serverName) {
        final long started = System.currentTimeMillis();
        requestNewClips(object -> {
            out.println(getDate() + " - " + serverName + " - Successfully requested new clips (took " + (System.currentTimeMillis()-started) + "ms)");
        });
    }
    private void requestUpdateTopStreams(String serverName) {
        final long started = System.currentTimeMillis();
        updateExistingStreams(object -> {
            out.println(getDate() + " - " + serverName + " - Successfully updated existing streams (took " + (System.currentTimeMillis()-started) + "ms)");
        });
    }
}
