import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class Procesador {

    private boolean encode = false;

    private boolean decode = false;
    private String filter = "";

    private int filterValue = 0;
    private int nTiles = 0;
    private int seekRange = 0;
    private int GOP = 10;
    private int quality = 0;
    private boolean batch = false;
    private String rutaArchivoEntrada = null;
    private String rutaArchivoSalida = null;
    //Velocidad estandar
    private int fps = 24;
    private LectorImagenes lector;
    private ReproductorImagenes reproductor;

    private CodificadorVideo codificadorVideo;

    public static ArrayList<Tiles> allTiles;

    public Procesador(String[] args) throws IOException, InterruptedException {
        procesarArgumentos(args);
    };

    private void procesarArgumentos(String[] args) throws IOException, InterruptedException  {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-i") || args[i].equals("--input")) {
                rutaArchivoEntrada = args[i+1];
                i++;
            } else if (args[i].equals("-o") || args[i].equals("--output")) {
                rutaArchivoSalida = args[i+1];
                i++;
            } else if (args[i].equals("-e") || args[i].equals("--encode")) {
                encode = true;
            }
            else if (args[i].equals("-d") || args[i].equals("--decode")) {
                decode = true;
            }
            else if (args[i].equals("--binarization")) {
                if (args[i + 1].equals(null)) {
                    System.out.println("Es necesario añadir un valor");
                }else{
                    filter = "binarization";
                    filterValue = Integer.parseInt(args[i+1]);
                }
            }
            else if (args[i].equals("--negative")) {
                filter = "negative";
            }
            else if (args[i].equals("--averagin")) {
                if (args[i + 1].equals(null)) {
                    System.out.println("Es necesario añadir un valor");
                }else{
                    filter = "averagin";
                    filterValue = Integer.parseInt(args[i+1]);
                }

            }
            else if (args[i].equals("--nTiles")) {
                nTiles = Integer.parseInt(args[i+1]);
            }
            else if (args[i].equals("--seekRange")) {
                seekRange = Integer.parseInt(args[i+1]);
            }
            else if (args[i].equals("--GOP")) {
                GOP = Integer.parseInt(args[i+1]);
            }
            else if (args[i].equals("--quality")) {
                quality = Integer.parseInt(args[i+1]);
            }
            else if (args[i].equals("-b") || args[i].equals("--batch")) {
                batch = true;
            }
            else if (args[i].equals("-fps")) {
                fps = Integer.parseInt(args[i+1]);
            }

        }

        if (rutaArchivoEntrada == null || rutaArchivoSalida == null) {
            System.out.println("Uso: java LectorImagenes -i/--input <rutaArchivoEntrada> -o/--output <rutaArchivoSalida> [-e/--encode]");
            System.exit(1);
        }

        if (encode) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            lector = new LectorImagenes(rutaArchivoEntrada, rutaArchivoSalida, countDownLatch, filter, filterValue);
            reproductor = new ReproductorImagenes(rutaArchivoSalida, fps, countDownLatch);
            codificadorVideo = new CodificadorVideo(rutaArchivoEntrada, rutaArchivoSalida, GOP);

            lector.lectorImagenes();
            reproductor.reproducir();
            codificadorVideo.codificador();

            ImageProcessing imageProcessing = new ImageProcessing();
            File imageFile = new File("004.jpg");
            BufferedImage image = ImageIO.read(imageFile);
            File imageFile2 = new File("003.jpg");
            BufferedImage image2 = ImageIO.read(imageFile2);
            allTiles = imageProcessing.divideImageIntoTiles(image, 5);
            System.out.println(allTiles.get(2).getId());
            imageProcessing.compareAndSaveTileCoordinates(allTiles, image2, image, "documento.txt");
        }else{
            CountDownLatch countDownLatch = new CountDownLatch(0);
            reproductor = new ReproductorImagenes(rutaArchivoEntrada, fps, countDownLatch);
            reproductor.reproducir();
            codificadorVideo = new CodificadorVideo(rutaArchivoEntrada, rutaArchivoSalida, GOP);
            codificadorVideo.codificador();
        }
    }

}
