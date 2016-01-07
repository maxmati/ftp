package pl.maxmati.po.ftp.server.exceptions;

import java.sql.SQLException;

/**
 * Created by maxmati on 1/6/16
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(SQLException e) {
        super(e);
    }

    public DatabaseException(String s) {
        super(s);
    }
}
