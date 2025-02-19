package FTP;

import java.io.*;
import java.net.*;
import java.util.*;

public class Serveur {
    private static final int clientPort = 21;
    private static final String repertoire = "Ressources/";
    private static final Map<String, String> utilisateurs = new HashMap<>();
    private static final Set<String> utilisateursConnectes = new HashSet<>(); //pour garder une liste des utilisateurs actuellement connectés.

    public static void main(String[] args) {
        utilisateurs.put("toto", "password");  // utilisateur autorisé
        System.out.println("Serveur FTP en cours d'exécution sur le port " + clientPort);

        try (ServerSocket serveurSocket = new ServerSocket(clientPort)) {
            while (true) {
                Socket socketClient = serveurSocket.accept();
                new Thread(() -> gererClient(socketClient)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void gererClient(Socket socketClient) {
        try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
             PrintWriter writer = new PrintWriter(socketClient.getOutputStream(), true)) {

            writer.println("220 => Service prêt pour un nouvel utilisateur");
            String repertoireActuel = repertoire;
            String nomUtilisateur = "";

            while (true) {
                String commande = lecteur.readLine();

                String[] elements = commande.split(" ");
                System.out.println(elements[0]);
                String cmd = elements[0].toUpperCase();

                switch (cmd) {
                    case "USER":
                        nomUtilisateur = elements[1]; //element = USER nom_utilisateur, donc on prend le nom d'utisateur
                        writer.println("331 => Utilisateur reconnu, en attente du mot de passe");
                        break;
                    case "PASS":
                        if (utilisateurs.containsKey(nomUtilisateur) && utilisateurs.get(nomUtilisateur).equals(elements[1])) {
                            utilisateursConnectes.add(nomUtilisateur);
                            writer.println("230 Connexion réussie");
                        } else {
                            writer.println("430 Identifiant ou mot de passe incorrect");
                        }
                        break;
                    case "LIST":
                        File dossier = new File(repertoireActuel);
                        String[] fichiers = dossier.list();
                        writer.println("200 OK");
                        for (String fichier : fichiers) {
                            writer.println(fichier);
                        }
                        break;
                    case "CWD":
                        String nouveauRepertoire = elements[1];
                        File tempRepertoire = new File(repertoire + nouveauRepertoire);
                        if (tempRepertoire.exists() && tempRepertoire.isDirectory()) {
                            repertoireActuel = tempRepertoire.getPath();
                            writer.println("200 Répertoire changé");
                        } else {
                            writer.println("501 Répertoire invalide");
                        }
                        break;
                    case "RETR":
                        String nomFichier = elements[1];
                        File fichier = new File(repertoireActuel + "/" + nomFichier);
                        if (fichier.exists() && fichier.isFile()) {
                            writer.println("150 Ouverture de la connexion pour le transfert du fichier");
                            transfererFichier(fichier, socketClient);
                            writer.println("2xx Le transfert a été fait avec succes");
                        } else {
                            writer.println("501 Erreur de syntaxe");
                        }
                        break;
                    case "QUIT":
                        writer.println("221 Au revoir");
                        utilisateursConnectes.remove(nomUtilisateur);
                        socketClient.close();
                        return;
                    default:
                        writer.println("502 Commande non implémentée");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void transfererFichier(File fichier, Socket socket) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fichier));
             OutputStream os = socket.getOutputStream()) {
            byte[] tampon = new byte[1024];
            int octetsLus;
            while ((octetsLus = bis.read(tampon)) != -1) {
                os.write(tampon, 0, octetsLus);
            }
        }
    }
}
