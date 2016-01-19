package pl.maxmati.po.ftp.client.network;

import pl.maxmati.po.ftp.client.events.ConnectEvent;
import pl.maxmati.po.ftp.client.events.Event;
import pl.maxmati.po.ftp.client.events.EventDispatcher;
import pl.maxmati.po.ftp.client.session.ClientSession;

import java.util.concurrent.ExecutorService;

/**
 * Created by maxmati on 1/14/16
 */
public class ConnectionManager {
    final private EventDispatcher dispatcher;
    final private ExecutorService executorService;
    final private ClientSession session;
    private ConnectionKeeper connectionKeeper = null;

    public ConnectionManager(EventDispatcher dispatcher, ExecutorService executorService) {
        this.dispatcher = dispatcher;
        this.executorService = executorService;
        session = new ClientSession(executorService, dispatcher);

        dispatcher.registerListener(ConnectEvent.class, this::onConnectionEvent);
    }

    private void onConnectionEvent(Event event) {
        ConnectEvent connectEvent = (ConnectEvent) event;
        switch (connectEvent.getType()){
            case REQUEST_CONNECTION:
                final Integer port = connectEvent.getPort();
                final String hostname = connectEvent.getHostname();
                final String username = connectEvent.getUsername();
                final String password = connectEvent.getPassword();
                if(!session.connect(hostname, port, username, password))
                    dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.ERROR_UNABLE_CONNECT));

                connectionKeeper = new ConnectionKeeper(dispatcher);
                break;
            case REQUEST_DISCONNECT:
            case ERROR_BAD_PASS:
                session.disconnect();
                if(connectionKeeper != null){
                    connectionKeeper.stop();
                    connectionKeeper = null;
                }
                break;
        }
    }

    public ClientSession getSession() {
        return session;
    }
}
