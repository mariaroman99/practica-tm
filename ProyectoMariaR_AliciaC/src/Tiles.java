import java.awt.image.BufferedImage;

import java.awt.image.BufferedImage;

/**
 *
 * @author BlaiSB
 */
public class Tiles {

    private int x,y, id;
    private BufferedImage img;

    public Tiles(BufferedImage img, int x, int y, int id) {
        this.x = x; this.y = y; this.img = img; this.id = id;
    }

    public void setImage(BufferedImage i) { this.img = i;}
    public void setX(int x) {this.x = x;}
    public void setY(int y) {this.y = y;}
    public int getX() {return x;}

    public int getId(){return id;}
    public int getY() {return y;}
    public BufferedImage getImage() {return img;}
}