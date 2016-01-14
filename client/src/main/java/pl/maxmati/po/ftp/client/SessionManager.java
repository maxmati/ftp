package pl.maxmati.po.ftp.client;

import pl.maxmati.po.ftp.client.events.ConnectEvent;
import pl.maxmati.po.ftp.client.events.Event;
import pl.maxmati.po.ftp.client.events.EventDispatcher;

import java.util.concurrent.ExecutorService;

/**
 * Created by maxmati on 1/14/16
 */
public class SessionManager {
    final private EventDispatcher dispatcher;
    final private ExecutorService executorService;
    final private ClientSession session;

    public SessionManager(EventDispatcher dispatcher, ExecutorService executorService) {
        this.dispatcher = dispatcher;
        this.executorService = executorService;
        session = new ClientSession(executorService);

        dispatcher.registerListener(ConnectEvent.class, this::onConnectionEvent);
    }

    private void onConnectionEvent(Event event) {
        ConnectEvent connectEvent = (ConnectEvent) event;
        switch (connectEvent.getType()){
            case REQUEST_CONNECTION:
                final Integer port = connectEvent.getPort();
                final String hostname = connectEvent.getHostname();
                session.connect(hostname, port);
                dispatcher.dispatch(new ConnectEvent(ConnectEvent.Type.CONNECTED, hostname, port));
                break;
        }
    }

}
