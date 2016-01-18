package pl.maxmati.po.ftp.client.events;

/**
 * Created by maxmati on 1/14/16
 */
public class ConnectEvent implements Event{

    private final Type type;
    private final String hostname;
    private final Integer port;
    private final String password;
    private final String username;

    public ConnectEvent(Type type, String hostname, Integer port, String username, String password) {
        this.type = type;
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public ConnectEvent(Type type) {
        this(type, null, null, null, null);
    }

    public Type getType() {
        return type;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public enum Type {
        CONNECTED, REQUEST_DISCONNECT, DISCONNECTED, ERROR_BAD_PASS, ERROR_UNABLE_CONNECT, REQUEST_CONNECTION
    }

    @Override
    public String toString() {
        return "ConnectEvent{" +
                "type=" + type +
                ", hostname='" + hostname + '\'' +
                ", port=" + port +
                '}';
    }
}
