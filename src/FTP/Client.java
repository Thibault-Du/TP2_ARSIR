package FTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private String serverAddress;
    private int serverPort;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void requestTime() {
        try (Socket socket = new Socket(serverAddress, serverPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            // Utilisation du Scanner pour que le client entre son choix
            Scanner sc = new Scanner(System.in);
            System.out.print("Que voulez-vous afficher: (DATE, HOUR, FULL): ");
            String message = sc.nextLine();  // Le message est lu depuis l'entrée de l'utilisateur

            /*while (!message.isEmpty() && !message.equals("CLOSE") && (!message.equals("DATE") || !message.equals("HOUR")|| !message.equals("FULL"))) {
                System.out.print("Message non-reconnu!");
                message = "";
                System.out.print("Utilisez DATE pour la date, HOUR pour l'heure, FULL pour les deux ou CLOSE pour quitter: ");

                System.out.print("Que voulez-vous afficher: (DATE, HOUR, FULL): ");
                message = sc.nextLine();  // Le message est lu depuis l'entrée de l'utilisateur
            }*/


            // Envoi de la demande au serveur
            writer.println(message);

            // Lecture et affichage de la réponse du serveur
            String serverResponse = reader.readLine();
            System.out.println("Réponse du serveur : " + serverResponse);

            // On peut envoyer d'autres demandes ici
            writer.println("CLOSE"); // Fermer la connexion après la demande
            serverResponse = reader.readLine();
            System.out.println("Réponse du serveur : " + serverResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1"; // Adresse du serveur
        int serverPort = 6789; // Port du serveur

        Client client = new Client(serverAddress, serverPort);
        client.requestTime();
    }
}