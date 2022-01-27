import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
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
                System.out.println(round);
                ArrayList<Integer> dist_vec_to_send = this.distance_vector.get(round);
                STCP send_massage = new STCP(clientSocket, dist_vec_to_send, true);
                send_massage.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
class STCP extends Thread {
    protected ArrayList<Integer> msg_to_send;
    protected Socket client_socket;
    protected boolean required_dist_vec;

    public STCP(Socket client_socket, ArrayList<Integer> msg_to_send, boolean dist){
        this.msg_to_send = msg_to_send;
        this.client_socket = client_socket;
        this.required_dist_vec = dist;
    }
    public void run(){
        DataOutputStream input;
        if (required_dist_vec)
        {
            for (int i = 0; i < this.msg_to_send.size(); i++) {
                try {
                    input = new DataOutputStream(this.client_socket.getOutputStream());
                    input.writeInt(this.msg_to_send.get(i));
                    input.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            try {
                input = new DataOutputStream(this.client_socket.getOutputStream());
                input.writeInt(this.msg_to_send.get(0));
                input.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



