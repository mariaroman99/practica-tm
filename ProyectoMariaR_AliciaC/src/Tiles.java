import java.awt.image.BufferedImage;


public class Tiles {

    private int x,y, id, xDest, yDest;
    private BufferedImage tiles;

    public Tiles(BufferedImage tiles, int id) {
        this.tiles = tiles; this.id = id;
    }
    public BufferedImage getTiles() {
        return tiles;
    }
    public void setTiles(BufferedImage tiles) {
        this.tiles = tiles;
    }
    public void setX(int x) {this.x = x;}
    public void setY(int y) {this.y = y;}
    public int getX() {return x;}

    public int getId(){return id;}
    public void setId(int id) {
        this.id = id;
    }
    public int getY() {return y;}

    public int getXDest() {
        return xDest;
    }
    public void setXDest(int xDest) {
        this.xDest = xDest;
    }

    public int getYDest() {
        return yDest;
    }

    public void setYDest(int yDest) {
        this.yDest = yDest;
    }

}