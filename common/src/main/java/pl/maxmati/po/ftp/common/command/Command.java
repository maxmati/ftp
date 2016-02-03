package pl.maxmati.po.ftp.common.command;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by maxmati on 1/8/16
 */
public class Command {

    private final Type type;
    private final String[] params;

    public Command(Type type) {
        this.type = type;
        this.params = new String[0];
    }

    public Command(Type type, String... params) {
        this.type = type;
        this.params = params;
    }

    @Override
    public String toString() {
        return "Command{" +
                "type=" + type +
                ", params=" + Arrays.toString(params) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Command)) return false;
        Command command = (Command) o;
        return type == command.type &&
                Arrays.equals(params, command.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, params);
    }

    public Type getType() {
        return type;
    }

    public String getParam(int i) {
        return params[i];
    }

    public boolean hasValidNumberOfArgs() {
        return type.getNumberOfArgs() == params.length || type == Type.NONE;
    }

    public String toNetworkString() {
        return type.toString() + (params.length > 0 ? " " : "") + String.join(" ", params) + "\r\n";
    }

    public enum Type {
        USER(1), PASS(1), QUIT(0), NOOP(0), PASV(0),
        NLST(0), PWD(0), CWD(1), MKD(1), RMD(1), DELE(1),
        RETR(1), STOR(1), APPE(1), ABOR(0), CHMOD(2), NONE(0);

        private final int numberOfArgs;

        Type(int numberOfArgs) {
            this.numberOfArgs = numberOfArgs;
        }

        public int getNumberOfArgs() {
            return numberOfArgs;
        }
    }
}
