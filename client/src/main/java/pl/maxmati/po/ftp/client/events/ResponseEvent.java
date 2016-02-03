package pl.maxmati.po.ftp.client.events;

import pl.maxmati.po.ftp.common.Response;

/**
 * Created by maxmati on 1/15/16
 */
public class ResponseEvent implements Event{
    private Response response;

    public ResponseEvent(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return "ResponseEvent{" +
                "response=" + response +
                '}';
    }
}
