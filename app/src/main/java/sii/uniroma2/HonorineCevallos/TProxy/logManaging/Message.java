package sii.uniroma2.HonorineCevallos.TProxy.logManaging;

/**
 * Created by Jesus on 04/02/2016.
 */
public class Message {
    public long packetNumber;
    public boolean isIncomming;
    public String transportProtocol;
    public String connectivityType;
    public String Timestamp;
    public String destinationAddr;
    public String sourceAddr;
    public String TCPSpecialPacket; /*FIN,SYN,RST,PSH,ACK,URG*/

    @Override
    public String toString(){
        String str = "Paq_no: "+this.packetNumber+" ";
        if(this.isIncomming){
            str+= " (Incomming) ";
        }else{
            str+= "(Outgoing) ";
        }
        str+= "\n T_Proto: "+this.transportProtocol;
        if(this.transportProtocol.equals("TCP")){
            str+= " "+TCPSpecialPacket;
        }
        str+= "\n Conn_type: "+this.connectivityType;
        str+= "\n Timsstamp: "+this.Timestamp;
        str+= "\n Dest_Addr: "+this.destinationAddr;
        str+= "\n Src_Addr: "+this.sourceAddr;
        return str;
    }

}