package me.randomhashtags.livestreams;

import me.randomhashtags.livestreams.util.Clip;
import me.randomhashtags.livestreams.util.Livestream;
import me.randomhashtags.livestreams.util.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public enum Cache {
    INSTANCE;

    private String mixerClipsJSON, twitchClipsJSON;
    private String livestreamsJSON, clipsJSON;

    private HashMap<Platform, String> streamsJSON;

    public String getLivestreamsJSON() {
        return livestreamsJSON;
    }
    public void updateLivestreamsJSON() {
        livestreamsJSON = "{" +
                "\"mixer\":" + streamsJSON.get(Platform.MIXER) + "," +
                "\"twitch\":" + streamsJSON.get(Platform.TWITCH) + "," +
                "\"youtube\":" + streamsJSON.get(Platform.YOUTUBE) +
                "}";
    }

    public String getClipsJSON() {
        return clipsJSON;
    }
    public void updateClipsJSON() {
        clipsJSON = "{" +
                "\"mixer\":" + mixerClipsJSON + "," +
                "\"twitch\":" + twitchClipsJSON +
                "}";
    }

    private String toJSONArray(Collection<?> objects) {
        final StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for(Object livestream : objects) {
            builder.append(first ? "" : ",").append(livestream.toString());
            first = false;
        }
        builder.append("]");
        return builder.toString();
    }

    public String getStreamsJSON(@NotNull Platform platform) {
        return streamsJSON.get(platform);
    }
    public void setStreamsJSON(@NotNull Platform platform, Collection<Livestream> livestreams) {
        streamsJSON.put(platform, toJSONArray(livestreams));
        updateLivestreamsJSON();
    }

    public void setTwitchClipsJSON(HashMap<String, List<Clip>> clips) {
        twitchClipsJSON = "{" +
                "\"day\":" + toJSONArray(clips.get("day")) + "," +
                "\"week\":" + toJSONArray(clips.get("week")) + "," +
                "\"month\":" + toJSONArray(clips.get("month")) + "," +
                "\"all\":" + toJSONArray(clips.get("all")) +
                "}";
        updateClipsJSON();
    }
}
