package pl.maxmati.po.ftp.server;

/**
 * Created by maxmati on 1/19/16
 */
public class Config {
    private static Config ourInstance = new Config();

    public static Config getInstance() {
        return ourInstance;
    }

    private Config() {
    }

    public String getDBUrl() {
        return "jdbc:mysql://mysql.maxmati.pl/poftp";
    }

    public String getDBUser() {
        return "poftp";
    }

    public String getDBPass() {
        return "aarmzEjvaRYFstCE";
    }

    public int getIP1() {
        return 127;
    }

    public int getIP2() {
        return 0;
    }

    public int getIP3() {
        return 0;
    }

    public int getIP4() {
        return 1;
    }
}
