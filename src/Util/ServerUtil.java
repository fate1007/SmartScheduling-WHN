package Util;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

/**
 * Created by bml on 05/07/2016.
 */
public class ServerUtil {
    public static void page404(HttpExchange request) {
        String str404 = "{\"error\": 404}";
        try {
            request.sendResponseHeaders(404, str404.getBytes().length);
            request.getResponseBody().write(str404.getBytes());
            request.getResponseBody().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
