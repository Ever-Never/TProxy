package sii.uniroma2.HonorineCevallos.TProxy.logManaging;

import java.nio.ByteBuffer;

import sii.uniroma2.HonorineCevallos.TProxy.PacketManager.AppLayerPacket;
import sii.uniroma2.HonorineCevallos.TProxy.PacketManager.Packet;

/**
 * Created by Jesus on 04/02/2016.
 */
public class PacketInfo {
    public static final int IP4_HEADER_SIZE = 20;
    public static final int TCP_HEADER_SIZE = 20;
    public static final int UDP_HEADER_SIZE = 8;

    public Packet.IP4Header ip4Header;
    public Packet.TCPHeader tcpHeader;
    public Packet.UDPHeader udpHeader;
    public AppLayerPacket payload;
    public ByteBuffer backingBuffer;



}
