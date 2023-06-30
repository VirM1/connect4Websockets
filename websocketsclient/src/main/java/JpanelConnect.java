import javax.swing.*;
import java.awt.*;

public class JpanelConnect extends JPanel {

    @Override
    protected void paintComponent(Graphics g) {
        Client frame = (Client) SwingUtilities.getWindowAncestor(this);
        super.paintComponent(g);
        Boolean[][] tableau = frame.getTableau();
        if (frame.getChoosenX() != -1) {//Java.awt a forcé ma main c'est la dernière fois que j'écris du code lié aux interfaces kek
            //C'est une atrocité, pire que les spam des elseif
            for (int ligne = 0; ligne < tableau.length; ligne++){
                for (int colonne = 0; colonne < tableau[0].length; colonne++){
                    if(tableau[ligne][colonne] != null) {
                        if (!tableau[ligne][colonne]) {
                            g.setColor(Color.yellow);
                        } else {
                            g.setColor(Color.red);
                        }
                        g.fillOval(25 + (colonne * 112), 600 - (ligne * 112), 60, 60);
                    }
                }
            }
        } else {
            repaint();
        }
    }

}
