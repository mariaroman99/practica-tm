
import java.awt.image.BufferedImage;
import java.util.ArrayList;


public class Imagen {
    private BufferedImage image;
    private int id;
    private ArrayList<Tiles> tiles;
    private ArrayList<BufferedImage> frames;

    public Imagen(BufferedImage image, int id) {
        this.image = image;
        this.id = id;
        this.tiles = new ArrayList<>();
        this.frames = new ArrayList<>();
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ArrayList<Tiles> getTiles() {
        return tiles;
    }

    public void setTiles(ArrayList<Tiles> tiles) {
        this.tiles = tiles;
    }

    public ArrayList<BufferedImage> getpFrames() {
        return frames;
    }

    public void setpFrames(ArrayList<BufferedImage> pFrames) {
        this.frames = pFrames;
    }

    public void addpFrame(BufferedImage image){
        this.frames.add(image);
    }
}