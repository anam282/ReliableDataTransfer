/**
 * Created by a2shadab on 18/10/17.
 * Sends a file to the receiver
 */
public class Sender {

    private static final int PROTOCOL = 0;
    private static final int TIMEOUT = 1;
    private static final int PATHNAME = 2;

    public static void main(String[] args) throws Exception {
        int protocolSelector = Integer.valueOf(args[PROTOCOL]);
        int timeout = Integer.valueOf(args[TIMEOUT]);
        String pathName = args[PATHNAME];
        if(timeout <= 0) {
            throw new Exception("Timeout cannot be zero or negative");
        }
        if(pathName == null || pathName.isEmpty()) {
            throw new Exception("Missing file name");
        }
        RDTSender sender = null;
        // Select the protocol based on the option selected by the user
        if(protocolSelector == Host.GO_BACK_N) {
            sender = new GoBackNSender(pathName, timeout);
        }
        else if(protocolSelector == Host.SELECTIVE_REPEAT){
            sender = new SelectiveRepeatSender(pathName, timeout);
        }
        if(sender != null) {
            sender.start();
        }
        else throw new Exception("Incorrect protocol id");
    }
}