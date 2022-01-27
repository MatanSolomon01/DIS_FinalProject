import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

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
                Send_table_TCP send_massage = new Send_table_TCP(clientSocket, dist_vec_to_send);
                send_massage.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class Send_table_TCP extends Thread {
    protected ArrayList<Integer> msg_to_send;
    protected Socket client_socket;


    public Send_table_TCP(Socket client_socket, ArrayList<Integer> msg_to_send) {
        this.msg_to_send = msg_to_send;
        this.client_socket = client_socket;

    }

    public void run() {
        DataOutputStream input;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < this.msg_to_send.size(); i++) {
            if (i != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(this.msg_to_send.get(i));
        }
        String msg_to_send_string = stringBuilder.toString();

        try {
            input = new DataOutputStream(this.client_socket.getOutputStream());
            input.writeUTF(msg_to_send_string);
            input.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Request_table_TCP extends Thread {
    protected int round;
    protected Socket client_socket;
    protected ArrayList<Integer> result_vector;
    protected CountDownLatch cdl;

    public Request_table_TCP(Socket client_socket, int round, CountDownLatch cdl) {
        this.round = round;
        this.client_socket = client_socket;
        this.cdl = cdl;
    }

    public void run() {
        DataOutputStream output;
        DataInputStream input;

        try {
            output = new DataOutputStream(this.client_socket.getOutputStream());
            output.writeUTF(String.valueOf(this.round));
            output.flush();

            input = new DataInputStream(this.client_socket.getInputStream());
            String message = "";
            try {
                message = input.readUTF();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            this.client_socket.close();
            assert !(message.equals(""));
            cdl.countDown();
            String[] message_splited = message.split(",");
            this.result_vector = new ArrayList<>();
            for (String str : message_splited)
                this.result_vector.add(Integer.valueOf(str));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


