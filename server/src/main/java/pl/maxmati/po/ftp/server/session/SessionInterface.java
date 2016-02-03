package pl.maxmati.po.ftp.server.session;

import pl.maxmati.po.ftp.common.Response;
import pl.maxmati.po.ftp.server.network.PassiveConnectionInterface;

/**
 * Created by maxmati on 2/3/16
 */
public interface SessionInterface extends Runnable {
    @Override
    void run();

    void dataSent(boolean success);

    void quit();

    void listenForPassiveConnection();

    void fetchUser(String username);

    void validatePassword(String password);

    void sendResponse(Response.Type type, Object... params);

    void sendResponse(Response response);

    boolean havePassiveConnection();

    PassiveConnectionInterface getPassiveConnection();

    boolean isAuthenticated();

    void abortTransfer();
}
