import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class UDP extends Thread
{
    protected DatagramPacket packet;
    private byte[] buf = new byte[4096];
    private String tableFilePrefix;
    private String forwardingFilePrefix;
    private int name;
    private int number_of_routers = 0;
    private Routing[]  routing_table;
    private String received;
    InetAddress address;
    int port;
    private DatagramSocket udp_socket;
    private ServerSocket tcp_socket;
    private HashMap<Integer,Neighbore> neighbors;
    private int update_number;

    public UDP(DatagramPacket packet, byte[] buf, String tableFilePrefix, String forwardingFilePrefix, int name,
               int number_of_routers, Routing[]  routing_table, String received, InetAddress address, int port,
               DatagramSocket socket, ServerSocket tcp_Socket, HashMap<Integer,Neighbore> neighbors, int update_number)
    {
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
        this.update_number = update_number;
    }
    public void run(){
        if (this.received.equals(MyConstants.PRINT_ROUTING_TABLE)) {
            try {
                 print_routing_table();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(this.received.equals(MyConstants.SHUT_DOWN)){
            try{
                shut_down();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        if(this.received.equals(MyConstants.UPDATE_ROUTING_TABLE)){
            try{
                update_routing_table();
            } catch (IOException e){
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

    private void update_routing_table() throws IOException{
        Socket clientSocket = this.tcp_socket.accept();
        ArrayList<Integer> message = new ArrayList<>();
        message.add(this.update_number);
        STCP send_massage = new STCP(clientSocket, message, false);
        send_massage.start();
    }
    private void shut_down() throws IOException
    {

    }

    public void forward() throws IOException {
        File myObj = new File(this.forwardingFilePrefix + this.name + ".txt");
        myObj.createNewFile();
        FileWriter myWriter = new FileWriter(this.forwardingFilePrefix + this.name + ".txt", true);
        myWriter.write(this.received + "\n");
        myWriter.close();
        String[] received_split = received.split(";");
        int hops = Integer.parseInt(received_split[2])-1;
        int dest = Integer.parseInt(received_split[1]);
        if (hops!=0 & dest != this.name) {
            received_split[2] = String.valueOf(hops);
            String message_to_send = String.join(";", received_split);
            Routing line = this.routing_table[dest-1];
            String ip = this.neighbors.get(line.next).ip_router;
            int port = this.neighbors.get(line. next).udp_port_neighbore;
            byte[] bytesToSend = message_to_send.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packetToSend = new DatagramPacket(bytesToSend, bytesToSend.length, InetAddress.getByName(ip), port);
            this.udp_socket.send(packetToSend);
        }
        else{
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
        for (int i=0;i<this.number_of_routers;i++) {
            if (i+1==this.name)
                line = String.valueOf(this.routing_table[i].dist) +";None";
            else
                line = String.valueOf(this.routing_table[i].dist) +";" + String.valueOf(this.routing_table[i].next);
            myWriter.write(line + "\n");
        }
        myWriter.close();
        byte[] bytesToSend = "FINISH".getBytes(StandardCharsets.UTF_8);
        DatagramPacket packetToSend = new DatagramPacket(bytesToSend, bytesToSend.length, this.address, this.port);
        this.udp_socket.send(packetToSend);

    }
}
