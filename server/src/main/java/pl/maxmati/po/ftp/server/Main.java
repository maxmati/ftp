package pl.maxmati.po.ftp.server;

import pl.maxmati.po.ftp.server.database.ConnectionPool;
import pl.maxmati.po.ftp.server.database.dao.FilesDAO;
import pl.maxmati.po.ftp.server.database.dao.GroupsDAO;
import pl.maxmati.po.ftp.server.database.dao.UsersDAO;
import pl.maxmati.po.ftp.server.session.SessionManager;

import java.nio.file.Paths;

/**
 * Created by maxmati on 1/6/16
 */
public class Main {
    public static void main(String[] args){
        System.out.println("Starting ftp server");
        Config.load(Paths.get("FTPConfig.conf"));
        ConnectionPool connectionPool = ConnectionPool.getInstance();
        UsersDAO usersDAO = new UsersDAO(connectionPool);
        SessionManager manager = new SessionManager(
                new UsersManager(usersDAO),
                new FilesDAO(
                        connectionPool,
                        usersDAO,
                        new GroupsDAO(connectionPool)
                        )
        );
        manager.start();
    }
}
