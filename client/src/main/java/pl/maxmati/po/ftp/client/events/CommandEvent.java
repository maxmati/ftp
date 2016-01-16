package pl.maxmati.po.ftp.client.events;

import pl.maxmati.ftp.common.command.Command;

/**
 * Created by maxmati on 1/15/16
 */
public class CommandEvent implements Event{
    private final Type type;
    private final Command command;

    public CommandEvent(Type type, Command command) {
        this.type = type;
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "CommandEvent{" +
                "type=" + type +
                ", command=" + command +
                '}';
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        REQUEST, PERFORMED
    }
}
