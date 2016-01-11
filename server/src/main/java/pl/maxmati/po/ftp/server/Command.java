package pl.maxmati.po.ftp.server;

import java.util.Arrays;

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

    public Command(Type type, String[] params) {
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

    public Type getType() {
        return type;
    }

    public String getParam(int i) {
        return params[i];
    }

    public boolean hasValidNumberOfArgs() {
        return type.getNumberOfArgs() == params.length || type == Type.NONE;
    }

    public enum Type {
        USER(1), PASS(1), QUIT(0), NOOP(0), PASV(0),
        NLST(0), PWD(0), CWD(1), MKD(1), RMD(1), DELE(1),
        RETR(1), STOR(1), APPE(1), NONE(0);

        private final int numberOfArgs;

        Type(int numberOfArgs) {
            this.numberOfArgs = numberOfArgs;
        }

        public int getNumberOfArgs() {
            return numberOfArgs;
        }
    }
}
