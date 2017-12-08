import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by a2shadab on 18/10/17.
 */
public class Receiver extends Host {

    private final static int PROTOCOL = 0;
    private final static int PATHNAME = 1;

    private FileOutputStream fileOutputStream;
    private BufferedOutputStream bufferedOutputStream;
    private byte[] senderBuffer;
    private int nextSeqNum;
    private int currSeqNum;
    private Map<Integer, Packet> recievedPackets;
    private int baseSeqNum;

    /**
     *
     * @param pathname
     * @throws IOException
     */
    public Receiver(String pathname) throws IOException {
        this.fileOutputStream = new FileOutputStream(pathname);
        this.bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        this.senderBuffer = new byte[BUFFER_SIZE];
        this.nextSeqNum = 0;
        this.currSeqNum = -1;
        this.recievedPackets = new HashMap<>();
        this.baseSeqNum = 0;
    }

    /**
     *
     * @param protocol
     * @throws IOException
     *
     * This function opens the UDP socket of the receiver and uses the type of
     * protocol provided by the user to receive packets from the sender. When
     * it receives an EOT packet it sends an EOT to the sender and exits.
     */
    public void startUDPConnection(int protocol) throws IOException {
        DatagramSocket datagramSocket = new DatagramSocket();
        FileWriter recvInfo = new FileWriter("recvInfo");
        recvInfo.write(InetAddress.getLocalHost().getHostName() + " " + datagramSocket.getLocalPort());
        recvInfo.close();
        while (true) {
            DatagramPacket datagramPacket = new DatagramPacket(senderBuffer, senderBuffer.length);
            System.out.println("WAITING FOR PACKET...");
            datagramSocket.receive(datagramPacket);
            Packet packet = new Packet(datagramPacket.getData());
            printLog(RECV, packet);
            if (packet.getType() == EOT_PACKET) {
                DatagramPacket eotPacket = new DatagramPacket(datagramPacket.getData(),
                        datagramPacket.getLength(),
                        datagramPacket.getAddress(),
                        datagramPacket.getPort());
                datagramSocket.send(eotPacket);
                printLog(SEND, packet);
                break;
            }
            else if(packet.getType() == DATA_PACKET) {
                if (protocol == GO_BACK_N) {
                    goBackN(datagramPacket, packet, datagramSocket);
                }
                else if (protocol == SELECTIVE_REPEAT) {
                    selectiveRepeat(datagramPacket, packet, datagramSocket);
                }
            }
        }
        datagramSocket.close();
        bufferedOutputStream.close();
        fileOutputStream.close();
    }

    /**
     *
     * @param datagramPacket
     * @param packet
     * @param datagramSocket
     * @throws IOException
     *
     * Uses the selective repeat protocol to receive packets from the sender
     * Stores all the received files in a map, with seq number as the key to
     * keep track of what has been received. When in sequence packets are
     * received, it writes the packets to the file and slides the window to
     * receive more packets.
     */
    public void selectiveRepeat(DatagramPacket datagramPacket,
                                Packet packet,
                                DatagramSocket datagramSocket) throws IOException {
        if (isSeqNumInWindow(baseSeqNum, packet.getSeqNumber())) {
            if (!recievedPackets.containsKey(packet.getSeqNumber())) {
                recievedPackets.put(packet.getSeqNumber(), packet);
            }
        }
        if (isSeqNumInCurrOrPrevWindow(baseSeqNum, packet.getSeqNumber())) {
            sendAckPacket(datagramPacket.getAddress(),
                    datagramPacket.getPort(),
                    datagramSocket,
                    packet.getSeqNumber());
        }
        // if packets are in order write them to the file and increment base sequence number
        while (recievedPackets.containsKey(baseSeqNum)) {
            System.out.println("WRITING PKT " + packet.getSeqNumber() + " TO FILE");
            bufferedOutputStream.write(recievedPackets.get(baseSeqNum).getPayload());
            recievedPackets.remove(baseSeqNum);
            baseSeqNum = incrementSeqNumber(baseSeqNum);
        }
    }

    /**
     *
     * @param datagramPacket
     * @param packet
     * @param datagramSocket
     * @throws IOException
     *
     * Uses the Go Back N protocol to receive packets from the sender
     */
    public void goBackN(DatagramPacket datagramPacket,
                        Packet packet,
                        DatagramSocket datagramSocket) throws IOException {
        if ( packet.getSeqNumber() == nextSeqNum) {
            System.out.println("WRITING PKT " + packet.getSeqNumber() + " TO FILE");
            bufferedOutputStream.write(packet.getPayload());
            sendAckPacket(datagramPacket.getAddress(),
                    datagramPacket.getPort(),
                    datagramSocket,
                    packet.getSeqNumber());
            currSeqNum = packet.getSeqNumber();
            nextSeqNum = incrementSeqNumber(nextSeqNum);
        }
        else if (currSeqNum > -1) {
            sendAckPacket(datagramPacket.getAddress(), datagramPacket.getPort(), datagramSocket, currSeqNum);
        }
    }

    /**
     *
     * @param channelName
     * @param channelPort
     * @param datagramSocket
     * @param seqNumber
     * @throws IOException
     *
     * Send ack packet to the sender for the received data packet
     */
    public void sendAckPacket(InetAddress channelName,
                              int channelPort,
                              DatagramSocket datagramSocket,
                              int seqNumber) throws IOException {
        Packet packet = new Packet(ACK_PACKET, seqNumber);
        DatagramPacket datagramPacket = new DatagramPacket(packet.getPacketAsByte(),
                packet.getLength(),
                channelName,
                channelPort);
        datagramSocket.send(datagramPacket);
        printLog(SEND, packet);
    }

    /**
     *
     * @param args
     * @throws IOException
     *
     * Main function to start the receiver
     */
    public static void main(String[] args) throws Exception {
        int protocol = Integer.parseInt(args[PROTOCOL]);
        String pathname = args[PATHNAME];
        if(!(protocol == 0 || protocol == 1)) {
            throw new Exception("Incorrect protocol");
        }
        if(pathname == null || pathname.isEmpty()) {
            throw new Exception("Missing file name");
        }
        Receiver receiver = new Receiver(pathname);
        receiver.startUDPConnection(protocol);
    }
}
