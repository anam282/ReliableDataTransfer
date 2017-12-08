/**
 * Created by a2shadab on 28/10/17.
 *
 * Contains constants and functions shared by both sender and receiver
 */

public abstract class Host {

    final static int WINDOW_SIZE = 10;
    final static int PAYLOAD_SIZE = 500;
    final static int BUFFER_SIZE = 1024;
    final static int GO_BACK_N = 0;
    final static int SELECTIVE_REPEAT = 1;
    final static int DATA_PACKET = 0;
    final static int ACK_PACKET = 1;
    final static int EOT_PACKET = 2;
    final static String SEND = "SEND";
    final static String RECV = "RECV";

    private final static int MAX_SEQ_NUM = 256;


    /**
     *
     * @param number sequence number of the packet
     * @return sequence number incremented by one
     *
     * Increments the sequence number
     */
    public int incrementSeqNumber(int number) {
        return (++number)%MAX_SEQ_NUM;
    }

    /**
     *
     * @param baseSeq
     * @param seqNum
     * @return true if sequence number lies within current window, false otherwise
     *
     * Checks if sequence number is within the current window
     */
    public boolean isSeqNumInWindow(int baseSeq, int seqNum) {

        if(baseSeq + WINDOW_SIZE < MAX_SEQ_NUM) {
            if(seqNum >= baseSeq && seqNum < baseSeq + WINDOW_SIZE) {
                return true;
            }
        }
        else {
            if(seqNum >= baseSeq && seqNum < MAX_SEQ_NUM ||
                    seqNum >= 0 && seqNum < (baseSeq+WINDOW_SIZE)%MAX_SEQ_NUM ) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param baseSeq
     * @param seqNum
     * @return true if sequence number lies in current window or previous window
     */
    public boolean isSeqNumInCurrOrPrevWindow(int baseSeq, int seqNum) {

        int oldBase = (baseSeq - WINDOW_SIZE)%MAX_SEQ_NUM;
        if(oldBase < 0) oldBase += MAX_SEQ_NUM;
        return isSeqNumInWindow(oldBase, seqNum) ||
                isSeqNumInWindow(baseSeq, seqNum);
    }

    /**
     *
     * @param status
     * @param packet
     *
     * Print log messages
     */
    public void printLog(String status, Packet packet) {
        String type = null;
        if(packet.getType() == DATA_PACKET) {
            type = "DAT";
        }
        if(packet.getType() == ACK_PACKET) {
            type = "ACK";
        }
        if(packet.getType() == EOT_PACKET) {
            type = "EOT";
        }
        System.out.println("PKT" + " " + status + " " + type + " " + packet.getLength() + " " + packet.getSeqNumber());
    }
}
