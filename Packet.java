import java.nio.ByteBuffer;

/**
 * Created by a2shadab on 19/10/17.
 * This class is used to create or unpack Packets
 */
public class Packet implements Comparable<Packet>{

    final int HEADER_SIZE = 12;

    private int type;
    private int length;
    private int seqNumber;
    private long timeout;
    private byte[] payload;
    private ByteBuffer packetAsBytes;

    /**
     *
     * @param type
     * @param seqNumber
     *
     * Create packets with only type and seq num.
     * Used for creating ack and eot packets
     *
     */
    public Packet(int type, int seqNumber) {
        this.type = type;
        this.seqNumber = seqNumber;
        this.length = HEADER_SIZE;
        this.payload = null;
        this.timeout = 0;
        setPacketAsBytes();
    }

    /**
     *
     * @param type
     * @param seqNumber
     * @param payload
     *
     * Create packet with payload. Used for data packets
     */
    public Packet(int type, int seqNumber, byte[] payload) {
        this.type = type;
        this.seqNumber = seqNumber;
        this.payload = payload;
        this.length = HEADER_SIZE + payload.length;
        this.timeout = 0;
        setPacketAsBytes();
    }

    /**
     *
     * @param packetAsBytesArray
     *
     * Used to unpack a byte array into a packet
     */
    public Packet(byte[] packetAsBytesArray) {
        this.packetAsBytes = ByteBuffer.wrap(packetAsBytesArray);
        bytesToValues();
    }

    /**
     *
     * @param type
     * @param seqNumber
     * @param payload
     * @param timeout
     *
     * Create packet with a timeout value. Used to generate packets for
     * Selective Repeat protocol
     */
    public Packet(int type, int seqNumber, byte[] payload, long timeout) {
        this.type = type;
        this.seqNumber = seqNumber;
        this.payload = payload;
        this.length = HEADER_SIZE + payload.length;
        this.timeout = timeout;
        setPacketAsBytes();
    }

    /**
     * Convert the packet conten into a byte array
     */
    private  void setPacketAsBytes() {
        packetAsBytes = ByteBuffer.allocate(length);
        packetAsBytes.putInt(type);
        packetAsBytes.putInt(length);
        packetAsBytes.putInt(seqNumber);
        if(payload != null && payload.length != 0) {
            packetAsBytes.put(payload, 0, payload.length);
        }
    }

    /**
     * Get packet properties from byte array
     */
    private void bytesToValues() {
        type = packetAsBytes.getInt();
        length = packetAsBytes.getInt();
        seqNumber = packetAsBytes.getInt();
        if(length <= HEADER_SIZE) {
            payload = null;
        }
        else {
            payload = new byte[length - HEADER_SIZE];
            packetAsBytes.get(payload);
        }
    }

    /**
     *
     * @return int
     * Type of packet
     */
    public int getType() {
        return type;
    }

    /**
     *
     * @return int
     * Get length of packet
     */
    public int getLength() {
        return length;
    }

    /**
     *
     * @return int
     * Get seq num of packet
     */
    public int getSeqNumber() {
        return seqNumber;
    }

    /**
     *
     * @return
     * Get payload
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     *
     * @return
     * Packet as byte array
     */
    public byte[] getPacketAsByte() {
        return packetAsBytes.array();
    }

    /**
     *
     * @return
     * Get timeout of the packet
     */
    public long getTimeout () {
        return timeout;
    }

    /**
     *
     * @param timeout
     * Set timeout for the packet
     */
    public void setTimeout (long timeout) {
        this.timeout = timeout;
    }

    /**
     *
     * @return String
     */
    @Override
    public String toString() {
        return "Packet No." + this.seqNumber + " timeout:" + this.getTimeout();
    }

    /**
     *
     * @param comparePacket
     * @return
     *
     * This is used to compare packets with respect to when they timeout
     */
    @Override
    public int compareTo(Packet comparePacket) {
        if (this.getTimeout() - comparePacket.getTimeout() < 0) return -1;
        else if (this.getTimeout() - comparePacket.getTimeout() > 0) return 1;
//        else if(this.getSeqNumber() - comparePacket.getSeqNumber() < 0) return -1;
//        else if(this.getSeqNumber() - comparePacket.getSeqNumber() > 0) return 1;
        else return 0;
    }
}
