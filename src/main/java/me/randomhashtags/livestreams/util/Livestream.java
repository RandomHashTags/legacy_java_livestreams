package me.randomhashtags.livestreams.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public final class Livestream {
    private static final Set<StreamStatistic> EXCLUDED = new HashSet<>() {{
        add(StreamStatistic.START_TIME);
        add(StreamStatistic.STREAMER_DESCRIPTION);
        add(StreamStatistic.VIDEO_DESCRIPTION);
        add(StreamStatistic.VIDEO_GAME_ID);
    }};
    private HashMap<StreamStatistic, String> values;
    public Livestream(HashMap<StreamStatistic, String> values) {
        this.values = values;
        this.values = LocalServer.fixStream(this, StreamStatistic.VIDEO_TITLE).values;
    }

    @NotNull
    public HashMap<StreamStatistic, String> getValues() {
        return values;
    }
    @NotNull
    public void setValues(@NotNull HashMap<StreamStatistic, String> values) {
        this.values = values;
    }
    public void setValue(@NotNull StreamStatistic stat, @Nullable String value) {
        values.put(stat, value);
    }
    @NotNull
    public String get(@NotNull StreamStatistic stat) {
        return values.getOrDefault(stat, "");
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean first = true;
        for(StreamStatistic stat : values.keySet()) {
            if(!EXCLUDED.contains(stat)) {
                if(first) {
                    first = false;
                } else {
                    builder.append(",");
                }
                builder.append("\"").append(stat.name()).append("\"").append(":").append("\"").append(get(stat)).append("\"");
            }
        }
        builder.append("}");
        return builder.toString();
    }
}
