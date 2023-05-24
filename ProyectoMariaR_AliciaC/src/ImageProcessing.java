

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessing {

    public static ArrayList<Tiles> allTiles = new ArrayList<>();

    public ImageProcessing(){}
    public  ArrayList<Tiles> divideImageIntoTiles(BufferedImage img, int nTiles) {
        allTiles = new ArrayList();

        int propX = img.getWidth() / nTiles;
        int propY = img.getHeight() / nTiles;
        int count = 1;

        WritableRaster wr = (WritableRaster) img.getData();


        for (int i = 0; i < nTiles; i++) {
            for (int j = 0; j < nTiles; j++) {
                if (i * propX + propX < img.getWidth()) {
                    if (j * propY + propY < img.getHeight()) {
                        WritableRaster tile = (WritableRaster)wr.createChild((i * propX), (j * propY), propX, propY, 0, 0, null);
                        BufferedImage buffTile = new BufferedImage(img.getColorModel(), tile,
                                img.getColorModel().isAlphaPremultiplied(), null);
                        Tiles t = new Tiles(buffTile, i*propX, j*propY, count);
                        count++;
                        allTiles.add(t);
                    }
                }
            }
        }

        return allTiles;
    }

    public void compareAndSaveTileCoordinates(ArrayList<Tiles> tiles, BufferedImage compareImage, BufferedImage originalImage, String outputFileName) {
        int tileWidth = tiles.get(0).getImage().getWidth();
        int tileHeight = tiles.get(0).getImage().getHeight();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFileName))) {
            for (Tiles tile : tiles) {
                int compareX = tile.getX();
                int compareY = tile.getY();

                boolean isTileMatched = false;

                for (int y = 0; y < tileHeight; y++) {
                    for (int x = 0; x < tileWidth; x++) {
                        int pixelX = compareX + x;
                        int pixelY = compareY + y;

                        int tileRGB = tile.getImage().getRGB(x, y);
                        int compareRGB = compareImage.getRGB(pixelX, pixelY);

                        if (tileRGB == compareRGB) {
                            isTileMatched = true;
                            break;
                        }
                    }

                    if (isTileMatched) {
                        break;
                    }
                }

                if (isTileMatched) {
                    // Save tile information and compared image coordinates in the document
                    writer.print(tile.getId());
                    writer.println("  (" + compareX + ", " + compareY + ")");
                    writer.println();
                    // Replace the matched tile with a blank space in the original image
                    Graphics2D g2d = (Graphics2D) originalImage.getGraphics();
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(compareX, compareY, tileWidth, tileHeight);
                    g2d.dispose();
                }
            }
            // Save the modified original image
            File outputImageFile = new File("modified_original_image.png");
            ImageIO.write(originalImage, "png", outputImageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }









}

