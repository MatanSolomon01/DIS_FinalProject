import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class UDP extends Thread {
    protected DatagramPacket packet;
    private byte[] buf = new byte[4096];
    private String tableFilePrefix;
    private String forwardingFilePrefix;
    private int name;
    private int number_of_routers = 0;
    private Routing[] routing_table;
    private String received;
    InetAddress address;
    int port;
    private DatagramSocket udp_socket;
    private ServerSocket tcp_socket;
    private HashMap<Integer, Neighbore> neighbors;
    private int round_num;
    protected HashMap<Integer, ArrayList<Integer>> distance_vector;
    private int neighbors_amount;

    public UDP(DatagramPacket packet, byte[] buf, String tableFilePrefix,
               String forwardingFilePrefix, int name, int number_of_routers,
               Routing[] routing_table, String received, InetAddress address,
               int port, DatagramSocket socket, ServerSocket tcp_Socket,
               HashMap<Integer, Neighbore> neighbors,
               int round_num,
               HashMap<Integer, ArrayList<Integer>> distance_vector,
               int neighbors_amount) {
        this.packet = packet;
        this.buf = buf;
        this.tableFilePrefix = tableFilePrefix;
        this.forwardingFilePrefix = forwardingFilePrefix;
        this.name = name;
        this.number_of_routers = number_of_routers;
        this.routing_table = routing_table;
        this.received = received;
        this.address = address;
        this.port = port;
        this.udp_socket = socket;
        this.tcp_socket = tcp_Socket;
        this.neighbors = neighbors;
        this.round_num = round_num;
        this.distance_vector = distance_vector;
        this.neighbors_amount = neighbors_amount;
    }

    public void run() {
        if (this.received.equals(MyConstants.PRINT_ROUTING_TABLE)) {
            try {
                print_routing_table();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (this.received.equals(MyConstants.UPDATE_ROUTING_TABLE)) {
            try {
                update_routing_table();

                // Send Finish After updating table
                byte[] bytesToSend = "FINISH".getBytes(StandardCharsets.UTF_8);
                DatagramPacket packetToSend = new DatagramPacket(bytesToSend, bytesToSend.length, this.address, this.port);
                this.udp_socket.send(packetToSend);


            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }


        String[] received_split = received.split(";");
        if (received_split[0].equals(MyConstants.FORWARD)) {
            try {
                forward();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void update_routing_table() throws IOException, InterruptedException {

        // Get new edge weights
        for (HashMap.Entry<Integer, Neighbore> entry : this.neighbors.entrySet()) {
            Integer n_name = entry.getKey();
            Neighbore neighbore = entry.getValue();

            int new_weight = CreateInput.weightsMatrix[this.name][n_name][this.round_num];
            if (new_weight == -1) continue;
            int old_weight = neighbore.edge_weight;
            neighbore.edge_weight = new_weight;
            for (int index = 0; index < this.routing_table.length; index++) {
                if (index != this.name) {
                    Routing routing = this.routing_table[index];
                    if (routing.next == n_name) {
                        routing.dist += (new_weight - old_weight);
                    }
                }
            }
        }

        CountDownLatch cdl = new CountDownLatch(this.neighbors_amount);
        // Ask and receive vectors from neighbors
        HashMap<Integer, Request_table_TCP> neighbors_vectors = new HashMap<Integer, Request_table_TCP>();
        for (HashMap.Entry<Integer, Neighbore> entry : this.neighbors.entrySet()) {
            Integer n_name = entry.getKey();
            Neighbore neighbore = entry.getValue();

            InetAddress address = InetAddress.getByName(neighbore.ip_router);
            Socket tcp_sender = new Socket(address, neighbore.tcp_port_neighbore);
            Request_table_TCP request = new Request_table_TCP(tcp_sender, this.round_num, cdl);
            request.start();
            neighbors_vectors.put(n_name, request);
        }

        cdl.await();
        // Update the routing table
        ArrayList<Integer> temp_distance_vector = new ArrayList<Integer>();
        for (int index = 0; index < this.routing_table.length; index++) {
            if (index != this.name) {
                int min_dist = Integer.MAX_VALUE;
                int min_name = 0;
                for (HashMap.Entry<Integer, Request_table_TCP> entry : neighbors_vectors.entrySet()) {
                    Integer n_name = entry.getKey();
                    ArrayList<Integer> neighbor_vec = entry.getValue().result_vector;
                    int dist = neighbor_vec.get(index);
                    dist += this.neighbors.get(n_name).edge_weight;
                    if (dist < min_dist) {
                        min_dist = dist;
                        min_name = n_name;
                    }
                }
                Routing index_routing = this.routing_table[index];
                index_routing.dist = min_dist;
                index_routing.next = min_name;
                temp_distance_vector.add(min_dist);
            } else temp_distance_vector.add(0);

            // Add the new distance vector
            this.distance_vector.put(this.round_num+1, temp_distance_vector);
        }
    }

    public void forward() throws IOException {
        File myObj = new File(this.forwardingFilePrefix + this.name + ".txt");
        myObj.createNewFile();
        FileWriter myWriter = new FileWriter(this.forwardingFilePrefix + this.name + ".txt", true);
        myWriter.write(this.received + "\n");
        myWriter.close();
        String[] received_split = received.split(";");
        int hops = Integer.parseInt(received_split[2]) - 1;
        int dest = Integer.parseInt(received_split[1]);
        if (hops != 0 & dest != this.name) {
            received_split[2] = String.valueOf(hops);
            String message_to_send = String.join(";", received_split);
            Routing line = this.routing_table[dest - 1];
            String ip = this.neighbors.get(line.next).ip_router;
            int port = this.neighbors.get(line.next).udp_port_neighbore;
            byte[] bytesToSend = message_to_send.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packetToSend = new DatagramPacket(bytesToSend, bytesToSend.length, InetAddress.getByName(ip), port);
            this.udp_socket.send(packetToSend);
        } else {
            String message = received_split[3];
            byte[] bytesToSend = message.getBytes(StandardCharsets.UTF_8);
            String ip = received_split[4];
            int port = Integer.parseInt(received_split[5]);
            DatagramPacket packetToSend = new DatagramPacket(bytesToSend, bytesToSend.length, InetAddress.getByName(ip), port);
            this.udp_socket.send(packetToSend);
        }


    }

    public void print_routing_table() throws IOException {
        File myObj = new File(this.tableFilePrefix + this.name + ".txt");
        myObj.createNewFile();
        FileWriter myWriter = new FileWriter(this.tableFilePrefix + this.name + ".txt", true);
        String line;
        for (int i = 0; i < this.number_of_routers; i++) {
            if (i + 1 == this.name)
                line = String.valueOf(this.routing_table[i].dist) + ";None";
            else
                line = String.valueOf(this.routing_table[i].dist) + ";" + String.valueOf(this.routing_table[i].next);
            myWriter.write(line + "\n");
        }
        myWriter.close();
        byte[] bytesToSend = "FINISH".getBytes(StandardCharsets.UTF_8);
        DatagramPacket packetToSend = new DatagramPacket(bytesToSend, bytesToSend.length, this.address, this.port);
        this.udp_socket.send(packetToSend);

    }
}
