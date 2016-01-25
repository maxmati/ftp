package pl.maxmati.po.ftp.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by maxmati on 1/19/16
 */
public class Config implements Serializable {
    private static final long serialVersionUID = 3579879431544726067L;

    private static Config ourInstance = new Config();
    private int ip1 = 127;
    private int ip2 = 0;
    private int ip3 = 0;
    private int ip4 = 1;
    private String DBPass = "aarmzEjvaRYFstCE";
    private String DBUser = "poftp";
    private String DBUrl = "jdbc:mysql://mysql.maxmati.pl/poftp";

    public static Config getInstance() {
        return ourInstance;
    }

    private Config() {
    }

    private transient Path configFile = Paths.get("FTPConfig.conf");

    public String getDBUrl() {
        return DBUrl;
    }

    public String getDBUser() {
        return DBUser;
    }

    public String getDBPass() {
        return DBPass;
    }

    public void setDBPass(String DBPass) {
        this.DBPass = DBPass;
    }

    public void setDBUser(String DBUser) {
        this.DBUser = DBUser;
    }

    public void setDBUrl(String DBUrl) {
        this.DBUrl = DBUrl;
    }

    public int getIP1() {
        return ip1;
    }

    public int getIP2() {
        return ip2;
    }

    public int getIP3() {
        return ip3;
    }

    public int getIP4() {
        return ip4;
    }

    public void setIp1(int ip1) {
        this.ip1 = ip1;
    }

    public void setIp2(int ip2) {
        this.ip2 = ip2;
    }

    public void setIp3(int ip3) {
        this.ip3 = ip3;
    }

    public void setIp4(int ip4) {
        this.ip4 = ip4;
    }

    public void store() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(configFile));
        out.writeObject(this);
        out.close();
    }

    public static void load(Path path) {
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(Files.newInputStream(path));
            ourInstance = (Config) in.readObject();
            ourInstance.configFile = path;
        } catch (IOException | ClassNotFoundException e) {
            ourInstance = new Config();
        }

    }

    public String getBaseCWD() {
        return "/";
    }
}
