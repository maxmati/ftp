package pl.maxmati.ftp.common;

import java.util.Arrays;


/**
 * Created by maxmati on 1/8/16
 */
public class Response {

    private final Type type;
    private final Object[] params;

    public Response(Type type, Object... params) {
        this.type = type;
        this.params = params;
    }

    public String toNetworkString(){
        return String.valueOf(type.getCode()) +
                " " +
                String.format(type.getFormat(), params) +
                "\r\n";
    }

    @Override
    public String toString() {
        return "Response{" +
                "type=" + type +
                ", params=" + Arrays.toString(params) +
                '}';
    }

    public enum Type{
        OPENING_PASSIVE_CONNECTION( 150, "Opening %s mode data connection for '%s'", 2),
        COMMAND_SUCCESSFUL(         200, "Command successful", 0),
        HELLO(                      220, "Maxmati Server ready", 0),
        BYE(                        221, "Bye", 0),
        TRANSFER_COMPLETE(          226, "Transfer complete", 0),
        ENTERING_PASSIVE_MODE(      227, "Entering Passive Mode (%d,%d,%d,%d,%d,%d)", 6),
        USER_LOGGED_IN(             230, "User logged in", 0),
        REQUEST_COMPLETED(          250, "%s was successful", 1),
        CURRENT_DIRECTORY(          257, "\"%s\" is current directory", 1),
        CREATED_DIRECTORY(          257, "%s was created", 1),
        PASSWORD_REQUIRED(          331, "Password required", 0),
        NO_DATA_CONNECTION(         425, "Can't open data connection", 0),
        INVALID_USER_OR_PASS(       430, "Invalid username or password", 0),
        ABORTED_LOCAL_ERROR(        451, "Requested action aborted. Local error in processing.", 0),
        PERMISSION_DENIED(          500, "Permission denied", 0),
        SYNTAX_ERROR(               501, "Syntax error in parameters or arguments", 0),
        NOT_IMPLEMENTED(            502, "Not implemented", 0),
        BAD_SEQUENCE_OF_COMMANDS(   503, "Bad sequence of commands", 0),
        NO_SUCH_FILE_OR_DIR(        550, "%s: No such file or directory", 1),
        FILE_EXISTS(                550, "%s: File exists", 1),
        NOT_REGULAR_FILE(           550, "%s: Not a regular file" , 1),
        NOT_DIRECTORY(              550, "%s: Not a directory" , 1),
        DIRECTORY_NOT_EMPTY(        550, "%s: Directory not empty" , 1);

        private final int code;
        private final String format;
        private final int noParams;

        Type(int code, String format, int noParams) {
            this.code = code;
            this.format = format;
            this.noParams = noParams;
        }

        public int getCode() {
            return code;
        }

        public String getFormat() {
            return format;
        }

        public int getNoParams() {
            return noParams;
        }
    }
}