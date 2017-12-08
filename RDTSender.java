import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Scanner;

/**
 * Created by a2shadab on 28/10/17.
 * Send data to the receiver over a reliable channel
 */
public abstract class RDTSender extends Host {

    protected InetAddress channelName;
    protected int channelPort;
    // tracks the base seq num
    protected int baseSqNum;
    // tracks the last acknowledged packet in sequence
    protected int ackSeqNum;
    // tracks the last sent packet in sequence
    protected int lastSentSeqNum;
    protected byte[] receiverBuffer;
    protected byte[] senderBuffer;
    // set the timeout
    protected int timeout;
    // to check if the file has reached the end of transmission
    protected boolean isEOT;
    // total send attempts made by the sender
    protected int sendAttempts;
    protected FileInputStream fileInputStream;
    protected BufferedInputStream bufferedInputStream;

    public RDTSender(String path, int timeout) throws FileNotFoundException {
        this.channelName = null;
        this.channelPort = 0;
        this.baseSqNum = 0;
        this.ackSeqNum = -1;
        this.lastSentSeqNum = -1;
        this.isEOT = false;
        this.sendAttempts = 0;
        this.receiverBuffer = new byte[BUFFER_SIZE];
        this.senderBuffer = new byte[PAYLOAD_SIZE];
        this.timeout = timeout;
        this.fileInputStream = new FileInputStream(path);
        this.bufferedInputStream = new BufferedInputStream(fileInputStream);
    }

    /**
     *
     * @param datagramSocket
     * @throws IOException
     *
     * Send packets to the receiver till the window is full. Has been implemented in
     * SelectiveRepeatSender and GoBackNSender
     */
    protected void sendPackets(DatagramSocket datagramSocket) throws IOException{}

    /**
     *
     * @param datagramSocket
     * @throws IOException
     *
     * Waits for acknowledgement from the receiver
     */
    protected void receiveAck(DatagramSocket datagramSocket) throws IOException{};

    /**
     *
     * @param datagramSocket
     * @throws IOException
     *
     * Retransmits packet/packets according to the protocol used when there is a
     * timeout.
     */
    protected void reTransmit(DatagramSocket datagramSocket) throws IOException{};

    public void start() throws IOException {

        getChannelInfo();
        DatagramSocket datagramSocket = new DatagramSocket();
        while (!(lastSentSeqNum == ackSeqNum && isEOT)) {
            try {
                sendPackets(datagramSocket);
                receiveAck(datagramSocket);
            }
            catch (SocketTimeoutException e) {
                reTransmit(datagramSocket);
            }
        }
        sendEOTPacket(datagramSocket);
        receiveEOTPacket(datagramSocket);
        System.out.println("TOTAL SEND ATTEMPTS:" + sendAttempts);
        datagramSocket.close();
    }

    /**
     *
     * @throws IOException
     * Extract channel address and port number from channelInfo file
     */
    private void getChannelInfo() throws IOException {
        FileReader channelInfo = new FileReader("channelInfo");
        Scanner scanner = new Scanner(channelInfo);
        String[] info = scanner.nextLine().split(" ");
        this.channelName = InetAddress.getByName(info[0]);
        this.channelPort = Integer.valueOf(info[1]);
        scanner.close();
        channelInfo.close();
    }

    /**
     *
     * @param datagramSocket
     * @throws IOException
     * Sends EOT packet to the receiver. The sequence number in this packet is
     * set to the seq number of the last sent data packet
     */
    private void sendEOTPacket(DatagramSocket datagramSocket) throws IOException {
        Packet packet = new Packet(EOT_PACKET, lastSentSeqNum);
        DatagramPacket datagramPacket = new DatagramPacket(packet.getPacketAsByte(), packet.getLength(), channelName, channelPort);
        datagramSocket.send(datagramPacket);
        printLog(SEND, packet);
    }

    /**
     *
     * @param datagramSocket
     * @throws IOException
     *
     * Receives EOT packet from the receiver
     */
    private void receiveEOTPacket(DatagramSocket datagramSocket) throws IOException {
        DatagramPacket eotPacket = new DatagramPacket(receiverBuffer, receiverBuffer.length);
        datagramSocket.setSoTimeout(0);
        datagramSocket.receive(eotPacket);
        Packet packet = new Packet(eotPacket.getData());
        printLog(RECV, packet);
    }
}