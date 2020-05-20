package me.randomhashtags.livestreams.mixer;

import me.randomhashtags.livestreams.Cache;
import me.randomhashtags.livestreams.DataValues;
import me.randomhashtags.livestreams.Platform;
import me.randomhashtags.livestreams.util.CompletionHandler;
import me.randomhashtags.livestreams.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;

public enum Mixer implements LocalServer, RestAPI, DataValues {
    INSTANCE;

    private LinkedHashMap<String, Livestream> MIXER_STREAMS;
    private HashMap<String, String> mixerHeaders, newStreamsQuery;

    public void boot() {
        mixerHeaders = new HashMap<>() {{
            putAll(CONTENT_HEADERS);
        }};
        newStreamsQuery = new HashMap<>() {{
            put("order", "viewersCurrent:DESC");
            put("limit", Integer.toString(MIXER_REQUEST_LIMIT));
        }};

        start("Mixer", 1.00f, 0.00f, object -> {
        });
    }

    public void requestNewStreams(CompletionHandler handler) {
        new Thread(() -> requestNewMixerStreams(handler)).start();
    }

    private void requestNewMixerStreams(CompletionHandler handler) {
        try {
            requestMixerStreams(true, 0, object1 -> {
                requestMixerStreams(false, 1, object2 -> {
                    requestMixerStreams(false, 2, object3 -> {
                        requestMixerStreams(false, 3, object4 -> {
                            requestMixerStreams(false, 4, object5 -> {
                                requestMixerStreams(false, 5, object6 -> {
                                    requestMixerStreams(false, 6, object7 -> {
                                        requestMixerStreams(false, 7, object8 -> {
                                            requestMixerStreams(false, 8, object9 -> {
                                                requestMixerStreams(false, 9, handler);
                                                Cache.INSTANCE.setStreamsJSON(Platform.MIXER, MIXER_STREAMS.values());
                                            });
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestMixerStreams(boolean clearMap, int page, CompletionHandler handler) {
        final HashMap<String, String> query = new HashMap<>(newStreamsQuery);
        query.put("page", Integer.toString(page));
        tryRequesting("https://mixer.com/api/v1/channels", query, clearMap, handler);
    }

    public void tryRequesting(String url, HashMap<String, String> query, boolean clearMap, CompletionHandler handler) {
        requestJSON(url, RequestMethod.GET, mixerHeaders, query, json -> {
            final JSONArray array = new JSONArray((String) json);
            if(clearMap) {
                MIXER_STREAMS = new LinkedHashMap<>();
            }
            for(Object obj : array) {
                final JSONObject stream = (JSONObject) obj, user = stream.getJSONObject("user");
                final String id = Integer.toString(stream.getInt("id"));
                final Object streamerBio = user.get("bio"), language = stream.get("languageId"), desc = stream.get("description"), avatarUrl = user.get("avatarUrl");

                final Object gameType = stream.get("type");
                final JSONObject game = gameType instanceof JSONObject ? (JSONObject) gameType : null;
                final boolean hasGame = game != null;

                final Livestream livestream = new Livestream(new HashMap<StreamStatistic, String>() {{
                    put(StreamStatistic.START_TIME, stream.getString("updatedAt"));
                    put(StreamStatistic.VIDEO_ID, id);
                    put(StreamStatistic.VIDEO_TITLE, stream.getString("name"));
                    //put(Statistic.VIDEO_DESCRIPTION, desc instanceof String ? (String) desc : "");
                    put(StreamStatistic.VIDEO_LANGUAGE, language instanceof String ? (String) language: "");
                    put(StreamStatistic.VIDEO_GAME_NAME, hasGame ? game.getString("name") : "");
                    //put(Statistic.VIDEO_GAME_THUMBNAIL, hasGame ? game.getString("coverUrl") : "");
                    put(StreamStatistic.VIDEO_THUMBNAIL, "https://thumbs.mixer.com/channel/" + id + ".small.jpg");
                    put(StreamStatistic.STREAMER_ID, Integer.toString(user.getInt("id")));
                    put(StreamStatistic.STREAMER_NAME, user.getString("username"));
                    //put(Statistic.STREAMER_DESCRIPTION, streamerBio instanceof String ? (String) streamerBio : "");
                    put(StreamStatistic.STREAMER_PROFILE_IMG, avatarUrl instanceof String ? (String) avatarUrl : "");
                    put(StreamStatistic.VIEWERS_CURRENT, Integer.toString(stream.getInt("viewersCurrent")));
                }});
                MIXER_STREAMS.put(id, livestream);
            }
            handler.handle(json);
        });
    }
    public void updateExistingStreams(CompletionHandler handler) {
        requestNewStreams(handler);
    }

    @Override
    public void requestNewClips(CompletionHandler handler) {
    }
}
