
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.Deflater;
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
    public ArrayList<BufferedImage> imagenes;
    public int GOP;
    public int tileWidth;
    public int tileHeight;
    public int nTiles;
    public int fps;
    String output;
    public boolean batch = false;

    ArrayList<Tiles> lista_tiles = new ArrayList<>();

    public LectorImagenes lectorImagenes;
    /**
     * Clase decodificador, que a partir de una archivo comprimido codificado y
     * un documento con los parametros de codificacion, descomprime el archivo y
     * recompone (decodifica) las imagenes.
    
     */
    public DecodificadorVideo(int fps ,int GOP, int nTiles, String output, LectorImagenes lectorImagenes) {
        this.output = output;
        this.fps = fps;
        this.GOP = GOP;
        this.nTiles = nTiles;
        this.ids = new ArrayList<>();
        this.xCoords = new ArrayList<>();
        this.yCoords = new ArrayList<>();
        this.imagenes = new ArrayList<>();
        this.lectorImagenes = lectorImagenes;

    }

    /**
     * Constructor de la clase decode, que inicializa los metodos internos, y
     * selecciona el fichero de descompresion.
     */
    public ArrayList<BufferedImage> decode() throws IOException {
        this.readZip();
        this.iterateImages();
        new File("output/ImagenesDescomprimidas/").mkdirs();
        int totalImages = this.imagenes.size();
        int completedImages = 0;
        int comptador = 0;
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream("output/ImagenesDescomprimidass/"));
        for (BufferedImage i : this.imagenes) {
            comprimirJPEG(i, "output/ImagenesDescomprimidas/", String.valueOf(comptador) + ".jpeg");
            comptador++;
            completedImages++;
            double progressPercentage = (double) completedImages / totalImages;
            ProgressDemo.updateProgress(progressPercentage);

        }
        convertToZip("output/ImagenesDescomprimidas");
        zipOut.close();
        return this.imagenes;
    }

    public static void convertToZip(String path) throws IOException {
        Path sourcePath = Paths.get(path);

        // Create a ZIP file
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream("output/ImagenesDescomprimidas.zip"));

        Files.walk(sourcePath)
                .filter(filePath -> !Files.isDirectory(filePath))
                .forEach(filePath -> {
                    try {
                        // Create a new ZIP entry
                        String relativePath = sourcePath.relativize(filePath).toString();
                        ZipEntry zipEntry = new ZipEntry(relativePath);
                        zipOutputStream.putNextEntry(zipEntry);

                        // Read the file and write its contents to the ZIP output stream
                        byte[] fileBytes = Files.readAllBytes(filePath);
                        zipOutputStream.write(fileBytes);

                        zipOutputStream.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        zipOutputStream.close();
    }



    public static void comprimirJPEG(BufferedImage image, String name, String outputIName) throws FileNotFoundException, IOException {
         //File imageFile = new File("Desert.jpg");
        File compressedImageFile = new File(name + "/" + outputIName);
        OutputStream outputStream = new FileOutputStream(compressedImageFile);
        float imageQuality = 1.0f;
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

        imageWriter.write(null, new IIOImage(image, null, null), imageWriteParam);
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
            File zipFile = new File("output/" + output);
            ZipFile zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.equalsIgnoreCase("ImagenesComprimidas/coords.txt")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                    String line;

                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(" ");
                        ids.add(Integer.parseInt(parts[0]));
                        xCoords.add(Integer.parseInt(parts[1]));
                        yCoords.add(Integer.parseInt(parts[2]));


                    }
                    reader.close();
                } else {
                    BufferedImage image = ImageIO.read(zip.getInputStream(entry));
                    imagenes.add(image);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    /**
     * Metodo que reconstruye las imagenes.
     */
    private void iterateImages() {
        this.tileWidth = this.imagenes.get(0).getWidth() / nTiles;
        this.tileHeight = this.imagenes.get(0).getHeight() / nTiles;
        BufferedImage iframe = null;
        int posInicio = 0;
        int posFinal = (nTiles * nTiles) -1;
        for (int x = 0; x < this.imagenes.size() - 1; x++) {
            BufferedImage frame = this.imagenes.get(x);
            // Para no recorrer toda la lista de ids para cada imagen, miramos las posiciones donde se
            // encuetran las coordenadas en el fichero texto
            if (x != 0 && x!=1 && x % 10 != 0){
                posInicio = posInicio + (nTiles * nTiles);
                posFinal = posFinal + (nTiles * nTiles);
            }
            if (x % this.GOP == 0) {
                iframe = frame;
            } else if (x == this.imagenes.size() - 2) {
                this.reconstruirImagen(frame, this.imagenes.get(x + 1), posInicio, posFinal);
            } else {
                this.reconstruirImagen(iframe, frame, posInicio, posFinal);
            }

        }
    }

    /**
     * Metodo auxiliar del metodo iterateImages, que a partir de un frame I y un 
     * frame P utilizando las teselas generadas
     * a partir del frame I, aplica la media de color al frame P.
   
     */
    private void reconstruirImagen(BufferedImage base, BufferedImage destino, int _inicio, int _final) {
        ArrayList<Tiles> tiles = new ArrayList<>();
        Tiles tile;
        int count = 0;
        for (int y = 0; y < base.getHeight(); y += this.tileHeight) {
            for (int x = 0; x < base.getWidth(); x += this.tileWidth) {
                tile = new Tiles(base.getSubimage(x, y, this.tileWidth, this.tileHeight), count);
                tiles.add(tile);
                count++;
            }
        }
        for (int k = _final - nTiles; k >= _inicio; --k) {
            Tiles t = tiles.get(ids.get(k + nTiles));
            Integer x = xCoords.get(k);
            Integer y = yCoords.get(k);
            //System.out.println(ids.get(k) + "," + x + "," + y);
            BufferedImage tes = t.getTiles();
            if (x != -1 && y != -1) {
                for (int i = 0; i < this.tileHeight; i++) {
                    for (int j = 0; j < this.tileWidth; j++) {
                        int RGB = tes.getRGB(j , i);
                        destino.setRGB(j + y  , i + x, RGB);
                    }
                }
            }
        }
    }

    /**
     * Metodo auxiliar del metodo reconstruirImagen, que a partir de una imagen pasada por parametro
     * permite subdividir dicha imagen en una array de teselas, que devuelve como retorno.

     */



}