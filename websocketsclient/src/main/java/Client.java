import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

public class Client extends JFrame {

    public static final String CODE_NEW_GAME = "01";

    public static final String CODE_JOIN_GAME = "02";

    public static final String CLOSE_GAME = "03";

    public static final String GAME_ACTION = "04";

    private static final long serialVersionUID = -6056260699202978657L;

    private JPanel ContentPanel;
    private JPanel GameFrame;
    private JPanel Lobby;
    private JPanel main;
    private JLabel connected;
    private JPanel disconnected;
    private JButton seReconnecterButton;
    private JLabel placeholder;
    private JPanel ServerList;
    private JPanel Options;
    private JButton creerpartie;
    private JPanel WaitingFrame;
    private JLabel gameIdWAIT;
    private JPanel MainPanel;
    private JPanel SecondaryPanel;
    private JButton button0;
    private JButton button2;
    private JButton button3;
    private JButton button4;
    private JButton button5;
    private JButton button6;
    private JButton button1;
    private JLabel labelGAMEFRAME;
    private JButton relancerButton;
    private JButton quitter;

    private HashMap buttonsServerMap;

    private final String location = "ws://localhost:8887";

    private WebSocketClient cc;

    private int x = -1;

    private int tour;

    private final Boolean[][] tableau = new Boolean[6][7];

    public Client() {
        $$$setupUI$$$();
        this.setContentPane(main);
        this.setTitle("Client Puissance4");
        this.setSize(800, 800);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        CardLayout cl = this.getCardLayout();
        cl.show(ContentPanel, "disconnected");
        this.initiateButtons();
        this.createComponentMap();
        Connect();
        this.setResizable(false);
        this.setVisible(true);
    }

    private CardLayout getCardLayout() {
        CardLayout cl = (CardLayout) (ContentPanel.getLayout());
        return cl;
    }


    public static void main(String[] args) {
        new Client();
    }

    private void Connect() {
        try {
            connected.setText("Tentative de connection");
            cc = new WebSocketClient(new URI(this.location), new Draft_6455()) {

                @Override
                public void onMessage(String message) {
                    Switch(message);
                }

                @Override
                public void onOpen(ServerHandshake handshake) {
                    connected.setText(String.format("Vous êtes connecté sur %s, informations : %s", getURI(), cc.getSocket()));
                    CardLayout cl = getCardLayout();
                    cl.show(ContentPanel, "lobby");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    placeholder.setText(String.format("Vous avez été déconnecté, code : %s, raison : %s", code, reason));
                    CardLayout cl = getCardLayout();
                    cl.show(ContentPanel, "disconnected");
                }

                @Override
                public void onError(Exception ex) {
                    placeholder.setText(String.format("Un erreur est survenue : %s", ex));
                    CardLayout cl = getCardLayout();
                    cl.show(ContentPanel, "disconnected");
                }
            };

            cc.connect();
        } catch (Exception ex) {
            placeholder.setText(String.format("Un erreur est survenue : %s", ex));
            CardLayout cl = getCardLayout();
            cl.show(ContentPanel, "disconnected");
        }
    }

    private void initiateButtons() {
        seReconnecterButton.addActionListener((ActionEvent e) -> Connect());
        ArrayList<JButton> boutons = getAllActionButtons();
        for (int x = 0; x < 7; x++) {
            int finalX = x;
            boutons.get(x).addActionListener((ActionEvent e) -> AjoutJeton(finalX));
        }
        creerpartie.addActionListener((ActionEvent e) -> CreateGame());
        quitter.addActionListener((ActionEvent e)-> QuitGame());
        relancerButton.addActionListener((ActionEvent e)->LaunchNewGame());
    }

    private void LaunchNewGame(){
        cc.send(String.format("%s_%s",GAME_ACTION,"02"));
    }

    private ArrayList<JButton> getAllActionButtons() {
        ArrayList<JButton> liste = new ArrayList<>();
        liste.add(button0);
        liste.add(button1);
        liste.add(button2);
        liste.add(button3);
        liste.add(button4);
        liste.add(button5);
        liste.add(button6);
        return liste;
    }

    private void AjoutJeton(int x)
    {
        cc.send(String.format("%s_%s_%d",GAME_ACTION,"03",x));
    }

    private void QuitGame()
    {
        cc.send(CLOSE_GAME);
    }

    private void CreateGame() {
        if (cc != null) {
            cc.send(CODE_NEW_GAME);
        }
    }

    private void Switch(String message) {
        CardLayout cl = this.getCardLayout();
        switch (message.substring(0, 2)) {
            case "01"://newSer
                System.out.println(message);
                convertStringToServers(message);
                break;
            case "02"://Created Success
                cl.show(ContentPanel, "attente");
                gameIdWAIT.setText(String.format("Salle d'attente, id de la game : %s", message.substring(36)));
                break;
            case "03"://Déco forcée
                cl.show(ContentPanel, "lobby");
                break;
            case "04"://Partie lancée
                cl.show(ContentPanel, "gameframe");
                setDefaultGame();
                break;
            case "05"://Interrupt game
                System.out.println(message.substring(3));
                for (Component component1 : ServerList.getComponents()) {
                    JButton boutonTeste = (JButton) component1;
                    if (boutonTeste.getText().substring(0, (boutonTeste.getText()).indexOf("_")).equals(message.substring(3))) {
                        ServerList.remove(component1);
                        ServerList.revalidate();
                        createComponentMap();
                        ServerList.updateUI();
                        break;
                    }
                }
            case "06"://GAMEEVENT
                handleGameEvents(message);
                break;
            default:
                break;
        }
    }

    private void handleGameEvents(String message)
    {
        switch (message.substring(3, 5)){
            case "01":
                updateGameLabelTurn(message);
                break;
            case "02":
                resetGAME();
                break;
            case "03":
                addNewJeton(message);
                break;
            case "04":
                setTextGame(String.format("Le joueur %s a gagné", (Integer.parseInt(message.substring(6))%2 != 0) ? "rouge" : "jaune"));
                break;
            case "05":
                setTextGame("Égalité");
                break;
            default:
                break;
        }
    }

    private void setTextGame(String content)
    {
        labelGAMEFRAME.setText(content);
    }


    private void addNewJeton(String message)
    {
        System.out.println(message);
        this.x = Character.getNumericValue(message.charAt(6));
        tableau[Character.getNumericValue(message.charAt(8))][Character.getNumericValue(message.charAt(6))] = (Integer.parseInt(message.substring(10)) % 2 != 0);
        SecondaryPanel.repaint();
    }

    private void resetGAME(){
        this.setDefaultGame();
    }

    private void updateGameLabelTurn(String message)
    {
        int tourServer = Integer.parseInt(message.substring(6));
        labelGAMEFRAME.setText(String.format("Tour n°%d: c'est au joueur %s de placer son jeton", tourServer, (tourServer % 2 != 0) ? "rouge" : "jaune"));
    }


    private void convertStringToServers(String message) {
        System.out.println("Checking !");
        System.out.println(message.substring(39));
        Component button = getComponentByName(message.substring(39));
        if (button == null) {
            System.out.println("CreatingButton");
            System.out.println(message.substring(39));
            JButton bouton = new JButton(message.substring(39));
            bouton.setPreferredSize(new Dimension(200, 20));
            bouton.addActionListener((ActionEvent e) -> ConnectionThreadJeu(message.substring(39, message.indexOf("_", 39))));
            ServerList.add(bouton);
            createComponentMap();
            ServerList.updateUI();
        }
    }

    private void ConnectionThreadJeu(String idjoueur) {
        System.out.println(String.format("%s_%s", CODE_JOIN_GAME, idjoueur));
        cc.send(String.format("%s_%s", CODE_JOIN_GAME, idjoueur));
    }

    private void createComponentMap() {
        buttonsServerMap = new HashMap<String, Component>();
        Component[] components = ServerList.getComponents();
        for (int i = 0; i < components.length; i++) {
            JButton button = (JButton) components[i];
            buttonsServerMap.put(button.getText(), components[i]);
        }
        System.out.println(buttonsServerMap.size());
    }

    public Component getComponentByName(String name) {
        if (buttonsServerMap.containsKey(name)) {
            return (Component) buttonsServerMap.get(name);
        } else return null;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        main = new JPanel();
        main.setLayout(new BorderLayout(0, 0));
        main.setMaximumSize(new Dimension(800, 800));
        main.setMinimumSize(new Dimension(800, 800));
        main.setPreferredSize(new Dimension(800, 800));
        ContentPanel = new JPanel();
        ContentPanel.setLayout(new CardLayout(0, 0));
        main.add(ContentPanel, BorderLayout.CENTER);
        GameFrame = new JPanel();
        GameFrame.setLayout(new CardLayout(0, 0));
        GameFrame.setName("");
        ContentPanel.add(GameFrame, "gameframe");
        MainPanel = new JPanel();
        MainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        MainPanel.setMaximumSize(new Dimension(800, 800));
        MainPanel.setMinimumSize(new Dimension(800, 800));
        MainPanel.setOpaque(true);
        MainPanel.setPreferredSize(new Dimension(800, 800));
        GameFrame.add(MainPanel, "Card1");
        SecondaryPanel.setLayout(new GridBagLayout());
        MainPanel.add(SecondaryPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 800), new Dimension(-1, 800), new Dimension(-1, 800), 0, false));
        button0 = new JButton();
        button0.setActionCommand("");
        button0.setBackground(new Color(-1446160));
        button0.setBorderPainted(true);
        button0.setContentAreaFilled(false);
        button0.setFocusPainted(false);
        button0.setFocusable(false);
        button0.setForeground(new Color(-1315861));
        button0.setLabel("");
        button0.setText("");
        button0.putClientProperty("hideActionText", Boolean.TRUE);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        SecondaryPanel.add(button0, gbc);
        button2 = new JButton();
        button2.setBackground(new Color(-1446160));
        button2.setBorderPainted(true);
        button2.setContentAreaFilled(false);
        button2.setFocusPainted(false);
        button2.setFocusable(false);
        button2.setForeground(new Color(-1315861));
        button2.setLabel("");
        button2.setText("");
        button2.putClientProperty("hideActionText", Boolean.TRUE);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        SecondaryPanel.add(button2, gbc);
        button3 = new JButton();
        button3.setBackground(new Color(-1446160));
        button3.setBorderPainted(true);
        button3.setContentAreaFilled(false);
        button3.setFocusPainted(false);
        button3.setFocusable(false);
        button3.setForeground(new Color(-1315861));
        button3.setLabel("");
        button3.setText("");
        button3.putClientProperty("hideActionText", Boolean.TRUE);
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        SecondaryPanel.add(button3, gbc);
        button4 = new JButton();
        button4.setBackground(new Color(-1446160));
        button4.setBorderPainted(true);
        button4.setContentAreaFilled(false);
        button4.setFocusPainted(false);
        button4.setFocusable(false);
        button4.setForeground(new Color(-1315861));
        button4.setLabel("");
        button4.setText("");
        button4.putClientProperty("hideActionText", Boolean.TRUE);
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        SecondaryPanel.add(button4, gbc);
        button5 = new JButton();
        button5.setBackground(new Color(-1446160));
        button5.setBorderPainted(true);
        button5.setContentAreaFilled(false);
        button5.setFocusPainted(false);
        button5.setFocusable(false);
        button5.setForeground(new Color(-1315861));
        button5.setLabel("");
        button5.setText("");
        button5.putClientProperty("hideActionText", Boolean.TRUE);
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        SecondaryPanel.add(button5, gbc);
        button6 = new JButton();
        button6.setBackground(new Color(-1446160));
        button6.setBorderPainted(true);
        button6.setContentAreaFilled(false);
        button6.setFocusPainted(false);
        button6.setFocusable(false);
        button6.setForeground(new Color(-1315861));
        button6.setLabel("");
        button6.setText("");
        button6.putClientProperty("hideActionText", Boolean.TRUE);
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        SecondaryPanel.add(button6, gbc);
        button1 = new JButton();
        button1.setBackground(new Color(-1446160));
        button1.setBorderPainted(true);
        button1.setContentAreaFilled(false);
        button1.setFocusPainted(false);
        button1.setFocusable(false);
        button1.setForeground(new Color(-1315861));
        button1.setLabel("");
        button1.setText("");
        button1.putClientProperty("hideActionText", Boolean.TRUE);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        SecondaryPanel.add(button1, gbc);
        labelGAMEFRAME = new JLabel();
        labelGAMEFRAME.setText("labelpogg");
        MainPanel.add(labelGAMEFRAME, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        relancerButton = new JButton();
        relancerButton.setText("relancer");
        MainPanel.add(relancerButton, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        MainPanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        MainPanel.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        quitter = new JButton();
        quitter.setText("quitter");
        MainPanel.add(quitter, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        Lobby = new JPanel();
        Lobby.setLayout(new BorderLayout(0, 0));
        Lobby.setMinimumSize(new Dimension(800, 800));
        Lobby.setPreferredSize(new Dimension(800, 800));
        ContentPanel.add(Lobby, "lobby");
        connected = new JLabel();
        connected.setHorizontalAlignment(0);
        connected.setHorizontalTextPosition(0);
        connected.setText("Label");
        Lobby.add(connected, BorderLayout.NORTH);
        ServerList.setMaximumSize(new Dimension(600, 800));
        ServerList.setMinimumSize(new Dimension(600, 800));
        ServerList.setPreferredSize(new Dimension(600, 800));
        Lobby.add(ServerList, BorderLayout.WEST);
        Options = new JPanel();
        Options.setLayout(new BorderLayout(0, 0));
        Options.setMaximumSize(new Dimension(200, 800));
        Options.setMinimumSize(new Dimension(200, 800));
        Options.setPreferredSize(new Dimension(200, 800));
        Lobby.add(Options, BorderLayout.EAST);
        creerpartie = new JButton();
        creerpartie.setOpaque(true);
        creerpartie.setText("Créer Partie");
        Options.add(creerpartie, BorderLayout.NORTH);
        disconnected = new JPanel();
        disconnected.setLayout(new BorderLayout(0, 0));
        ContentPanel.add(disconnected, "disconnected");
        placeholder = new JLabel();
        placeholder.setHorizontalAlignment(0);
        placeholder.setHorizontalTextPosition(0);
        placeholder.setText("Tentative de connection");
        disconnected.add(placeholder, BorderLayout.CENTER);
        seReconnecterButton = new JButton();
        seReconnecterButton.setText("Se reconnecter");
        disconnected.add(seReconnecterButton, BorderLayout.NORTH);
        WaitingFrame = new JPanel();
        WaitingFrame.setLayout(new CardLayout(0, 0));
        ContentPanel.add(WaitingFrame, "attente");
        gameIdWAIT = new JLabel();
        gameIdWAIT.setHorizontalAlignment(0);
        gameIdWAIT.setText("Label");
        WaitingFrame.add(gameIdWAIT, "Card1");
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return main;
    }

    private void createUIComponents() {
        ServerList = new JPanel();
        SecondaryPanel = new JpanelConnect();
        ServerList.setLayout(new BoxLayout(ServerList, BoxLayout.Y_AXIS));
        ServerList.setMaximumSize(new Dimension(600, 800));
        ServerList.setMinimumSize(new Dimension(600, 800));
        ServerList.setPreferredSize(new Dimension(600, 800));
    }

    public Boolean[][] getTableau() {
        return this.tableau;
    }

    public int getChoosenX() {
        return this.x;
    }

    private void setDefaultGame()
    {
        this.tour = 1;
        this.x = -1;
        SecondaryPanel.repaint();
        for (int ligne = 0; ligne < 6; ligne++) {
            for (int colonne = 0; colonne < 7; colonne++) {
                this.tableau[ligne][colonne] = null;
            }
        }
    }

}
