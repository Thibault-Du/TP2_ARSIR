package FTP;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Serveur {
    private ServerSocket serverSocket;

    public Serveur(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void start() {
        try {
            System.out.println("Serveur démarré...");
            System.out.println("Adresse IP du serveur : " + InetAddress.getLocalHost().getHostAddress());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté: " + clientSocket.getInetAddress().getHostAddress());

                InputStream inputStream = clientSocket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                OutputStream outputStream = clientSocket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);

                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    if (clientMessage.equals("CLOSE")) {
                        writer.println("Connexion fermée.");
                        break; // Sort de la boucle si le client envoie "CLOSE"
                    }
                    // Repondre en fonction du message du client
                    LocalDateTime currentTime = LocalDateTime.now();
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

                    if (clientMessage.equals("DATE")) {
                        writer.println("Date : " + currentTime.format(dateFormatter));
                    } else if (clientMessage.equals("HOUR")) {
                        writer.println("Heure : " + currentTime.format(timeFormatter));
                    } else if (clientMessage.equals("FULL")) {
                        writer.println("Date et Heure : " + currentTime.format(dateFormatter) + " " + currentTime.format(timeFormatter));
                    } else {
                        writer.println("Message non reconnu! Utilisez DATE, HOUR, FULL ou CLOSE.");
                    }
                }

                writer.close();
                reader.close();
                clientSocket.close();
                System.out.println("Connexion fermée avec le client.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            int port = 6789; // Port du serveur
            Serveur serveur = new Serveur(port);
            serveur.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}