
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *  Classe Marc que implementara certs atributs per a la gestió de frames.
 *  @Author Victor i Alvaro
 */
public class Marco{
    private BufferedImage image;
    private int id;
    private ArrayList<Tiles> tiles;
    private ArrayList<BufferedImage> pFrames;

    public Marco(BufferedImage image, int id) {
        this.image = image;
        this.id = id;
        this.tiles = new ArrayList<>();
        this.pFrames = new ArrayList<>();
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

    public void setTiles(ArrayList<Tiles> tesseles) {
        this.tiles = tesseles;
    }

    public ArrayList<BufferedImage> getpFrames() {
        return pFrames;
    }

    public void setpFrames(ArrayList<BufferedImage> pFrames) {
        this.pFrames = pFrames;
    }

    public void addpFrame(BufferedImage image){
        this.pFrames.add(image);
    }
}