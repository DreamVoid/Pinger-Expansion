package com.extendedclip.papi.expansion.pinger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class PingerExpansion extends PlaceholderExpansion implements Cacheable, Taskable, Configurable {
    private BukkitTask pingTask = null;
    private String online = "&aOnline";
    private String offline = "&cOffline";
    private final Map<String, Pinger> servers = new ConcurrentHashMap<>();
    private final Map<String, InetSocketAddress> toPing = new ConcurrentHashMap<>();
    private int interval = 60;

    public Map<String, Object> getDefaults() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("check_interval", 30);
        defaults.put("online", "&aOnline");
        defaults.put("offline", "&cOffline");
        return defaults;
    }

    public void start() {
        this.online = getString("online", "&aOnline");
        this.offline = getString("offline", "&cOffline");
        int time = getInt("check_interval", 60);
        if (time > 0) {
            this.interval = time;
        }
        this.pingTask = new BukkitRunnable() {
            public void run() {
                if (!PingerExpansion.this.toPing.isEmpty()) {
                    for (Map.Entry<String, InetSocketAddress> address : PingerExpansion.this.toPing.entrySet()) {
                        try {
                            Pinger r = new Pinger(address.getValue().getHostName(), address.getValue().getPort());
                            if (r.fetchData()) {
                                PingerExpansion.this.servers.put(address.getKey(), r);
                            } else if (PingerExpansion.this.servers.containsKey(address.getKey())) {
                                PingerExpansion.this.servers.remove(address.getKey());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(getPlaceholderAPI(), 20L, 20L * this.interval);
    }

    public void stop() {
        try {
            this.pingTask.cancel();
        } catch (Exception ignored) {
        }
        this.pingTask = null;
    }

    public void clear() {
        this.servers.clear();
        this.toPing.clear();
    }

    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "clip";
    }

    @Override
    public String getIdentifier() {
        return "pinger";
    }

    @Override
    public String getVersion() {
        return "1.0.1";
    }

    public String onPlaceholderRequest(Player p, String identifier) {
        int place = identifier.indexOf("_");
        if (place == -1) {
            return null;
        }
        String type = identifier.substring(0, place);
        String address = identifier.substring(place + 1);
        Pinger r = null;
        Iterator<String> it = this.servers.keySet().iterator();
        while (true) {
            if (it.hasNext()) {
                String a = it.next();
                if (a.equalsIgnoreCase(address)) {
                    r = this.servers.get(a);
                    break;
                }
            } else {
                break;
            }
        }
        if (r == null && !this.toPing.containsKey(address)) {
            int port = 25565;
            String add = address;
            if (address.contains(":")) {
                add = address.substring(0, address.indexOf(":"));
                try {
                    port = Integer.parseInt(address.substring(address.indexOf(":") + 1));
                } catch (Exception ignored) {}
            }
            this.toPing.put(address, new InetSocketAddress(add, port));
        }
        if (type.equalsIgnoreCase("motd")) {
            return r != null ? r.getMotd() : "";
        } else if (type.equalsIgnoreCase("count") || type.equalsIgnoreCase("players")) {
            return r != null ? String.valueOf(r.getPlayersOnline()) : "0";
        } else if (type.equalsIgnoreCase("max") || type.equalsIgnoreCase("maxplayers")) {
            return r != null ? String.valueOf(r.getMaxPlayers()) : "0";
        } else if (type.equalsIgnoreCase("pingversion") || type.equalsIgnoreCase("pingv")) {
            return r != null ? String.valueOf(r.getPingVersion()) : "-1";
        } else if (type.equalsIgnoreCase("gameversion") || type.equalsIgnoreCase("version")) {
            return (r == null || r.getGameVersion() == null) ? "离线" : String.valueOf(r.getGameVersion());
        } else if (!type.equalsIgnoreCase("online") && !type.equalsIgnoreCase("isonline")) {
            return null;
        } else {
            return r != null ? this.online : this.offline;
        }
    }

    public static final class Pinger {
        private String gameVersion;
        private String motd;
        private String address = "localhost";
        private int port = 25565;
        private int timeout = 2000;
        private int pingVersion = -1;
        private int protocolVersion = -1;
        private int playersOnline = -1;
        private int maxPlayers = -1;

        public Pinger(String address, int port) {
            setAddress(address);
            setPort(port);
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getAddress() {
            return this.address;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getPort() {
            return this.port;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getTimeout() {
            return this.timeout;
        }

        private void setPingVersion(int pingVersion) {
            this.pingVersion = pingVersion;
        }

        public int getPingVersion() {
            return this.pingVersion;
        }

        private void setProtocolVersion(int protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        public int getProtocolVersion() {
            return this.protocolVersion;
        }

        private void setGameVersion(String gameVersion) {
            this.gameVersion = gameVersion;
        }

        public String getGameVersion() {
            return this.gameVersion;
        }

        private void setMotd(String motd) {
            this.motd = motd;
        }

        public String getMotd() {
            return this.motd;
        }

        private void setPlayersOnline(int playersOnline) {
            this.playersOnline = playersOnline;
        }

        public int getPlayersOnline() {
            return this.playersOnline;
        }

        private void setMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
        }

        public int getMaxPlayers() {
            return this.maxPlayers;
        }

        public boolean fetchData() {
            try {
                Socket socket = new Socket();
                socket.setSoTimeout(this.timeout);
                socket.connect(new InetSocketAddress(getAddress(), getPort()), getTimeout());
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                InputStream inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-16BE"));
                dataOutputStream.write(new byte[]{-2, 1});
                int packetId = inputStream.read();
                if (packetId == -1) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                    return false;
                } else if (packetId != 255) {
                    try {
                        socket.close();
                    } catch (IOException e2) {
                    }
                    return false;
                } else {
                    int length = inputStreamReader.read();
                    if (length == -1) {
                        try {
                            socket.close();
                        } catch (IOException e3) {
                        }
                        return false;
                    } else if (length == 0) {
                        try {
                            socket.close();
                        } catch (IOException e4) {
                        }
                        return false;
                    } else {
                        char[] chars = new char[length];
                        if (inputStreamReader.read(chars, 0, length) != length) {
                            try {
                                socket.close();
                            } catch (IOException e5) {
                            }
                            return false;
                        }
                        String string = new String(chars);
                        if (string.startsWith("§")) {
                            String[] data = string.split("\000");
                            setPingVersion(Integer.parseInt(data[0].substring(1)));
                            setProtocolVersion(Integer.parseInt(data[1]));
                            setGameVersion(data[2]);
                            setMotd(data[3]);
                            setPlayersOnline(Integer.parseInt(data[4]));
                            setMaxPlayers(Integer.parseInt(data[5]));
                        } else {
                            String[] data2 = string.split("§");
                            setMotd(data2[0]);
                            setPlayersOnline(Integer.parseInt(data2[1]));
                            setMaxPlayers(Integer.parseInt(data2[2]));
                        }
                        dataOutputStream.close();
                        outputStream.close();
                        inputStreamReader.close();
                        inputStream.close();
                        socket.close();
                        return true;
                    }
                }
            } catch (IOException e7) {
                return false;
            }
        }
    }
}