import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.*;

/**
 * Created by a2shadab on 28/10/17.
 * Uses selective repeat protocol to send packets to the receiver
 */
public class SelectiveRepeatSender extends RDTSender {

    private PriorityQueue<Packet> priorityQueue;
    private Set<Integer> receivedAckFor;
    private Map<Integer, Packet> map;

    /**
     *
     * @param path
     * @param timeout
     * @throws FileNotFoundException
     */
    public SelectiveRepeatSender(String path, int timeout) throws FileNotFoundException {
        super(path, timeout);
        this.priorityQueue = new PriorityQueue<>();
        this.receivedAckFor = new HashSet<>();
        this.map = new HashMap<>();
    }

    /**
     *
     * @param datagramSocket
     * @throws IOException
     *
     * Sends packets to the receiver until the window size is full. Each packet is set
     * with its own timeout. A priority queue is used to store the packets. The packet
     * that is going to timeout first is always at the head of the queue. A map is used
     * to maintain the packets in the window. The key in the map is the seq number to
     * keep track of what has been sent.
     */
    public void sendPackets(DatagramSocket datagramSocket) throws IOException {
        while (map.size() < WINDOW_SIZE && !isEOT) {
            int dataLen = bufferedInputStream.read(senderBuffer);
            if (dataLen < 1) {
                bufferedInputStream.close();
                fileInputStream.close();
                isEOT = true;
                break;
            }
            lastSentSeqNum = incrementSeqNumber(lastSentSeqNum);
            Packet packet = new Packet(DATA_PACKET,
                    lastSentSeqNum,
                    Arrays.copyOf(senderBuffer, dataLen),
                    System.currentTimeMillis() + (long) timeout);
            DatagramPacket datagramPacket = new DatagramPacket(packet.getPacketAsByte(), packet.getLength(), channelName, channelPort);
            priorityQueue.offer(packet);
            map.put(packet.getSeqNumber(), packet);
            datagramSocket.send(datagramPacket);
            sendAttempts++;
            printLog(SEND, packet);
        }
    }

    /**
     *
     * @param datagramSocket
     * @throws IOException
     *
     * Receive ack packets from the receiver. The timeout for receive() is set to the
     * time at which the first packet is going to expire. This is done so that when
     * the timer is up for one packet, it can be retransmitted.
     */
    public void receiveAck(DatagramSocket datagramSocket) throws IOException {
        if (priorityQueue.isEmpty()) return;
        // Check if the timer of the first packet has run out. If so throw an exception
        int time = (int) (priorityQueue.peek().getTimeout() - System.currentTimeMillis());
        if (time <= 0) {
            throw new SocketTimeoutException("PACKET TIMED OUT");
        }
        DatagramPacket datagramPacket = new DatagramPacket(receiverBuffer, receiverBuffer.length);
        System.out.println("WAITING FOR ACK PACKET..." );
        // set the waiting time such that receive() waits only until the time a packet times out
        datagramSocket.setSoTimeout(time);
        datagramSocket.receive(datagramPacket);
        Packet packet = new Packet(datagramPacket.getData());
        printLog(RECV, packet);
        if (isSeqNumInWindow(baseSqNum, packet.getSeqNumber())) {
            receivedAckFor.add(packet.getSeqNumber());
            priorityQueue.remove(map.get(packet.getSeqNumber()));
        }
        while (receivedAckFor.contains(baseSqNum)) {
            receivedAckFor.remove(baseSqNum);
            map.remove(baseSqNum);
            ackSeqNum = baseSqNum;
            baseSqNum = incrementSeqNumber(baseSqNum);
        }
    }

    /**
     *
     * @param datagramSocket
     * @throws IOException
     *
     * Retransmit the packets that have timed out by looking at the
     * head of the queue
     */
    public void reTransmit(DatagramSocket datagramSocket) throws IOException {
        while(!priorityQueue.isEmpty() && priorityQueue.peek().getTimeout() < System.currentTimeMillis()) {
            Packet packet = priorityQueue.poll();
            DatagramPacket datagramPacket = new DatagramPacket(packet.getPacketAsByte(), packet.getLength(), channelName, channelPort);
            packet.setTimeout(System.currentTimeMillis() + (long)timeout);
            priorityQueue.offer(packet);
            datagramSocket.send(datagramPacket);
            printLog(SEND, packet);
            sendAttempts++;
        }
    }
}
