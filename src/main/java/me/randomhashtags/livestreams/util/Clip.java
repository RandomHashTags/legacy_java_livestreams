package me.randomhashtags.livestreams.util;

import java.util.HashMap;

public final class Clip {
    private HashMap<ClipStatistic, String> values;
    public Clip(HashMap<ClipStatistic, String> values) {
        this.values = values;
        this.values = LocalServer.fixClip(this, ClipStatistic.CLIP_TITLE).values;
    }

    @NotNull
    public HashMap<ClipStatistic, String> getValues() {
        return values;
    }
    @NotNull
    public void setValues(@NotNull HashMap<ClipStatistic, String> values) {
        this.values = values;
    }
    public void setValue(@NotNull ClipStatistic stat, @Nullable String value) {
        values.put(stat, value);
    }
    @NotNull
    public String get(@NotNull ClipStatistic stat) {
        return values.getOrDefault(stat, "");
    }

    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean first = true;
        for(ClipStatistic stat : values.keySet()) {
            if(first) {
                first = false;
            } else {
                builder.append(",");
            }
            builder.append("\"").append(stat.name()).append("\"").append(":").append("\"").append(get(stat)).append("\"");
        }
        builder.append("}");
        return builder.toString();
    }
}
