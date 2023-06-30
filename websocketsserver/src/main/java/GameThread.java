import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.Objects;

public class GameThread extends Thread{

    //Launched
    public static final String UPDATE_LABEL = "01";

    public static final String RELOAD_GAME = "02";

    public static final String CONFIRMED_JETON = "03";

    public static final String WIN_CONFIRMED = "04";

    public static final String GAME_DRAW = "05";


    //Recieved
    public static final String RELANCER = "02";

    public static final String NEW_JETON = "03";



    private final String identifiant;

    private final WebSocket gameHost; //pair rouge

    private WebSocket OtherPlayer; //impair jaune

    private final Server server;

    private final Boolean[][] tableau = new Boolean[6][7];

    private int tour;

    private boolean victoire;

    public GameThread(WebSocket GameHost, Server server){
        this.gameHost = GameHost;
        this.identifiant = GameHost.toString();
        this.server = server;
        this.OtherPlayer = null;
    }


    public String getIdentifiant() {
        return identifiant;
    }

    public int getNumPlayers()
    {
        if(OtherPlayer == null){
            return 1;
        }else{
            return 2;
        }
    }

    @Override
    public void run()
    {
        this.setDefaultGame();
        System.out.println(gameHost);
        System.out.println("launched pog");
    }


    @Override
    public void interrupt()
    {
        super.interrupt();
        ArrayList<WebSocket> connections = new ArrayList<WebSocket>();
        connections.add(gameHost);
        if(OtherPlayer != null){
            connections.add(OtherPlayer);
        }
        server.broadcast(Server.FORCE_CLOSE,connections);
        System.out.println(Integer.toHexString(gameHost.hashCode()));
        server.broadcast(String.format("%s_%s", Server.BUTTON_REMOVE,Integer.toHexString(gameHost.hashCode())));
    }

    public WebSocket getGameHost() {
        return gameHost;
    }

    public WebSocket getOtherPlayer() {
        return OtherPlayer;
    }

    public Server getServer() {
        return server;
    }

    public void setOtherPlayer(WebSocket otherPlayer) {
        OtherPlayer = otherPlayer;
    }

    public Boolean[][] getTableau() {
        return this.tableau;
    }

    public void addJETONToThread(String message, WebSocket conn)
    {
        if(checkJETON(conn)){
            int colonne = Integer.parseInt(message.substring(6));
            if(tableau[5][colonne] == null) {
                addNewEmplacement(colonne);
                this.checkIfWinning((this.tour%2 != 0));
            }
        }
    }

    private void addNewEmplacement(int numCol)
    {
        int index = 0;
        for(Boolean[] ligne : this.tableau){
            if(ligne[numCol] == null)
            {
                ligne[numCol] = (this.tour%2 != 0);
                server.broadcast(String.format("%s_%s_%d_%d_%d", Server.GAME_EVENT,CONFIRMED_JETON,numCol,index,this.tour),getBothPlayer());
                break;
            }
            index++;
        }
    }

    private void checkIfWinning(Boolean joueur)
    {
        this.victoire = win(joueur);
        if(!victoire) {
            if(this.tour < 42){
                this.tour++;
                sendBroadcast(UPDATE_LABEL);
            }else{
                sendBroadcast(GAME_DRAW);
            }
        }else{
            sendBroadcast(WIN_CONFIRMED);
        }
    }

    private boolean win(Boolean joueur){
        for(int ligne = 0; ligne<6; ligne++){
            for (int colonne = 0;colonne < 4 ;colonne++){
                if(tableau[ligne][colonne] == joueur && tableau[ligne][colonne+1] == joueur && tableau[ligne][colonne+2] == joueur && tableau[ligne][colonne+3] == joueur){
                    return true;
                }
            }
        }
        for(int ligne = 3; ligne < 6; ligne++){
            for(int colonne = 0; colonne < 4; colonne++){
                if(tableau[ligne][colonne] == joueur && tableau[ligne-1][colonne+1] == joueur && tableau[ligne-2][colonne+2] == joueur && tableau[ligne-3][colonne+3] == joueur){
                    return true;
                }
            }
        }
        for(int ligne = 0; ligne < 3; ligne++){
            for(int colonne = 0; colonne < 4; colonne++){
                if(tableau[ligne][colonne] == joueur && tableau[ligne+1][colonne+1] == joueur && tableau[ligne+2][colonne+2] == joueur && tableau[ligne+3][colonne+3] == joueur){
                    return true;
                }
            }
            for(int colonne = 0; colonne < 7; colonne++){
                if (tableau[ligne][colonne] == joueur && tableau[ligne+1][colonne] == joueur && tableau[ligne+2][colonne] == joueur && tableau[ligne+3][colonne] == joueur){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkJETON(WebSocket conn)
    {
        boolean bol = false;
        System.out.println(conn.toString());
        System.out.println(gameHost.toString());
        System.out.println(OtherPlayer.toString());
        System.out.println(tour);
        if(!victoire && this.tour <= 42){
            if ((Objects.equals(conn.toString(), gameHost.toString())) && tour % 2 == 0) {
                bol = true;
            } else if ((Objects.equals(conn.toString(), OtherPlayer.toString())) && tour % 2 != 0) {
                bol = true;
            }
        }
        return bol;
    }

    private void setDefaultGame() {
        this.tour = 1;
        for (int ligne = 0; ligne < 6; ligne++) {
            for (int colonne = 0; colonne < 7; colonne++) {
                this.tableau[ligne][colonne] = null;
            }
        }
        this.victoire = false;
        sendBroadcast(UPDATE_LABEL);
    }

    public void relancerPartie()
    {
        this.setDefaultGame();
        sendBroadcast(RELOAD_GAME);
    }

    private void sendBroadcast(String code)
    {
        server.broadcast(String.format("%s_%s_%s", Server.GAME_EVENT,code,this.tour),getBothPlayer());
    }

    private ArrayList<WebSocket> getBothPlayer()
    {
        ArrayList<WebSocket> conns = new ArrayList<WebSocket>();
        conns.add(gameHost);
        conns.add(OtherPlayer);
        return conns;
    }
}
