package pl.maxmati.po.ftp.client.events;

import pl.maxmati.ftp.common.command.Command;

/**
 * Created by maxmati on 1/15/16
 */
public class CommandEvent implements Event{
    private Command command;

    public CommandEvent(Command command) {
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "CommandEvent{" +
                "command=" + command +
                '}';
    }
}
