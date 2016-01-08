package pl.maxmati.po.ftp.server;

import pl.maxmati.po.ftp.server.session.SessionManager;

/**
 * Created by maxmati on 1/6/16
 */
public class Main {
    public static void main(String[] args){
        System.out.println("Starting ftp server");
        SessionManager manager = new SessionManager();
        manager.start();
    }
}
