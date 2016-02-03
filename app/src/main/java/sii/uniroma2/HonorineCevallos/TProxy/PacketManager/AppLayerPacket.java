package sii.uniroma2.HonorineCevallos.TProxy.PacketManager;

import sii.uniroma2.HonorineCevallos.TProxy.PacketManager.Http.Header;
import sii.uniroma2.HonorineCevallos.TProxy.PacketManager.Http.HttpParser;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Jesus on 19/01/2016.
 */
public class AppLayerPacket {

    public Header[] headers;
    public ByteBuffer buffer;
    public HttpParser parser;

    public AppLayerPacket(ByteBuffer _buffer){
        parser = new HttpParser();
        try {
            this.buffer=_buffer;
            this.headers =parser.parseHeaders(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
