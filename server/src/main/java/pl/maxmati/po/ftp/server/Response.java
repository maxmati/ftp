package pl.maxmati.po.ftp.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by maxmati on 1/8/16
 */
public class Response {
    //2xx
    public static final int BYE_CODE = 221;
    public static final int USER_LOGGED_IN_CODE = 230;
    //3xx
    public static final int PASSWORD_REQUIRED_CODE = 331;
    //5xx
    public static final int SYNTAX_ERROR_CODE = 501;
    public static final int BAD_SEQUENCE_OF_COMMANDS_CODE = 503;

    private static Map<Integer, String> codeFormatMap = new HashMap<>();

    static {
        //2xx
        codeFormatMap.put(BYE_CODE, "Bye");
        codeFormatMap.put(USER_LOGGED_IN_CODE, "User logged in");
        //3xx
        codeFormatMap.put(PASSWORD_REQUIRED_CODE, "Password required");
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
