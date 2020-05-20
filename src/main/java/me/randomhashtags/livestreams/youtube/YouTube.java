package me.randomhashtags.livestreams.youtube;

import me.randomhashtags.livestreams.Cache;
import me.randomhashtags.livestreams.Platform;
import me.randomhashtags.livestreams.util.CompletionHandler;
import me.randomhashtags.livestreams.DataValues;
import me.randomhashtags.livestreams.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public enum YouTube implements LocalServer, RestAPI, DataValues {
    INSTANCE;

    private HashMap<String, String> HEADERS;
    private LinkedHashMap<String, Livestream> YOUTUBE_STREAMS;

    private String CURRENT_YT_IDS;

    public void boot() {
        HEADERS = new HashMap<>() {{
            putAll(CONTENT_HEADERS);
            put(YOUTUBE_KEY_IDENTIFIER, YOUTUBE_KEY_VALUE);
        }};

        start("YouTube", 20.00f, 4.00f, object -> {
        });
    }

    public void requestNewStreams(CompletionHandler handler) {
        new Thread(() -> requestNewYouTubeStreams(handler)).start();
    }
    private void requestNewYouTubeStreams(CompletionHandler handler) {
        final HashMap<String, String> query = new HashMap<>();
        final String url = "https://www.googleapis.com/youtube/v3/search", limit = Integer.toString(YOUTUBE_REQUEST_LIMIT);
        query.put("part", "id");
        query.put("eventType", "live");
        query.put("type", "video");
        query.put("maxResults", limit);
        query.put("order", "viewCount");
        query.put("key", YOUTUBE_KEY);

        try {
            tryRequesting(url, query, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateExistingStreams(CompletionHandler handler) {
        new Thread(() -> updateExistingYouTubeStreams(handler)).start();
    }

    @Override
    public void requestNewClips(CompletionHandler handler) {
    }

    private void updateExistingYouTubeStreams(CompletionHandler handler) {
        final HashMap<String, String> query = new HashMap<>();
        query.put("key", YOUTUBE_KEY);
        query.put("id", CURRENT_YT_IDS);
        query.put("part", "snippet,liveStreamingDetails");
        updateExistingStreams(query, handler);
    }
    private void updateExistingStreams(HashMap<String, String> query, CompletionHandler handler) {
        try {
            requestJSON("https://www.googleapis.com/youtube/v3/videos", RequestMethod.GET, HEADERS, query, youtubeVideoJson -> {
                final JSONArray ytArray = new JSONObject(youtubeVideoJson.toString()).getJSONArray("items");
                final HashMap<String, Livestream> streams = new HashMap<>();
                final StringBuilder channelIds = new StringBuilder();
                for(Object obj : ytArray) {
                    final JSONObject stream = (JSONObject) obj;
                    final JSONObject snippet = stream.getJSONObject("snippet"), details = stream.getJSONObject("liveStreamingDetails");
                    final String id = stream.getString("id");
                    Object langId = snippet.has("defaultLanguage") ? snippet.get("defaultLanguage") : null, viewers = details.has("concurrentViewers") ? details.get("concurrentViewers") : null;
                    if(langId == null) {
                        langId = snippet.has("defaultAudioLanguage") ? snippet.get("defaultAudioLanguage") : null;
                    }
                    final Object language = langId;
                    final String streamerId = snippet.getString("channelId");
                    channelIds.append(streamerId).append(",");
                    final Livestream livestream = new Livestream(new HashMap<>() {{
                        put(StreamStatistic.START_TIME, details.getString("actualStartTime"));
                        put(StreamStatistic.VIDEO_ID, id);
                        put(StreamStatistic.VIDEO_TITLE, snippet.getString("title"));
                        //put(Statistic.VIDEO_DESCRIPTION, snippet.getString("description"));
                        put(StreamStatistic.VIDEO_LANGUAGE, language instanceof String ? (String) language : "");
                        put(StreamStatistic.VIDEO_THUMBNAIL, snippet.getJSONObject("thumbnails").getJSONObject("medium").getString("url"));
                        put(StreamStatistic.VIDEO_GAME_ID, snippet.getString("categoryId"));
                        put(StreamStatistic.STREAMER_ID, streamerId);
                        put(StreamStatistic.STREAMER_NAME, snippet.getString("channelTitle"));
                        put(StreamStatistic.VIEWERS_CURRENT, viewers instanceof String ? (String) viewers : "-1");
                        put(StreamStatistic.STREAMER_PROFILE_IMG, YOUTUBE_STREAMS.containsKey(id) ? YOUTUBE_STREAMS.get(id).get(StreamStatistic.STREAMER_PROFILE_IMG) : "");
                    }});
                    YOUTUBE_STREAMS.put(id, livestream);
                    streams.put(streamerId, livestream);
                }
                Cache.INSTANCE.setStreamsJSON(Platform.YOUTUBE, YOUTUBE_STREAMS.values());
                handler.handle(youtubeVideoJson);
                handler.handleThree(youtubeVideoJson, streams, channelIds);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tryRequesting(String url, HashMap<String, String> query, CompletionHandler handler) {
        requestJSON(url, RequestMethod.GET, HEADERS, query, json -> {
            final JSONArray array = getYouTubeItems(json.toString());
            YOUTUBE_STREAMS = new LinkedHashMap<>();
            final String ids = getYouTubeVideoIdsString(array, true);
            query.remove("maxResults");
            query.remove("order");
            query.remove("eventType");
            query.remove("type");
            query.put("part", "snippet,liveStreamingDetails");
            query.put("id", ids);

            final CompletionHandler completionHandler = new CompletionHandler() {
                @Override
                public void handle(Object object) {
                }

                @Override
                public void handleThree(Object youtubeVideoJson, Object object2, Object object3) {
                    final HashMap<Integer, Livestream> streams = (HashMap<Integer, Livestream>) object2;
                    final StringBuilder channelIds = (StringBuilder) object3;
                    query.put("part", "snippet");
                    query.put("id", channelIds.toString());
                    requestJSON("https://www.googleapis.com/youtube/v3/channels", RequestMethod.GET, HEADERS, query, channelJson -> {
                        final JSONObject channelJSON = new JSONObject((String) channelJson);
                        final JSONArray channelArray = channelJSON.getJSONArray("items");
                        for(Object obj : channelArray) {
                            final JSONObject channel = (JSONObject) obj, streamerSnippet = channel.getJSONObject("snippet"), thumbnails = streamerSnippet.getJSONObject("thumbnails");
                            final String id = channel.getString("id");
                            final Livestream stream = streams.get(id);
                            stream.setValue(StreamStatistic.STREAMER_ID, id);
                            stream.setValue(StreamStatistic.STREAMER_NAME, streamerSnippet.getString("title"));
                            //stream.setValue(Statistic.STREAMER_DESCRIPTION, streamerSnippet.getString("description"));
                            stream.setValue(StreamStatistic.STREAMER_PROFILE_IMG, thumbnails != null && thumbnails.has("medium") ? thumbnails.getJSONObject("medium").getString("url") : "");
                            YOUTUBE_STREAMS.put(stream.get(StreamStatistic.VIDEO_ID), stream);
                        }
                        Cache.INSTANCE.setStreamsJSON(Platform.YOUTUBE, YOUTUBE_STREAMS.values());
                        handler.handle(youtubeVideoJson);
                    });
                }
            };
            updateExistingStreams(query, completionHandler);
        });
    }

    private JSONArray getYouTubeItems(@NotNull Object object) {
        final JSONObject obj = new JSONObject(object.toString());
        return obj.getJSONArray("items");
    }
    private List<String> getYouTubeVideoIds(@NotNull JSONArray array) {
        final List<String> ids = new ArrayList<>();
        for(Object video : array) {
            final JSONObject json = (JSONObject) video;
            ids.add(json.getJSONObject("id").getString("videoId"));
        }
        return ids;
    }
    private String getYouTubeVideoIdsString(@NotNull JSONArray array, boolean update) {
        String ids = getYouTubeVideoIds(array).toString();
        ids = ids.substring(1, ids.length()-1).replaceAll("\\p{Z}", "");
        if(update) {
            CURRENT_YT_IDS = ids;
        }
        return ids;
    }
}
