
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;



public class DecodificadorVideo {
    public ArrayList<Integer> ids;
    public ArrayList<Integer> xCoords;
    public ArrayList<Integer> yCoords;
    public ArrayList<BufferedImage> imatges;
    public int gop;
    public int tileWidth;
    public int tileHeight;
    public int nTiles;
    public int fps;
    String output;
    public boolean batch = false;

    public LectorImagenes lectorImagenes;
    /**
     * Clase decodificador, que a partir de una archivo comprimido codificado y
     * un documento con los parametros de codificacion, descomprime el archivo y
     * recompone (decodifica) las imagenes.
     *
     * @param gop
     * @param nTiles
     */
    public DecodificadorVideo(int fps ,int gop, int nTiles, String output, LectorImagenes lectorImagenes) {
        this.batch = batch;
        this.output = output;
        this.fps = fps;
        this.gop = gop;
        this.nTiles = nTiles;
        this.ids = new ArrayList<>();
        this.xCoords = new ArrayList<>();
        this.yCoords = new ArrayList<>();
        this.imatges = new ArrayList<>();
        this.lectorImagenes = lectorImagenes;

    }

    /**
     * Constructor de la clase decode, que inicializa los metodos internos, y
     * selecciona el fichero de descompresion.
     */
    public ArrayList<BufferedImage> decode() throws IOException {
        this.readZip();
        this.iterateImages();
        new File("output/decompressed/").mkdirs();
        int comptador = 0;
        for (BufferedImage i : this.imatges) {
            try {
                compressInJPEG(lectorImagenes.average(i, 3), "output/decompressed/", String.valueOf(comptador) + ".jpeg");
            } catch (IOException ex) {
              
            }
            comptador++;
        }
        convertToZip("output/decompressed");
        return this.imatges;
    }

    public static void convertToZip(String path) throws IOException {
        Path sourcePath = Paths.get(path);

        // Create a ZIP file
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream("output/decompressed.zip"));

        Files.walk(sourcePath)
                .filter(filePath -> !Files.isDirectory(filePath))
                .forEach(filePath -> {
                    try {
                        // Create a new ZIP entry
                        String relativePath = sourcePath.relativize(filePath).toString();
                        zipOutputStream.putNextEntry(new ZipEntry(relativePath));

                        // Read the file and write its contents to the ZIP output stream
                        byte[] fileBytes = Files.readAllBytes(filePath);
                        zipOutputStream.write(fileBytes, 0, fileBytes.length);
                        zipOutputStream.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        zipOutputStream.close();
    }

    public static void compressInJPEG(BufferedImage image, String name, String outputIName) throws FileNotFoundException, IOException {

        //File imageFile = new File("Desert.jpg");
        File compressedImageFile = new File(name + "/" + outputIName);
        //InputStream inputStream = new FileInputStream(imageFile);
        OutputStream outputStream = new FileOutputStream(compressedImageFile);
        float imageQuality = 1.0f;
        BufferedImage bufferedImage = image;
        //Get image writers
        Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName("jpg");
        if (!imageWriters.hasNext()) {
            throw new IllegalStateException("Writers Not Found!!");
        }
        ImageWriter imageWriter = (ImageWriter) imageWriters.next();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
        imageWriter.setOutput(imageOutputStream);
        ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();
        //Set the compress quality metrics
        imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        imageWriteParam.setCompressionQuality(imageQuality);

        imageWriter.write(null, new IIOImage(bufferedImage, null, null), imageWriteParam);
        //inputStream.close();
        outputStream.close();
        imageOutputStream.close();
        imageWriter.dispose();
    }

    /**
     * El metodo readZip permite leer un archivo zip codificado y descodificarlo
     * a la vez que descomprime el zip. Cuando se descomprime uno de los
     * elementos del zip, se busca en la lista de parametros las coordenadas y
     * se decodifica.
     */
    public void readZip() {
        try {
            File f = new File("output/"+ output);
            ZipFile z = new ZipFile(f);
            Enumeration<? extends ZipEntry> entries = z.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.equalsIgnoreCase("Compressed/coords.txt")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(z.getInputStream(entry)));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(" ");
                        ids.add(Integer.parseInt(parts[0]));
                        xCoords.add(Integer.parseInt(parts[1]));
                        yCoords.add(Integer.parseInt(parts[2]));
                    }
                    reader.close();
                } else {
                    BufferedImage im = ImageIO.read(z.getInputStream(entry));
                    this.imatges.add(im);
                }
            }
        } catch (IOException ex) {
            
        }
    }

    /**
     * Metodo que reconstruye las imagenes.
     */
    private void iterateImages() {
        this.tileWidth = this.imatges.get(0).getWidth() / nTiles;
        this.tileHeight = this.imatges.get(0).getHeight() / nTiles;
        BufferedImage iframe = null;
        for (int x = 0; x < this.imatges.size() - 1; x++) {
            BufferedImage frame = this.imatges.get(x);
            if (x % this.gop == 0) {
                iframe = frame;
            } else if (x == this.imatges.size() - 2) {
                this.buildPFrames(iframe, frame);
                this.buildPFrames(frame, this.imatges.get(x + 1));
            } else {
                this.buildPFrames(iframe, frame);
            }

        }
    }

    /**
     * Metodo auxiliar del metodo iterateImages, que a partir de un frame I y un 
     * frame P utilizando las teselas generadas por el metodo generateMacroblocks
     * a partir del frame I, aplica la media de color al frame P.
     * @param iframe
     * @param pframe
     */
    private void buildPFrames(BufferedImage iframe, BufferedImage pframe) {
        ArrayList<Tiles> tiles = generateMacroblocks(iframe);
        for (int i = 0; i < ids.size(); i++) {
            Tiles t = tiles.get(ids.get(i));
            Integer x = xCoords.get(i);
            Integer y = yCoords.get(i);
            if (x != -1 && y != -1) {
                for (int xCoord = 0; xCoord < (this.tileHeight); xCoord++) {
                    for (int yCoord = 0; yCoord < (this.tileWidth); yCoord++) {
                        int rgb = t.getTiles().getRGB(yCoord, xCoord);
                        pframe.setRGB(yCoord + y, xCoord + x, rgb);
                    }
                }
            }
        }
    }

    /**
     * Metodo auxiliar del metodo buildPFrames, que a partir de una imagen pasada por parametro
     * permite subdividir dicha imagen en una array de teselas, que devuelve como retorno.
     * @param image
     * @return array de teselas de la imagen.
     */
    public ArrayList<Tiles> generateMacroblocks(BufferedImage image) {
        ArrayList<Tiles> tiles = new ArrayList<>();
        Tiles t;
        int count = 0;
        for (int y = 0; y < image.getHeight(); y += this.tileHeight) {
            for (int x = 0; x < image.getWidth(); x += this.tileWidth) {
                t = new Tiles(image.getSubimage(x, y, this.tileWidth, this.tileHeight), count);
                tiles.add(t);
                count++;
            }
        }
        return tiles;
    }


}