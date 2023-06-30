
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;

import netscape.javascript.JSObject;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import javax.swing.*;

public class Server extends WebSocketServer {

    ArrayList<GameThread> gameThreads = new ArrayList<GameThread>();

    public static final String UPDATED_LISTE = "01";

    public static final String CREATED_SUCCESS = "02";

    public static final String FORCE_CLOSE = "03";

    public static final String CONNECTION_ACCEPTED = "04";

    public static final String BUTTON_REMOVE = "05";

    public static final String GAME_EVENT = "06";
    private JPanel panel1;
    private JTextArea textArea1;


    public Server(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    public Server(InetSocketAddress address) {
        super(address);
    }

    public Server(int port, Draft_6455 draft) {
        super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
    }


    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {/*
        conn.send("Welcome to the server!"); //This method sends a message to the new client
        broadcast("new connection: " + handshake
                .getResourceDescriptor()); //This method sends a message to all clients connected
        */
        System.out.println(
                conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        broadcast(conn + " has left the room!");
        closeAssociatedThread(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        //broadcast(message);
        textArea1.append("got: " + message + "\n");
        textArea1.setCaretPosition(textArea1.getDocument().getLength());
        switchRoutes(message, conn);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        //broadcast(message.array());
        //System.out.println(conn + ": " + message);
    }


    public static void main(String[] args) throws InterruptedException, IOException {
        int port = 8887;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        Server s = new Server(port);
        s.start();
        System.out.printf("Serveur lancé au port: %n", s.getPort());

        BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String in = sysin.readLine();
            s.broadcast(in);
            if (in.equals("exit")) {
                s.stop(1000);
                break;
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
        }
    }

    @Override
    public void onStart() {
        System.out.println("Server started!");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }


    public void switchRoutes(String message, WebSocket conn) {
        switch (message.substring(0, 2)) {
            case "01": //Demande création de partie
                try {
                    if (!checkIfExisting(conn)) {
                        GameThread gameThread = new GameThread(conn, this);
                        this.gameThreads.add(gameThread);
                        broadcast(turnThreadToString(gameThread));

                        ArrayList<WebSocket> coll = new ArrayList<>();
                        coll.add(conn);
                        broadcast(String.format("%s_%S", CREATED_SUCCESS, conn), coll);
                    } else {
                        System.out.println(conn.toString() + "a essayé d'être à plusieurs endroits");
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "02"://tentative de join
                try {
                    GameThread thread = GetUnknown(message);
                    if (thread != null) {
                        if (thread.getOtherPlayer() != conn && thread.getGameHost() != conn && thread.getOtherPlayer() == null) {
                            thread.setOtherPlayer(conn);
                            ArrayList<WebSocket> coll = new ArrayList<>();
                            coll.add(conn);
                            coll.add(thread.getGameHost());
                            broadcast(String.format("%s_%s", CONNECTION_ACCEPTED, message), coll);
                            thread.start();
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    break;
                }
            case "03":
                closeAssociatedThread(conn);
                break;
            case "04"://Game events
                switchGame(message, conn);
                break;
            default:
                break;
        }
    }

    private void switchGame(String message, WebSocket conn) {
        System.out.println("Switch game : " + message.substring(3, 5));
        switch (message.substring(3, 5)) {
            case GameThread.RELANCER:
                relancerPartie(conn);
                break;
            case GameThread.NEW_JETON:
                addJETON(conn, message);
            default:
                break;
        }
    }


    private void addJETON(WebSocket conn, String message) {
        for (GameThread gameThread : gameThreads) {
            if (Objects.equals(gameThread.getIdentifiant(), conn.toString())) {
                gameThread.addJETONToThread(message, conn);
                break;
            } else if (gameThread.getOtherPlayer() != null) {
                if (Objects.equals(gameThread.getOtherPlayer().toString(), conn.toString())) {
                    gameThread.addJETONToThread(message, conn);
                    break;
                }
            }
        }
    }

    private void relancerPartie(WebSocket conn) {
        for (GameThread gameThread : gameThreads) {
            if (Objects.equals(gameThread.getIdentifiant(), conn.toString())) {
                gameThread.relancerPartie();
                break;
            } else if (gameThread.getOtherPlayer() != null) {
                if (Objects.equals(gameThread.getOtherPlayer().toString(), conn.toString())) {
                    gameThread.relancerPartie();
                    break;
                }
            }
        }
    }

    private String turnThreadToString(GameThread gameThread) {
        return UPDATED_LISTE + "-id:" + gameThread.getIdentifiant() + "_players:" + gameThread.getNumPlayers();
    }

    private GameThread GetUnknown(String message) {
        for (GameThread num : gameThreads) {
            if (Objects.equals(num.getIdentifiant().substring(33), message.substring(3))) {
                return num;
            }
        }
        return null;
    }

    private boolean checkIfExisting(WebSocket conn) {
        for (GameThread num : gameThreads) {
            if (Objects.equals(num.getIdentifiant(), conn.toString())) {
                return true;
            }
        }
        return false;
    }


    public void closeAssociatedThread(WebSocket conn)
    //Oui on peut essayer de destroy le thread, cependant c'est déconseillé par la doc
    //Donc je fais juste ça, sur le long terme ça cassera(ça causera un thread/memory leak), sur ma prochaine application je tenterai juste de faire fermer arrêter le run()
    {
        System.out.println(gameThreads.size());
        for (GameThread gameThread : gameThreads) {
            if (Objects.equals(gameThread.getIdentifiant(), conn.toString())) {
                gameThread.interrupt();
                gameThreads.remove(gameThread);
                break;
            } else if (gameThread.getOtherPlayer() != null) {
                if (Objects.equals(gameThread.getOtherPlayer().toString(), conn.toString())) {
                    gameThread.interrupt();
                    gameThreads.remove(gameThread);
                    break;
                }
            }
        }
        System.out.println(gameThreads.size());
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        textArea1 = new JTextArea();
        textArea1.setPreferredSize(new Dimension(200, 100));
        panel1.add(textArea1);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}
