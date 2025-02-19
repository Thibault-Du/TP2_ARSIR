package FTP;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Serveur {
    private static final int clientPort = 21;
    private static final String repertoire = "Ressources/";
    private static final Map<String, String> utilisateurs = new HashMap<>();
    private static final Set<String> utilisateursConnectes = new HashSet<>(); //pour garder une liste des utilisateurs actuellement connectés.
    private static BufferedReader lecteur;
    private static PrintWriter writer;
    private static String nomUtilisateur = "";



    public static void main(String[] args) {
        Serveur s = new Serveur();
    }

    public Serveur() {
        utilisateurs.put("toto", "password");
        utilisateurs.put("anonymous", "*");// utilisateur autorisé
        System.out.println("Serveur FTP en cours d'exécution sur le port " + clientPort);

        try (ServerSocket serveurSocket = new ServerSocket(clientPort)) {
            while (true) {
                Socket socketClient = serveurSocket.accept();
                lecteur = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
                writer = new PrintWriter(socketClient.getOutputStream(), true);
                new Thread(() -> gererClient(socketClient)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void gererClient(Socket socketClient) {
        try {

            writer.println("220 => Service prêt pour un nouvel utilisateur");
            String repertoireActuel = repertoire;

            while (true) {
                String commande = lecteur.readLine();

                String[] elements = commande.split(" ");
                System.out.println(elements[0]);
                String cmd = elements[0].toUpperCase();

                switch (cmd) {
                    case "USER":
                        this.userCommand(elements[1]);
                        break;
                    case "PASS":
                        this.passCommand(elements[1]);
                        break;
                    case "DIR":
                        System.out.println("Contents of the directory");
                        Path directory = Paths.get(repertoireActuel);
                        List<String> tableauRepertoire = new ArrayList<>();

                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                            for (Path file : stream) {
                                tableauRepertoire.add(file.getFileName().toString()); // Ajouter à la liste
                            }
                        } catch (IOException e) {
                            writer.println("550 => Erreur lors de la lecture du répertoire : " + e.getMessage());
                            break;
                        }

                        // Vérifier si le dossier est vide
                        if (tableauRepertoire.isEmpty()) {
                            writer.println("226 => Aucun fichier trouvé dans le répertoire.");
                        } else {
                            writer.println("226 => Liste des fichiers : " + String.join(", ", tableauRepertoire));
                        }
                        break;
                    case "CWD":
                        String nouveauRepertoire = elements[1];
                        File tempRepertoire = new File(repertoire + nouveauRepertoire);
                        if (tempRepertoire.exists() && tempRepertoire.isDirectory()) {
                            repertoireActuel = tempRepertoire.getPath();
                            writer.println("200 => Répertoire changé");
                        } else {
                            writer.println("501 => Répertoire invalide");
                        }
                        break;
                    case "PASV":
                        // Vérifier que la commande contient bien un nom de fichier
                        if (elements.length < 2) {
                            writer.println("501 => Erreur de syntaxe : fichier non spécifié");
                            break;
                        }

                        String nomFichier = elements[1];
                        File fichier = new File(repertoireActuel, nomFichier);

                        // Vérifier si le fichier existe et est lisible
                        if (!fichier.exists()) {
                            writer.println("550 => Le fichier n'existe pas");
                            break;
                        }
                        if (!fichier.isFile()) {
                            writer.println("550 => Ce n'est pas un fichier valide");
                            break;
                        }
                        if (!fichier.canRead()) {
                            writer.println("550 => Permission refusée pour lire le fichier");
                            break;
                        }

                        // Ouvrir une connexion pour le transfert du fichier
                        writer.println("150 => Ouverture de la connexion pour le transfert du fichier");
                        writer.flush(); // S'assurer que le message est envoyé immédiatement

                        // Création d'une nouvelle connexion de données
                        try (ServerSocket dataServerSocket = new ServerSocket(0)) { // Utilise un port aléatoire disponible
                            int dataPort = dataServerSocket.getLocalPort();
                            writer.println("125 => Utilisation du port " + dataPort + " pour le transfert de fichier");

                            try (Socket dataSocket = dataServerSocket.accept()) {
                                boolean transfertReussi = transfererFichier(fichier, dataSocket);

                                if (transfertReussi) {
                                    writer.println("226 => Le transfert a été fait avec succès");
                                } else {
                                    writer.println("426 => Échec du transfert");
                                }
                            }
                        } catch (IOException e) {
                            writer.println("425 => Impossible d'ouvrir une connexion de données");
                            e.printStackTrace();
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

    private void userCommand(String userName){
        nomUtilisateur = userName;
        if (utilisateurs.containsKey(userName)){
            writer.println("331 => Utilisateur reconnu, en attente du mot de passe");
        } else {
            writer.println("431 => Utilisateur non reconnu, entrez un autre utilisateur");
        }
    }

    private void passCommand(String password){
        if ((utilisateurs.containsKey(nomUtilisateur) && utilisateurs.get(nomUtilisateur).equals(password)) || nomUtilisateur.equals("anonymous")) {
            utilisateursConnectes.add(nomUtilisateur);
            writer.println("230 Connexion réussie");
        } else {
            writer.println("430 Identifiant ou mot de passe incorrect");
        }
    }

    private static boolean transfererFichier(File fichier, Socket dataSocket) {
        try (FileInputStream fis = new FileInputStream(fichier);
             OutputStream os = dataSocket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush(); // S'assurer que toutes les données sont bien envoyées

            return true; // Indiquer que le transfert a réussi
        } catch (IOException e) {
            System.out.println("Erreur lors du transfert du fichier : " + e.getMessage());
            return false; // Indiquer que le transfert a échoué
        }
    }

}
