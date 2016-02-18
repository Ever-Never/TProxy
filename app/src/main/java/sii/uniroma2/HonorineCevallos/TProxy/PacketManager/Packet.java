package sii.uniroma2.HonorineCevallos.TProxy.PacketManager;

/*
** Copyright 2015, Mohamed Naufal
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;


/**
 * Classe di riferimento per i datagrammi IP
 */
public class Packet
{
    public static final int IP4_HEADER_SIZE = 20;
    public static final int TCP_HEADER_SIZE = 20;
    public static final int UDP_HEADER_SIZE = 8;

    public IP4Header ip4Header;
    public TCPHeader tcpHeader;
    public UDPHeader udpHeader;
    public ByteBuffer backingBuffer;


    private boolean isTCP;
    private boolean isUDP;
    private boolean isIncomming;

    /** Il costruttore fa il parsing del buffer per istanziare
     * le classi che rappresentano tutte le componenti del datagramma,
     * quindi header e pacchetti di trasporto, anche questi con il loro
     * header e payload.
     * ATT. Il header di TCP ha lunghezza variabile per il fatto che ci sono
     * le options TCP, che di solto vengon inviate durante l'handshake.
     * @param buffer
     * @throws UnknownHostException
     */
    public Packet(ByteBuffer buffer) throws UnknownHostException {
        this.ip4Header = new IP4Header(buffer);
        if (this.ip4Header.protocol == IP4Header.TransportProtocol.TCP) {
            this.tcpHeader = new TCPHeader(buffer);
            this.isTCP = true;
        } else if (ip4Header.protocol == IP4Header.TransportProtocol.UDP) {
            this.udpHeader = new UDPHeader(buffer);
            this.isUDP = true;
        }
        this.backingBuffer = buffer;

    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("Packet{");
        sb.append("ip4Header=").append(ip4Header);
        if (isTCP) sb.append(", tcpHeader=").append(tcpHeader);
        else if (isUDP) sb.append(", udpHeader=").append(udpHeader);
        sb.append(", payloadSize=").append(backingBuffer.limit() - backingBuffer.position());
        sb.append('}');
        return sb.toString();
    }

    public boolean isTCP()
    {
        return isTCP;
    }

    public boolean isUDP()
    {
        return isUDP;
    }

    public boolean isIncomming() {
        return isIncomming;
    }


    public void setIncomming(boolean incomming) {
        isIncomming = incomming;
    }



    public void swapSourceAndDestination()
    {
        InetAddress newSourceAddress = ip4Header.destinationAddress;
        ip4Header.destinationAddress = ip4Header.sourceAddress;
        ip4Header.sourceAddress = newSourceAddress;

        if (isUDP)
        {
            int newSourcePort = udpHeader.destinationPort;
            udpHeader.destinationPort = udpHeader.sourcePort;
            udpHeader.sourcePort = newSourcePort;
        }
        else if (isTCP)
        {
            int newSourcePort = tcpHeader.destinationPort;
            tcpHeader.destinationPort = tcpHeader.sourcePort;
            tcpHeader.sourcePort = newSourcePort;
        }
    }

    /**
     * @param buffer
     * @param flags
     * @param sequenceNum
     * @param ackNum
     * @param payloadSize
     */
    public void updateTCPBuffer(ByteBuffer buffer, byte flags, long sequenceNum, long ackNum, int payloadSize)
    {
        buffer.position(0);
        fillHeader(buffer);
        backingBuffer = buffer;

        tcpHeader.flags = flags;

        /*a call to put(int index, byte b)
        Writes a byte to the specified index of this buffer without changing the position.
        The following codelines are thus modifying the layer 4 header corrispondingly to the
        parameters of the presetn method call
        */
        backingBuffer.put(IP4_HEADER_SIZE + 13, flags);

        tcpHeader.sequenceNumber = sequenceNum;
        backingBuffer.putInt(IP4_HEADER_SIZE + 4, (int) sequenceNum);

        tcpHeader.acknowledgementNumber = ackNum;
        backingBuffer.putInt(IP4_HEADER_SIZE + 8, (int) ackNum);

        // Reset header size, since we don't need options
        //TODO Change with dataOffset = (byte) 5; for better understanding!
        byte dataOffset = (byte) (TCP_HEADER_SIZE << 2);
        tcpHeader.dataOffsetAndReserved = dataOffset;
        backingBuffer.put(IP4_HEADER_SIZE + 12, dataOffset);
        //end resetting header size to 5

        updateTCPChecksum(payloadSize);

        int ip4TotalLength = IP4_HEADER_SIZE + TCP_HEADER_SIZE + payloadSize;
        backingBuffer.putShort(2, (short) ip4TotalLength);
        ip4Header.totalLength = ip4TotalLength;

        updateIP4Checksum();
    }

    public void updateUDPBuffer(ByteBuffer buffer, int payloadSize)
    {
        buffer.position(0);
        fillHeader(buffer);
        backingBuffer = buffer;

        int udpTotalLength = UDP_HEADER_SIZE + payloadSize;
        backingBuffer.putShort(IP4_HEADER_SIZE + 4, (short) udpTotalLength);
        udpHeader.length = udpTotalLength;

        // Disable UDP checksum validation
        backingBuffer.putShort(IP4_HEADER_SIZE + 6, (short) 0);
        udpHeader.checksum = 0;

        int ip4TotalLength = IP4_HEADER_SIZE + udpTotalLength;
        backingBuffer.putShort(2, (short) ip4TotalLength);
        ip4Header.totalLength = ip4TotalLength;

        updateIP4Checksum();
    }

    private void updateIP4Checksum()
    {
        ByteBuffer buffer = backingBuffer.duplicate();
        buffer.position(0);

        // Clear previous checksum
        buffer.putShort(10, (short) 0);

        int ipLength = ip4Header.headerLength;
        int sum = 0;
        while (ipLength > 0)
        {
            sum += BitUtils.getUnsignedShort(buffer.getShort());
            ipLength -= 2;
        }
        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;
        ip4Header.headerChecksum = sum;
        backingBuffer.putShort(10, (short) sum);
    }

    /**
     * @param payloadSize
     */
    private void updateTCPChecksum(int payloadSize)
    {
        //TODO check if this is correct, + ipv6 support??
        int sum = 0;
        int tcpLength = TCP_HEADER_SIZE + payloadSize;

        // Calculate pseudo-header checksum
        ByteBuffer buffer = ByteBuffer.wrap(ip4Header.sourceAddress.getAddress());
        sum = BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());

        //we create a new buffer wrapping the current address at destinationAddress.
        buffer = ByteBuffer.wrap(ip4Header.destinationAddress.getAddress());
        sum += BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());


        sum += IP4Header.TransportProtocol.TCP.getNumber() + tcpLength;

        buffer = backingBuffer.duplicate();
        // Clear previous checksum
        buffer.putShort(IP4_HEADER_SIZE + 16, (short) 0);

        // Calculate TCP segment checksum
        buffer.position(IP4_HEADER_SIZE);
        while (tcpLength > 1)
        {
            sum += BitUtils.getUnsignedShort(buffer.getShort());
            tcpLength -= 2;
        }
        if (tcpLength > 0)
            sum += BitUtils.getUnsignedByte(buffer.get()) << 8;

        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;
        tcpHeader.checksum = sum;
        backingBuffer.putShort(IP4_HEADER_SIZE + 16, (short) sum);
    }

    /**Writes the layer 3 and layer 4 headers of the packet according to the info present on the
     * members of the current packet object.
     * @param buffer
     */
    private void fillHeader(ByteBuffer buffer)
    {
        ip4Header.fillHeader(buffer);
        if (isUDP)
            udpHeader.fillHeader(buffer);
        else if (isTCP)
            tcpHeader.fillHeader(buffer);
    }


    public static class IP4Header
    {
        public byte version;
        public byte IHL;
        public int headerLength;
        public short typeOfService;
        public int totalLength;

        public int identificationAndFlagsAndFragmentOffset;

        public short TTL;
        private short protocolNum;
        public TransportProtocol protocol;
        public int headerChecksum;

        public InetAddress sourceAddress;
        public InetAddress destinationAddress;

        public int optionsAndPadding;

        private enum TransportProtocol
        {
            TCP(6),
            UDP(17),
            Other(0xFF);

            private int protocolNumber;

            TransportProtocol(int protocolNumber)
            {
                this.protocolNumber = protocolNumber;
            }

            private static TransportProtocol numberToEnum(int protocolNumber)
            {
                if (protocolNumber == 6)
                    return TCP;
                else if (protocolNumber == 17)
                    return UDP;
                else
                    return Other;
            }

            public int getNumber()
            {
                return this.protocolNumber;
            }

            public String getName(){
                if (this.equals(TCP) )
                    return "TCP";
                else if (this.equals(UDP))
                    return "UDP";
                else
                    return "Other";
            }
        }

        public IP4Header(){

        }

        private IP4Header(ByteBuffer buffer) throws UnknownHostException
        {
            //take the first byte
            byte versionAndIHL = buffer.get();
            //Shift right 4 bits
            this.version = (byte) (versionAndIHL >> 4);

            //AND with 00001111
            //4-byte word length (min 5, max 15)
            this.IHL = (byte) (versionAndIHL & 0x0F);
            //byte length (min 20, max 60)
            this.headerLength = this.IHL << 2;


            this.typeOfService = BitUtils.getUnsignedByte(buffer.get());
            this.totalLength = BitUtils.getUnsignedShort(buffer.getShort());

            this.identificationAndFlagsAndFragmentOffset = buffer.getInt();

            this.TTL = BitUtils.getUnsignedByte(buffer.get());
            this.protocolNum = BitUtils.getUnsignedByte(buffer.get());
            this.protocol = TransportProtocol.numberToEnum(protocolNum);
            this.headerChecksum = BitUtils.getUnsignedShort(buffer.getShort());

            byte[] addressBytes = new byte[4];
            buffer.get(addressBytes, 0, 4);
            this.sourceAddress = InetAddress.getByAddress(addressBytes);

            buffer.get(addressBytes, 0, 4);
            this.destinationAddress = InetAddress.getByAddress(addressBytes);

            //this.optionsAndPadding = buffer.getInt();
        }


        /**Given a ByteBuffer, writes the ip header to it.
         * @param buffer
         */
        public void fillHeader(ByteBuffer buffer)
        {
            buffer.put((byte) (this.version << 4 | this.IHL));
            buffer.put((byte) this.typeOfService);
            buffer.putShort((short) this.totalLength);

            buffer.putInt(this.identificationAndFlagsAndFragmentOffset);

            buffer.put((byte) this.TTL);
            buffer.put((byte) this.protocol.getNumber());
            buffer.putShort((short) this.headerChecksum);

            buffer.put(this.sourceAddress.getAddress());
            buffer.put(this.destinationAddress.getAddress());
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder("IP4Header{");
            sb.append("version=").append(version);
            sb.append(", IHL=").append(IHL);
            sb.append(", typeOfService=").append(typeOfService);
            sb.append(", totalLength=").append(totalLength);
            sb.append(", identificationAndFlagsAndFragmentOffset=").append(identificationAndFlagsAndFragmentOffset);
            sb.append(", TTL=").append(TTL);
            sb.append(", protocol=").append(protocolNum).append(":").append(protocol);
            sb.append(", headerChecksum=").append(headerChecksum);
            sb.append(", sourceAddress=").append(sourceAddress.getHostAddress());
            sb.append(", destinationAddress=").append(destinationAddress.getHostAddress());
            sb.append('}');
            return sb.toString();
        }



    }

    public static class TCPHeader
    {
        public static final int FIN = 0x01;
        public static final int SYN = 0x02;
        public static final int RST = 0x04;
        public static final int PSH = 0x08;
        public static final int ACK = 0x10;
        public static final int URG = 0x20;

        public int sourcePort;
        public int destinationPort;

        public long sequenceNumber;
        public long acknowledgementNumber;

        public byte dataOffsetAndReserved;
        public int headerLength;
        public byte flags;
        public int window;

        public int checksum;
        public int urgentPointer;

        public boolean hasOptions;
        public TcpOptions optionsAndPadding;


        private TCPHeader(ByteBuffer buffer)
        {
            this.sourcePort = BitUtils.getUnsignedShort(buffer.getShort());
            this.destinationPort = BitUtils.getUnsignedShort(buffer.getShort());

            this.sequenceNumber = BitUtils.getUnsignedInt(buffer.getInt());
            this.acknowledgementNumber = BitUtils.getUnsignedInt(buffer.getInt());

            this.dataOffsetAndReserved = buffer.get();
            this.headerLength = (this.dataOffsetAndReserved & 0xF0) >> 2;
            this.flags = buffer.get();
            this.window = BitUtils.getUnsignedShort(buffer.getShort());

            this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
            this.urgentPointer = BitUtils.getUnsignedShort(buffer.getShort());

            int optionsAndPaddingLength = this.headerLength - TCP_HEADER_SIZE;
            if (optionsAndPaddingLength > 0)
            {
                try {
                    hasOptions=true;
                    optionsAndPadding = new TcpOptions(buffer, optionsAndPaddingLength);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean isFIN()
        {
            return (flags & FIN) == FIN;
        }

        public boolean isSYN()
        {
            return (flags & SYN) == SYN;
        }

        public boolean isRST()
        {
            return (flags & RST) == RST;
        }

        public boolean isPSH()
        {
            return (flags & PSH) == PSH;
        }

        public boolean isACK()
        {
            return (flags & ACK) == ACK;
        }

        public boolean isURG()
        {
            return (flags & URG) == URG;
        }

        /**Writes the TCP header on a ByteBuffer
         * (supposing it already contains a valid IP header.)
         * @param buffer
         */
        private void fillHeader(ByteBuffer buffer)
        {
            buffer.putShort((short) sourcePort);
            buffer.putShort((short) destinationPort);

            buffer.putInt((int) sequenceNumber);
            buffer.putInt((int) acknowledgementNumber);

            buffer.put(dataOffsetAndReserved);
            buffer.put(flags);
            buffer.putShort((short) window);

            buffer.putShort((short) checksum);
            buffer.putShort((short) urgentPointer);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder("TCPHeader{");
            sb.append("sourcePort=").append(sourcePort);
            sb.append(", destinationPort=").append(destinationPort);
            sb.append(", sequenceNumber=").append(sequenceNumber);
            sb.append(", acknowledgementNumber=").append(acknowledgementNumber);
            sb.append(", headerLength=").append(headerLength);
            sb.append(", window=").append(window);
            sb.append(", checksum=").append(checksum);
            sb.append(", flags=");
            if (isFIN()) sb.append(" FIN");
            if (isSYN()) sb.append(" SYN");
            if (isRST()) sb.append(" RST");
            if (isPSH()) sb.append(" PSH");
            if (isACK()) sb.append(" ACK");
            if (isURG()) sb.append(" URG");
            sb.append('}');
            return sb.toString();
        }

        /** Sottoclasse aggiunta per la gestione delle opzioni TCP.
         * In particolare si gestisce la MSS con nui possiamo consegnare l'informazione
         * all'applicazione.
         * cfr.  http://jnetpcap.com/?q=node/401
         */
        public class TcpOptions
        {
            public static final int KIND_END = 0x00;
            public static final int KIND_NOP = 0x01;
            public static final int KIND_MSS = 0x02;
            public static final int KIND_WINSCALE = 0x03;
            public static final int KIND_SACK = 0x04;
            public static final int KIND_TS = 0x08;

            public static final int KIND_MSS_LENGTH = 4;
            public static final int KIND_WINSCALE_LENGTH = 3;
            public static final int KIND_SACK_LENGTH = 2;
            public static final int KIND_TS_LENGTH = 10;

            public static final int OPTION_HEADER_LENGTH = 2;

            public static final String BAD_FORMAT = "bad tcp option format";

            public int size = 0;
            public int offset = TCP_HEADER_SIZE;

            public boolean hasMss = false;
            public boolean hasWinScale = false;
            public boolean hasSack = false;
            public boolean hasTs = false;

            public int mss = 0;
            public int winScale = 0;
            public boolean sack = false;
            public long tsVal = 0;
            public long tsEcr = 0;

            public TcpOptions(ByteBuffer buffer, int length) throws Exception
            {
                this.size = length;

                while(buffer.position() <  IP4_HEADER_SIZE + TCP_HEADER_SIZE + size)
                {
                    switch( BitUtils.getUnsignedByte(buffer.get()))
                    {
                        case KIND_END:
                            return;

                        case KIND_NOP:
                            break;

                        case KIND_MSS:
                            parseMss(buffer);
                            break;

                        case KIND_WINSCALE:
                            parseWinScale(buffer);
                            break;

                        case KIND_SACK:
                            parseSack(buffer);
                            break;

                        case KIND_TS:
                            parseTs(buffer);
                            break;

                        default:
                            return;
                    }
                }
            }

            private void parseMss(ByteBuffer buffer) throws Exception
            {
                assertLengthFieldOK(KIND_MSS_LENGTH, buffer);
                this.hasMss = true;
                this.mss = BitUtils.getUnsignedShort(buffer.getShort());

            }

            private void parseWinScale(ByteBuffer buffer) throws Exception
            {
                assertLengthFieldOK(KIND_WINSCALE_LENGTH, buffer);
                this.hasWinScale = true;
                this.winScale = BitUtils.getUnsignedByte(buffer.get());

            }

            private void parseSack(ByteBuffer buffer) throws Exception
            {
                assertLengthFieldOK(KIND_SACK_LENGTH, buffer);
                this.hasSack = true;
                this.sack = true;

            }

            private void parseTs(ByteBuffer buffer) throws Exception
            {
                assertLengthFieldOK(KIND_TS_LENGTH, buffer);
                this.hasTs = true;
                this.tsVal = BitUtils.getUnsignedInt(buffer.getInt());
                this.tsEcr = BitUtils.getUnsignedInt(buffer.getInt());

            }

            private void assertLengthFieldOK(int correctValue, ByteBuffer buffer) throws Exception
            {
                if(correctValue != BitUtils.getUnsignedByte(buffer.get()))
                    throw new Exception(BAD_FORMAT);
            }

        }
    }

    public static class UDPHeader
    {
        public int sourcePort;
        public int destinationPort;

        public int length;
        public int checksum;

        private UDPHeader(ByteBuffer buffer)
        {
            this.sourcePort = BitUtils.getUnsignedShort(buffer.getShort());
            this.destinationPort = BitUtils.getUnsignedShort(buffer.getShort());

            this.length = BitUtils.getUnsignedShort(buffer.getShort());
            this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
        }

        /**Fills the UDP header on a ByteBuffer,
         * supposing it already contains an ip header.
         * @param buffer
         */
        private void fillHeader(ByteBuffer buffer)
        {
            buffer.putShort((short) this.sourcePort);
            buffer.putShort((short) this.destinationPort);

            buffer.putShort((short) this.length);
            buffer.putShort((short) this.checksum);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder("UDPHeader{");
            sb.append("sourcePort=").append(sourcePort);
            sb.append(", destinationPort=").append(destinationPort);
            sb.append(", length=").append(length);
            sb.append(", checksum=").append(checksum);
            sb.append('}');
            return sb.toString();
        }
    }

    private static class BitUtils
    {
        private static short getUnsignedByte(byte value)
        {
            return (short)(value & 0xFF);
        }

        private static int getUnsignedShort(short value)
        {
            return value & 0xFFFF;
        }

        private static long getUnsignedInt(int value)
        {
            return value & 0xFFFFFFFFL;
        }
    }
}