import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.*;

/**
 * Created by a2shadab on 28/10/17.
 * Uses the go back n protocol to send packets to the receiver
 */
public class GoBackNSender extends RDTSender {

    private Set<Integer> unackSeqNums;
    private Queue<DatagramPacket> queue;
    private long timer;
    private boolean isTimerOn;

    /**
     *
     * @param path
     * @param timeout
     * @throws FileNotFoundException
     */
    public GoBackNSender(String path, int timeout) throws FileNotFoundException {
        super(path, timeout);
        this.unackSeqNums = new HashSet<>();
        this.queue = new LinkedList<>();
        this.timer = 0;
        this.isTimerOn = false;
    }

    /**
     *
     * @param datagramSocket
     * @throws IOException
     *
     * Sends packet to the receiver if window is not full or there are no more packets
     * left to transmit. Also, if the timer is not already running, starts the timer before
     * sending the packets
     */
    public void sendPackets(DatagramSocket datagramSocket) throws IOException {
        while (queue.size() < WINDOW_SIZE && !isEOT) {
            if(!isTimerOn) {
                timer = System.currentTimeMillis() + timeout;
                isTimerOn = true;
            }
            int dataLen = bufferedInputStream.read(senderBuffer);
            if(dataLen < 1) {
                bufferedInputStream.close();
                fileInputStream.close();
                isEOT = true;
                break;
            }
            lastSentSeqNum = incrementSeqNumber(lastSentSeqNum);
            Packet packet = new Packet(DATA_PACKET, lastSentSeqNum, Arrays.copyOf(senderBuffer, dataLen));
            DatagramPacket datagramPacket = new DatagramPacket(packet.getPacketAsByte(), packet.getLength(), channelName, channelPort);
            queue.offer(datagramPacket);
            datagramSocket.send(datagramPacket);
            sendAttempts++;
            unackSeqNums.add(lastSentSeqNum);
            printLog(SEND, packet);
        }
    }

    /**
     *
     * @param datagramSocket
     * @throws IOException
     *
     * Waits for the receiver to send an ack packet. If there is a time out before any
     * ack is received, throw an exception so that the packets can be retransmitted.
     * If an ack is received, increment the base seq num. If the window has more packets
     * left, restart timer
     */
    public void receiveAck(DatagramSocket datagramSocket) throws IOException {
        // error in receiving ack, when old ack comes in
        int waitingTime = (int) (timer - System.currentTimeMillis());
        if(waitingTime <= 0) {
            throw new SocketTimeoutException("PACKET TIMED OUT");
        }
        DatagramPacket datagramPacket = new DatagramPacket(receiverBuffer, receiverBuffer.length);
        System.out.println("WAITING FOR ACK PACKET...");
        datagramSocket.setSoTimeout(waitingTime);
        datagramSocket.receive(datagramPacket);
        Packet packet = new Packet(datagramPacket.getData());
        printLog(RECV, packet);
        if(!(unackSeqNums.contains(packet.getSeqNumber()))) return;
        ackSeqNum = packet.getSeqNumber();
        while(ackSeqNum != baseSqNum) {
            queue.poll();
            unackSeqNums.remove(baseSqNum);
            baseSqNum = incrementSeqNumber(baseSqNum);
        }
        queue.poll();
        unackSeqNums.remove(baseSqNum);
        baseSqNum = incrementSeqNumber(baseSqNum);
        if(!queue.isEmpty())
            timer = System.currentTimeMillis() + timeout;
        else isTimerOn = false;
    }

    /**
     *
     * @param datagramSocket
     * @throws IOException
     * Restart timer and retransmit all the packets in the window
     */
    public void reTransmit(DatagramSocket datagramSocket) throws IOException {
        timer = System.currentTimeMillis() +timeout;
        isTimerOn = true;
        for(DatagramPacket datagramPacket: queue) {
            datagramSocket.send(datagramPacket);
            sendAttempts++;
            Packet packet = new Packet(datagramPacket.getData());
            printLog(SEND, packet);
        }
    }
}
