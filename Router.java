import java.net.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Router extends Thread {
    private int name;
    private int port_udp;
    private int port_tcp;
    private int number_of_routers;
    private HashMap<Integer, Neighbore> neighbors;
    private int neighbors_amount;
    private int diameter;
    private HashMap<Integer, ArrayList<Integer>> distance_vector;
    private Routing[] routing_table;
    private byte[] buf = new byte[4096];
    private String tableFilePrefix;
    private String forwardingFilePrefix;
    private int first_neighbor;
    private int round_num;

    public Router(int name, String inputFilePrefix, String tableFilePrefix, String
            forwardingFilePrefix) {
        try {
            this.tableFilePrefix = tableFilePrefix;
            this.forwardingFilePrefix = forwardingFilePrefix;
            String file_name = inputFilePrefix + name + ".txt";
            Scanner scanner = new Scanner(new File(file_name));
            String neighbore_ip;
            int neighbore_name, neighbore_udp, neighbore_tcp, edge_weight;
            this.name = name;
            this.neighbors = new HashMap<Integer, Neighbore>();
            this.port_udp = Integer.parseInt(scanner.nextLine());
            this.port_tcp = Integer.parseInt(scanner.nextLine());
            this.number_of_routers = Integer.parseInt(scanner.nextLine());
            int counter = 0;
            String next;
            while (scanner.hasNextLine())  // Neighbors
            {
                next = scanner.nextLine();
                if (next.equals("*"))
                    break;

                neighbore_name = Integer.parseInt(next);
                neighbore_ip = scanner.nextLine();
                neighbore_udp = Integer.parseInt(scanner.nextLine());
                neighbore_tcp = Integer.parseInt(scanner.nextLine());
                edge_weight = Integer.parseInt(scanner.nextLine());
                Neighbore n = new Neighbore(name, neighbore_name, neighbore_ip, neighbore_udp, neighbore_tcp, edge_weight);
                this.neighbors.put(neighbore_name, n);
                if (counter == 0)
                    this.first_neighbor = neighbore_name;
                counter++;
            }
            this.neighbors_amount = counter;
            this.diameter = Integer.parseInt(scanner.nextLine());
            scanner.close();
            this.distance_vector = new HashMap<Integer, ArrayList<Integer>>();
            ArrayList<Integer> temp_distance_vector = new ArrayList<Integer>();
            this.routing_table = new Routing[this.number_of_routers];
            Routing rout;
            for (int i = 0; i < this.number_of_routers; i++) {
                if (i + 1 == this.name)
                    rout = new Routing(0, this.first_neighbor);
                else
                    rout = new Routing(this.diameter, this.first_neighbor);
                this.routing_table[i] = rout;
                temp_distance_vector.add(rout.dist);
            }
            this.distance_vector.put(1, temp_distance_vector);
            this.round_num = 1;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        InetAddress address;
        int port;
        String received;
        try {
            DatagramPacket packet = new DatagramPacket(this.buf, this.buf.length);
            DatagramSocket udp_socket = new DatagramSocket(this.port_udp);
            ServerSocket tcp_Socket = new ServerSocket(this.port_tcp);


            TCP l_tcp = new TCP(tcp_Socket, this.distance_vector);
            l_tcp.start();


            while (true) {
                udp_socket.receive(packet);
                address = packet.getAddress();
                port = packet.getPort();
                received = new String(packet.getData(), 0, packet.getLength());
                received = received.replaceAll(" ", "");
                System.out.println(received);
                if (received.equals(MyConstants.SHUT_DOWN)) {
                    tcp_Socket.close();
                    udp_socket.close();
                    break;
                }

                if (received.equals(MyConstants.UPDATE_ROUTING_TABLE)) {
                    UDP s_udp = new UDP(packet, this.buf, this.tableFilePrefix, this.forwardingFilePrefix, name,
                            number_of_routers, routing_table, received, address, port, udp_socket, tcp_Socket,
                            this.neighbors, this.round_num, this.distance_vector, this.neighbors_amount);
                    s_udp.start();
                    this.round_num++;
                }
                if (received.equals(MyConstants.PRINT_ROUTING_TABLE)) {
                    UDP s_udp = new UDP(packet, this.buf, this.tableFilePrefix, this.forwardingFilePrefix, name,
                            number_of_routers, routing_table, received, address, port, udp_socket, tcp_Socket,
                            this.neighbors, this.round_num, this.distance_vector, this.neighbors_amount);
                    s_udp.start();
                } else {
                    String[] received_split = received.split(";");
                    if (received_split[0].equals("FORWARD")) {
                        UDP s_udp = new UDP
                                (packet, this.buf, this.tableFilePrefix, this.forwardingFilePrefix, name,
                                        number_of_routers, routing_table, received, address, port, udp_socket, tcp_Socket,
                                        this.neighbors, this.round_num, this.distance_vector, this.neighbors_amount);
                        s_udp.start();

                    }
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}


