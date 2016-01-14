package pl.maxmati.po.ftp.client.events;

/**
 * Created by maxmati on 1/14/16
 */
public class ConnectEvent implements Event{

    private final Type type;
    private final String hostname;
    private final Integer port;

    public ConnectEvent(Type type, String hostname, Integer port) {
        this.type = type;
        this.hostname = hostname;
        this.port = port;
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

    public enum Type {
        CONNECTED, REQUEST_CONNECTION
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
