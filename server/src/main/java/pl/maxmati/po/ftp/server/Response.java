package pl.maxmati.po.ftp.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by maxmati on 1/8/16
 */
public class Response {
    //1xx
    public static final int OPENING_PASSIVE_CONNECTION_CODE = 150;
    //2xx
    public static final int COMMAND_SUCCESSFUL_CODE = 200;
    public static final int BYE_CODE = 221;
    public static final int TRANSFER_COMPLETE_CODE = 226;
    public static final int ENTERING_PASSIVE_MODE_CODE = 227;
    public static final int USER_LOGGED_IN_CODE = 230;
    //3xx
    public static final int PASSWORD_REQUIRED_CODE = 331;
    //4xx
    public static final int INVALID_USER_OR_PASS_CODE = 430;
    //5xx
    public static final int SYNTAX_ERROR_CODE = 501;
    public static final int BAD_SEQUENCE_OF_COMMANDS_CODE = 503;

    private static Map<Integer, String> codeFormatMap = new HashMap<>();

    static {
        //1xx
        codeFormatMap.put(OPENING_PASSIVE_CONNECTION_CODE, "Opening %s mode data connection for '%s'");
        //2xx
        codeFormatMap.put(COMMAND_SUCCESSFUL_CODE, "Command successful");
        codeFormatMap.put(BYE_CODE, "Bye");
        codeFormatMap.put(TRANSFER_COMPLETE_CODE, "Transfer complete");
        codeFormatMap.put(ENTERING_PASSIVE_MODE_CODE, "Entering Passive Mode (%d,%d,%d,%d,%d,%d)");
        codeFormatMap.put(USER_LOGGED_IN_CODE, "User logged in");
        //3xx
        codeFormatMap.put(PASSWORD_REQUIRED_CODE, "Password required");
        //4xx
        codeFormatMap.put(INVALID_USER_OR_PASS_CODE, "Invalid username or password");
        //5xx
        codeFormatMap.put(SYNTAX_ERROR_CODE, "Syntax error in parameters or arguments");
        codeFormatMap.put(BAD_SEQUENCE_OF_COMMANDS_CODE, "Bad sequence of commands");
    }

    private final int code;
    private final Object[] params;

    public Response(int code, Object... params) {
        this.code = code;
        this.params = params;
    }

    public String toNetworkString(){
        return String.valueOf(code) +
                " " +
                String.format(codeFormatMap.get(code), params) +
                "\n";
    }

    @Override
    public String toString() {
        return "Response{" +
                "code=" + code +
                ", params=" + Arrays.toString(params) +
                '}';
    }
}
