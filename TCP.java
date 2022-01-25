import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class TCP extends Thread {
    protected ServerSocket tcp_socket;
    protected HashMap<Integer, ArrayList<Integer>> distance_vector;


    public TCP(ServerSocket tcp_socket, HashMap<Integer, ArrayList<Integer>> distance_vector) {
        this.tcp_socket = tcp_socket;
        this.distance_vector = distance_vector;

    }

    public void run() {
        Socket clientSocket;

        while (true) {
            try {
                clientSocket = this.tcp_socket.accept();
                DataInputStream stream = new DataInputStream(clientSocket.getInputStream());
                int round = Integer.parseInt(stream.readUTF());

//                this.distance_vector.get(round)

//                DataOutputStream input = new DataOutputStream(this.clientSocket.getOutputStream());
//                input.writeUTF(this.routing_table);
//                input.flush();



            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
