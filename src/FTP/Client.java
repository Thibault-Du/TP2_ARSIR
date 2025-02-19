package FTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public Client(String serverAddress, int serverPort) {

        try {
            // Initialisation du socket et des flux
            this.socket = new Socket(serverAddress, serverPort);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Erreur lors de la connexion au serveur : " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void receiveMessage() {
        try {
            while(true){
                String serverResponse = reader.readLine();
                System.out.println("Réponse du serveur : " + serverResponse);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void start(){
        try {
            System.out.println("Adresse IP du client : " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Client démarré...");

            Thread receivingThread = new Thread(this::receiveMessage);
            receivingThread.start();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Entrez votre message (ou 'quit' pour terminer) : ");

                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("quit")) {
                    break;
                }
                writer.println(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1"; // Adresse du serveur
        int serverPort = 6789; // Port du serveur

        Client client = new Client(serverAddress, serverPort);
        client.start();
    }
}