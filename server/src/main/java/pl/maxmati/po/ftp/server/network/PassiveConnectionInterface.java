package pl.maxmati.po.ftp.server.network;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by maxmati on 2/3/16
 */
public interface PassiveConnectionInterface {
    void sendData(String data);

    void receiveData(OutputStream out);

    void sendData(InputStream in);

    int getPort();

    boolean abort();
}
