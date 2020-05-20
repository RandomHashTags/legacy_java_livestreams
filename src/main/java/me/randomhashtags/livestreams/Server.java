package me.randomhashtags.livestreams;

import me.randomhashtags.livestreams.mixer.Mixer;
import me.randomhashtags.livestreams.twitch.Twitch;
import me.randomhashtags.livestreams.youtube.YouTube;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.security.KeyStore;

import static java.lang.System.out;

public class Server {
    protected static final Server INSTANCE = new Server();

    public static void main(String[] args) {
        INSTANCE.boot();
    }

    private void boot() {
        Mixer.INSTANCE.boot();
        Twitch.INSTANCE.boot();
        YouTube.INSTANCE.boot();
        setupServer(false);
    }
    private void setupServer(boolean https) {
        if(https) {
            setupHttpsServer();
        } else {
            setupHttpServer();
        }
    }

    private void connectClients(ServerSocket server, boolean https) throws Exception {
        out.println("\nLivestreams Proxy - Listening for clients on port " + DataValues.LIVESTREAMS_PORT + "...");
        while (true) {
            new Client(server.accept()).start();
        }
    }

    private void setupHttpServer() {
        try {
            connectClients(new ServerSocket(DataValues.LIVESTREAMS_PORT), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupHttpsServer() {
        try {
            final SSLContext context = getHttpsContext();
            final SSLServerSocketFactory socketFactory = context.getServerSocketFactory();
            final SSLServerSocket server = (SSLServerSocket) socketFactory.createServerSocket(DataValues.LIVESTREAMS_PORT);
            connectClients(server, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private SSLContext getHttpsContext() {
        try {
            final KeyStore store = KeyStore.getInstance("JKS");
            final char[] password = DataValues.HTTPS_KEYSTORE_PASSWORD.toCharArray();
            final String factoryType = "SunX509";
            store.load(new FileInputStream("livestreams.keystore"), password);

            final KeyManagerFactory factory = KeyManagerFactory.getInstance(factoryType);
            factory.init(store, password);
            final KeyManager[] keyManagers = factory.getKeyManagers();

            final TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(factoryType);
            trustFactory.init(store);
            final TrustManager[] trustManagers = trustFactory.getTrustManagers();

            final SSLContext context = SSLContext.getInstance("SSL");
            context.init(keyManagers, trustManagers, null);
            return context;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
