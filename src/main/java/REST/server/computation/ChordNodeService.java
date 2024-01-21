package REST.server.computation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Path("/chordnode")
public class ChordNodeService implements ChordNodeInterface {

    private String nodeId;
    private List<String> fingerTable = new ArrayList<>(); // For simplicity, using a list of strings
    private String predecessor;
    private String successor;

    public ChordNodeService(String chordAddress) {
        this.nodeId = chordAddress;
        initializeFingerTable();
    }

    private void initializeFingerTable() {
        for (int i = 0; i < m; i++) {
            fingerTable.add("");
        }
    }

    @Override
    @GET
    @Path("/join")
    @Produces({ MediaType.APPLICATION_JSON })
    public String join(@QueryParam("newNodeAddress") String newNodeAddress) {
        int newNodeId = Integer.parseInt(newNodeAddress);
        int currentId = Integer.parseInt(nodeId);

        for (int i = 1; i <= fingerTable.size(); i++) {
            int fingerId = (currentId + (int) Math.pow(2, i - 1)) % 32;
            if (isBetween(fingerId, currentId, newNodeId, false, true)) {
                fingerTable.set(i - 1, newNodeAddress);
            }
        }

        updateReferences(newNodeAddress);

        return "\"OK\"";
    }

    private void updateReferences(String newNodeAddress) {
        String newSuccessor = findSuccessor(newNodeAddress);

        predecessor = findPredecessor(newNodeAddress);
        successor = newSuccessor;
    }

    private String findPredecessor(String address) {
        int targetId = Integer.parseInt(address);
        int currentId = Integer.parseInt(nodeId);

        String predecessorAddress = nodeId;
        int predecessorId = currentId;

        for (int i = fingerTable.size() - 1; i >= 0; i--) {
            int fingerId = Integer.parseInt(fingerTable.get(i));

            if (isBetween(fingerId, currentId, targetId, false, false)) {
                predecessorAddress = fingerTable.get(i);
                predecessorId = fingerId;
            }
        }

        if (!predecessorAddress.equals(nodeId) && isBetween(targetId, predecessorId, currentId, false, false)) {
            String closestPredecessor = lookupPredecessor(targetId);
            if (!closestPredecessor.equals(nodeId)) {
                predecessorAddress = closestPredecessor;
            }
        }

        return predecessorAddress;
    }

    private String lookupPredecessor(int targetId) {
        for (int i = fingerTable.size() - 1; i >= 0; i--) {
            String fingerNode = fingerTable.get(i);
            int fingerNodeId = extractNodeId(fingerNode);
            if (isBetween(targetId, fingerNodeId, extractNodeId(nodeId))) {
                return fingerNode;
            }
        }
        return nodeId;
    }

    private boolean isBetween(int targetId, int fingerNodeId, int i) {
        if (fingerNodeId == Integer.parseInt(nodeId)) {
            return true;
        } else if (fingerNodeId < Integer.parseInt(nodeId)) {
            return false;
        } else {
            return targetId > fingerNodeId || targetId <= Integer.parseInt(nodeId);
        }
    }

    private int extractNodeId(String fingerNode) {
        String[] parts = fingerNode.split("/");
        String lastSegment = parts[parts.length - 1];

        try {
            return Integer.parseInt(lastSegment);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private boolean isBetween(int checkId, int lowerId, int upperId, boolean inclusiveLower, boolean inclusiveUpper) {
        if (inclusiveLower && checkId == lowerId) {
            return true;
        }
        if (inclusiveUpper && checkId == upperId) {
            return true;
        }
        if (lowerId < upperId) {
            return checkId > lowerId && checkId < upperId;
        } else {
            return checkId > lowerId || checkId < upperId;
        }
    }

    private String findSuccessor(String address) {
        int targetId = Integer.parseInt(address);
        int currentId = Integer.parseInt(nodeId);

        if (currentId == targetId) {
            return nodeId;
        }

        for (String s : fingerTable) {
            int fingerId = Integer.parseInt(s);

            if (isBetween(targetId, currentId, fingerId, false, true)) {
                return s;
            }
        }

        String closestSuccessor = lookupSuccessor(targetId);
        if (!closestSuccessor.equals(nodeId)) {
            return closestSuccessor;
        }

        return nodeId;
    }

    private String lookupSuccessor(int targetId) {
        for (String fingerNode : fingerTable) {
            int fingerNodeId = extractNodeId(fingerNode);

            if (isBetween(targetId, extractNodeId(nodeId), fingerNodeId)) {

                return fingerNode;
            }
        }

        return nodeId;
    }

    private static final int m = 5;

    @Override
    @GET
    @Path("/leave")
    @Produces({ MediaType.APPLICATION_JSON })
    public String leave() {
        String successorAddress = findSuccessor(nodeId);

        updateSuccessorPredecessor(successorAddress);

        notifyPredecessor();

        notifySuccessor();

        return "\"OK\"";
    }

    private void updateSuccessorPredecessor(String successorAddress) {
        String predecessorAddress = findPredecessor(successorAddress);
        updatePredecessorReference(successorAddress, predecessorAddress);
    }

    private void notifyPredecessor() {
        String predecessorAddress = findPredecessor(nodeId);
        sendLeaveNotification(predecessorAddress);
    }

    private void notifySuccessor() {
        String successorAddress = findSuccessor(nodeId);
        sendLeaveNotification(successorAddress);
    }

    private void updatePredecessorReference(String successorAddress, String newPredecessorAddress) {
        sendUpdatePredecessorMessage(successorAddress, newPredecessorAddress);
    }

    private void sendLeaveNotification(String destinationAddress) {
        try {

            URL url = new URL(destinationAddress + "/chordnode/leaving");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Leave notification sent successfully to " + destinationAddress);
            } else {
                System.out.println("Failed to send leave notification to " + destinationAddress +
                        ". Response code: " + responseCode);
            }

            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    private void sendUpdatePredecessorMessage(String destinationAddress, String newPredecessorAddress) {
        try {
            URL url = new URL(destinationAddress + "/chordnode/updatePredecessor");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(newPredecessorAddress.getBytes());
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Update predecessor message sent successfully to " + destinationAddress);
            } else {
                System.out.println("Failed to send update predecessor message to " + destinationAddress +
                        ". Response code: " + responseCode);
            }


            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    private String generateRandomNodeId() {
        UUID randomUUID = UUID.randomUUID();
        return randomUUID.toString();
    }

    @Override
    @GET
    @Path("/lookup")
    @Produces({ MediaType.APPLICATION_JSON })
    public String lookup(@QueryParam("key") String key) {
        int targetId = hashKey(key);

        String successorAddress = findSuccessor(Integer.toString(targetId));

        return "\"" + successorAddress + "\"";
    }

    private int hashKey(String key) {
        int hash = 0;

        for (int i = 0; i < key.length(); i++) {
            hash = 31 * hash + key.charAt(i);
        }

        return Math.abs(hash);
    }

    @Override
    @GET
    @Path("/sendMessage")
    @Produces({ MediaType.APPLICATION_JSON })
    public String sendMessage(
            @QueryParam("destinationNode") String destinationNode,
            @QueryParam("message") String message
    ) {
        boolean messageSent = sendMessageToNode(destinationNode, message);

        if (messageSent) {
            return "\"OK\"";
        } else {
            return "\"Error sending message\"";
        }
    }

    private boolean sendMessageToNode(String destinationNode, String message) {
        try {

            URL url = new URL(destinationNode + "/chordnode/sendMessage");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(message.getBytes());
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Message sent to " + destinationNode + ": " + message);
                return true;
            } else {
                System.out.println("Failed to send message to " + destinationNode +
                        ". Response code: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
