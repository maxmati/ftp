package pl.maxmati.po.ftp.client.network;

import pl.maxmati.po.ftp.client.events.CommandEvent;
import pl.maxmati.po.ftp.client.events.EventDispatcher;
import pl.maxmati.po.ftp.common.Watchdog;
import pl.maxmati.po.ftp.common.command.Command;

/**
 * Created by maxmati on 1/19/16
 */
public class ConnectionKeeper {
    private final Watchdog watchdog;

    public ConnectionKeeper(EventDispatcher dispatcher) {

        this.watchdog = new Watchdog(
                50 * 1000,
                () -> dispatcher.dispatch(new CommandEvent(CommandEvent.Type.REQUEST, new Command(Command.Type.NOOP))),
                true
        );

        dispatcher.registerListener(CommandEvent.class, event -> {
            if( ((CommandEvent)event).getType() == CommandEvent.Type.PERFORMED )
                watchdog.reset();
        });
    }

    public void stop() {
        watchdog.stop();
    }
}
