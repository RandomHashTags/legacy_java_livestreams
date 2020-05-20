package me.randomhashtags.livestreams.twitch;

import me.randomhashtags.livestreams.Cache;
import me.randomhashtags.livestreams.Platform;
import me.randomhashtags.livestreams.util.CompletionHandler;
import me.randomhashtags.livestreams.DataValues;
import me.randomhashtags.livestreams.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static java.lang.System.out;

public enum Twitch implements LocalServer, DataValues, RestAPI {
    INSTANCE;

    private String pagination, accessToken;
    private HashMap<String, String> HEADERS;
    private LinkedHashMap<String, Livestream> TWITCH_STREAMS;
    private HashMap<String, String> TWITCH_PROFILE_IMAGES;
    private HashMap<String, String> TWITCH_GAMES;
    private HashMap<String, List<Clip>> TWITCH_CLIPS;
    private List<String> UNKNOWN_USER_IMAGE_IDS;
    private List<String> UNKNOWN_GAME_IDS;

    public void boot() {
        HEADERS = new HashMap<>() {{
            putAll(CONTENT_HEADERS);
            put("Client-ID", TWITCH_CLIENT_ID);
        }};

        TWITCH_STREAMS = new LinkedHashMap<>();
        TWITCH_PROFILE_IMAGES = new HashMap<>();
        TWITCH_GAMES = new HashMap<>();
        TWITCH_CLIPS = new HashMap<>();
        UNKNOWN_USER_IMAGE_IDS = new ArrayList<>();
        UNKNOWN_GAME_IDS = new ArrayList<>();
        requestAuthorizationToken();
        start("Twitch", 1.00f, 0.00f, object -> {
        });
    }

    private void requestAuthorizationToken() {
        if(true) {
            accessToken = TWITCH_ACCESS_TOKEN;
            updateHeaders();
        } else {
            requestNewAuthorizationToken(object -> {
                updateHeaders();
            });
        }
    }
    private void updateHeaders() {
        HEADERS.put("Authorization", "Bearer " + accessToken);
    }
    private void requestNewAuthorizationToken(CompletionHandler handler) {
        final HashMap<String, String> query = new HashMap<>();
        query.put("client_id", TWITCH_CLIENT_ID);
        query.put("client_secret", TWITCH_CLIENT_SECRET);
        query.put("grant_type", "client_credentials");
        requestJSON("https://id.twitch.tv/oauth2/token", RequestMethod.POST, null, query, object -> {
            final JSONObject json = new JSONObject(object.toString());
            accessToken = json.getString("access_token");

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    requestAuthorizationToken();
                }
            }, json.getLong("expires_in")*1000);
            handler.handle(null);
        });
    }

    public void requestNewStreams(CompletionHandler handler) {
        new Thread(() -> requestNewTwitchStreams(handler)).start();
    }
    public void updateExistingStreams(CompletionHandler handler) {
        requestNewStreams(handler);
    }

    private void requestNewTwitchStreams(CompletionHandler completionHandler) {
        try {
            requestTwitchStreams(true, false, object1 -> {
                requestTwitchStreams(false, true, object2 -> {
                    requestTwitchStreams(false, true, object3 -> {
                        requestTwitchStreams(false, true, object4 -> {
                            requestTwitchStreams(false, true, object5 -> {
                                requestTwitchStreams(false, true, object6 -> {
                                    requestTwitchStreams(false, true, object7 -> {
                                        requestTwitchStreams(false, true, object8 -> {
                                            requestTwitchStreams(false, true, object9 -> {
                                                requestTwitchStreams(false, true, true, completionHandler);
                                                Cache.INSTANCE.setStreamsJSON(Platform.TWITCH, TWITCH_STREAMS.values());
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

    private void requestTwitchStreams(boolean clearMap, boolean hasPagination, CompletionHandler handler) {
        requestTwitchStreams(clearMap, hasPagination, false, handler);
    }
    private void requestTwitchStreams(boolean clearMap, boolean hasPagination, boolean override, CompletionHandler handler) {
        final HashMap<String, String> query = new HashMap<>();
        query.put("first", Integer.toString(TWITCH_REQUEST_LIMIT));
        if(hasPagination) {
            query.put("after", pagination);
        }
        tryRequesting("https://api.twitch.tv/helix/streams", query, clearMap, override, handler);
    }

    private void tryRequesting(String url, HashMap<String, String> query, boolean clearMap, boolean override, CompletionHandler handler) {
        requestJSON(url, RequestMethod.GET, HEADERS, query, responseJson -> {
            if(clearMap) {
                TWITCH_STREAMS.clear();
            }
            final JSONObject json = new JSONObject(responseJson.toString());
            pagination = json.getJSONObject("pagination").getString("cursor");
            for(Object streamObject : json.getJSONArray("data")) {
                final JSONObject stream = new JSONObject(streamObject.toString());
                final String gameid = stream.getString("game_id"), userId = stream.getString("user_id");
                final String startedAt = stream.getString("started_at");
                final Livestream livestream = new Livestream(new HashMap<>() {{
                    put(StreamStatistic.START_TIME, startedAt);
                    put(StreamStatistic.VIDEO_ID, stream.getString("id"));
                    put(StreamStatistic.VIDEO_TITLE, stream.getString("title"));
                    put(StreamStatistic.VIDEO_LANGUAGE, stream.getString("language"));
                    put(StreamStatistic.VIDEO_THUMBNAIL, stream.getString("thumbnail_url"));
                    put(StreamStatistic.VIDEO_GAME_ID, gameid);
                    put(StreamStatistic.STREAMER_ID, userId);
                    put(StreamStatistic.STREAMER_NAME, stream.getString("user_name"));
                    //put(Statistic.STREAMER_DESCRIPTION, stream.getString("description"));
                    put(StreamStatistic.VIEWERS_CURRENT, Integer.toString(stream.getInt("viewer_count")));
                }});
                if(TWITCH_PROFILE_IMAGES.containsKey(userId)) {
                    livestream.setValue(StreamStatistic.STREAMER_PROFILE_IMG, TWITCH_PROFILE_IMAGES.get(userId));
                } else if(!UNKNOWN_USER_IMAGE_IDS.contains(userId)) {
                    UNKNOWN_USER_IMAGE_IDS.add(userId);
                }
                TWITCH_STREAMS.put(userId, livestream);
                if(!TWITCH_GAMES.containsKey(gameid)) {
                    TWITCH_GAMES.put(gameid, "null");
                    if(gameid != null && !gameid.isEmpty()) {
                        UNKNOWN_GAME_IDS.add(gameid);
                    }
                } else if(!TWITCH_GAMES.get(gameid).equals("null")) {
                    final String[] values = TWITCH_GAMES.get(gameid).split("\\|\\|");
                    livestream.setValue(StreamStatistic.VIDEO_GAME_NAME, values[0]);
                    livestream.setValue(StreamStatistic.VIDEO_GAME_THUMBNAIL, values[1]);
                }
            }
            if(override || UNKNOWN_USER_IMAGE_IDS.size() >= 100) {
                updateStreamProfileImages();
            }
            if(override || UNKNOWN_GAME_IDS.size() >= 100) {
                updateGameIds();
            }
            handler.handle(responseJson);
        });
    }

    private void updateGameIds() {
        final int size = UNKNOWN_GAME_IDS.size();
        if(size > 0) {
            final StringBuilder gameIds = new StringBuilder();
            final List<String> copy = new ArrayList<>(UNKNOWN_GAME_IDS).subList(0, Math.min(100, size));
            final String first = copy.get(0);
            for(String gameId : copy) {
                gameIds.append(gameId.equals(first) ? "" : "&id=").append(gameId);
            }
            updateExistingStreamGames(gameIds.toString());
            UNKNOWN_GAME_IDS.removeAll(copy);
        }
    }

    @Override
    public void requestNewClips(CompletionHandler handler) {
        new Thread(this::updateTwitchTopClips).start();
    }
    private void updateTwitchTopClips() {
        // https://dev.twitch.tv/docs/v5/reference/clips#get-top-clips
        final long started = System.currentTimeMillis();
        final HashMap<String, String> query = new HashMap<>();
        final HashMap<String, String> headers = new HashMap<>() {{
            putAll(HEADERS);
            put("Accept", "application/vnd.twitchtv.v5+json");
        }};
        query.put("limit", Integer.toString(TWITCH_REQUEST_LIMIT));
        query.put("trending", "false");
        for(int i = 1; i <= 4; i++) {
            final String type = i == 1 ? "all" : i == 2 ? "month" : i == 3 ? "week" : i == 4 ? "day" : "";
            final HashMap<String, String> realQuery = new HashMap<>(query);
            realQuery.put("period", type);
            try {
                requestJSON("https://api.twitch.tv/kraken/clips/top", RequestMethod.GET, headers, realQuery, json -> {
                    final JSONArray array = new JSONObject(json.toString()).getJSONArray("clips");
                    final List<Clip> clips = new ArrayList<>();
                    for(Object obj : array) {
                        final JSONObject clipJson = new JSONObject(obj.toString());
                        final JSONObject creator = clipJson.getJSONObject("curator"), streamer = clipJson.getJSONObject("broadcaster");
                        final HashMap<ClipStatistic, String> values = new HashMap<>() {{
                            put(ClipStatistic.CLIP_SLUG, clipJson.getString("slug"));
                            put(ClipStatistic.CLIP_TITLE, clipJson.getString("title"));
                            put(ClipStatistic.CLIP_GAME, clipJson.getString("game"));
                            put(ClipStatistic.CLIP_LANGUAGE, clipJson.getString("language"));
                            put(ClipStatistic.CLIP_VIEW_COUNT, Integer.toString(clipJson.getInt("views")));
                            put(ClipStatistic.CLIP_DURATION, Double.toString(clipJson.getDouble("duration")));
                            put(ClipStatistic.CLIP_CREATED_AT, clipJson.getString("created_at"));
                            put(ClipStatistic.CLIP_URL, clipJson.getString("url"));
                            put(ClipStatistic.CLIP_EMBED_URL, clipJson.getString("embed_url"));
                            put(ClipStatistic.CLIP_EMBED_HTML, clipJson.getString("embed_html"));
                            put(ClipStatistic.CLIP_THUMBNAIL_MEDIUM, clipJson.getJSONObject("thumbnails").getString("medium"));
                            put(ClipStatistic.CREATOR_ID, creator.getString("id"));
                            put(ClipStatistic.CREATOR_NAME, creator.getString("display_name"));
                            put(ClipStatistic.CREATOR_PROFILE_IMG, creator.getString("logo"));
                            put(ClipStatistic.STREAMER_ID, streamer.getString("id"));
                            put(ClipStatistic.STREAMER_NAME, streamer.getString("display_name"));
                            put(ClipStatistic.STREAMER_PROFILE_IMG, streamer.getString("logo"));
                        }};
                        final Clip clip = new Clip(values);
                        clips.add(clip);
                    }
                    TWITCH_CLIPS.put(type, clips);
                });
            } catch (Exception e) {
                out.println("[ERROR] " + getDate() + " - Twitch - Failed to update Top \"" + type + "\" clips!");
            }
        }
        updateTopClipsJSON();
        out.println(getDate() + " - Twitch - Successfully updated Top Clips (took " + (System.currentTimeMillis()-started) + "ms)");
    }
    private void updateTopClipsJSON() {
        Cache.INSTANCE.setTwitchClipsJSON(TWITCH_CLIPS);
    }

    private void updateExistingStreamGames() {
        for(Livestream stream : TWITCH_STREAMS.values()) {
            final String id = stream.get(StreamStatistic.VIDEO_GAME_ID);
            if(id != null && !id.isEmpty()) {
                final String game = TWITCH_GAMES.get(id);
                if(!game.equals("null")) {
                    final String[] values = game.split("\\|\\|");
                    stream.setValue(StreamStatistic.VIDEO_GAME_NAME, values[0]);
                    stream.setValue(StreamStatistic.VIDEO_GAME_THUMBNAIL, values[1]);
                }
            }
        }
    }
    private void updateExistingStreamGames(String gameString) {
        final long started = System.currentTimeMillis();
        final HashMap<String, String> query = new HashMap<>();
        query.put("id", gameString);
        try {
            requestJSON("https://api.twitch.tv/helix/games", RequestMethod.GET, HEADERS, query, object -> {
                final JSONArray response = new JSONObject(object.toString()).getJSONArray("data");
                if(!response.isEmpty()) {
                    for(Object obj : response) {
                        final JSONObject gameJSON = (JSONObject) obj;
                        TWITCH_GAMES.put(gameJSON.getString("id"), gameJSON.getString("name") + "||" + gameJSON.getString("box_art_url"));
                    }
                    updateExistingStreamGames();
                }
                out.println(getDate() + " - Twitch - Successfully requested " + response.length() + " Games (took " + (System.currentTimeMillis()-started) + "ms)");
            });
        } catch (Exception e) {
            out.println("[ERROR] " + getDate() + "- Twitch - Unable to find game using ids \"" + gameString + "\"! (took " + (System.currentTimeMillis()-started) + "ms)");
        }
    }
    private void updateStreamProfileImages() {
        final long started = System.currentTimeMillis();
        final int size = UNKNOWN_USER_IMAGE_IDS.size();
        if(size > 0) {
            final HashMap<String, String> query = new HashMap<>();
            final List<String> copy = new ArrayList<>(UNKNOWN_USER_IMAGE_IDS).subList(0, Math.min(100, size));
            final String first = copy.get(0);
            final StringBuilder builder = new StringBuilder();
            for(String id : copy) {
                builder.append(id.equals(first) ? "" : "&id=").append(id);
            }
            UNKNOWN_USER_IMAGE_IDS.removeAll(copy);
            query.put("id", builder.toString());
            requestJSON("https://api.twitch.tv/helix/users", RequestMethod.GET, HEADERS, query, json -> {
                if(json != null) {
                    final JSONArray array = new JSONObject(json.toString()).getJSONArray("data");
                    for(Object object : array) {
                        final JSONObject stream = (JSONObject) object;
                        final String id = stream.getString("id"), url = stream.getString("profile_image_url");
                        if(TWITCH_STREAMS.containsKey(id)) {
                            TWITCH_STREAMS.get(id).setValue(StreamStatistic.STREAMER_PROFILE_IMG, url);
                        } else {
                            out.println(getDate() + " - ERROR - Twitch - TWITCH_STREAMS doesn't contain stream with id \"" + id + "\", aka \"" + stream.getString("display_name") + "\"!");
                        }
                        TWITCH_PROFILE_IMAGES.put(id, url);
                    }
                    out.println(getDate() + " - Twitch - Successfully updated " + array.length() + " Stream Profile Images (took " + (System.currentTimeMillis()-started) + "ms)");
                }
            });
        }
    }
}
