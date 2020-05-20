package me.randomhashtags.livestreams;

import me.randomhashtags.livestreams.util.NotNull;
import me.randomhashtags.livestreams.util.RestAPI;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;

import static java.lang.System.out;

public final class Client extends Thread implements RestAPI {

    private Socket client;
    private OutputStream outToClient;
    private String headers;

    Client(@NotNull Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            setupHeaders();
            sendResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendResponse() throws Exception {
        final long started = System.currentTimeMillis();
        outToClient = client.getOutputStream();

        final String[] headers = getHeaderList();
        final String ip = client.getInetAddress().toString(), platform = getPlatform(headers), identifier = getIdentifier(headers);
        final boolean isValidRequest = platform != null && identifier != null;
        final String prefix = "[" + platform + " CLIENT] - " + identifier + " - ";

        final String response;
        if(isValidRequest) {
            out.println(prefix + "VALID - hostname: \"" + ip + "\" (took " + (System.currentTimeMillis()-started) + "ms)");
            switch (getTarget()) {
                case "streams/all":
                    response = Cache.INSTANCE.getLivestreamsJSON();
                    break;
                case "streams/twitch":
                    response = Cache.INSTANCE.getStreamsJSON(Platform.TWITCH);
                    break;
                case "streams/mixer":
                    response = Cache.INSTANCE.getStreamsJSON(Platform.MIXER);
                    break;
                case "streams/youtube":
                    response = Cache.INSTANCE.getStreamsJSON(Platform.YOUTUBE);
                    break;

                case "clips/all":
                    response = Cache.INSTANCE.getClipsJSON();
                    break;
                default:
                    response = null;
                    break;
            }
        } else {
            response = null;
            out.println(prefix + "INVALID - Tried connecting using hostname: \"" + ip + "\"");
        }
        writeOutput(client, response != null ? DataValues.HTTP_SUCCESS_200 + response : DataValues.HTTP_ERROR_404);
    }
    private void writeOutput(Socket client, String input) {
        if(client.isOutputShutdown() || client.isClosed()) {
            return;
        }
        try {
            outToClient.write(input.getBytes(DataValues.ENCODING));
            closeClient(client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void closeClient(Socket client) {
        try {
            outToClient.close();
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getPlatform(@NotNull String[] headers) {
        for(String string : headers) {
            if(string.startsWith("Platform: ")) {
                return string.split("Platform: ")[1];
            }
        }
        return null;
    }
    private String getIdentifier(@NotNull String[] headers) {
        for(String string : headers) {
            if(string.startsWith("Identifier: ")) {
                return string.split("Identifier: ")[1];
            }
        }
        return null;
    }

    private String getTarget() {
        for(String string : getHeaderList()) {
            if(string.startsWith("GET ") && string.endsWith("HTTP/1.1")) {
                return string.split("GET ")[1].split(" HTTP/1\\.1")[0].replaceFirst("/", "");
            }
        }
        return "";
    }

    private String[] getHeaderList() {
        return headers.replaceAll("\r", "").split("\n");
    }
    private void setupHeaders() {
        if(headers == null) {
            try {
                final Reader reader = new InputStreamReader(client.getInputStream());
                String headers = "";
                try {
                    int c;
                    while ((c = reader.read()) != -1) {
                        headers += (char) c;
                        if(headers.contains("\r\n\r\n")) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.headers = headers;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
