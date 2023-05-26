import javax.imageio.ImageIO;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

    private DecodificadorVideo decodificadorVideo;

    public HashMap<Integer, Image> imagenesFiltradas = new HashMap<Integer, Image>();


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
                System.out.println("Entra en decode");
            }
            else if (args[i].equals("--binarization")) {
                filter = "binarization";
                filterValue = Integer.parseInt(args[i+1]);
            }
            else if (args[i].equals("--negative")) {
                filter = "negative";
            }
            else if (args[i].equals("--averagin")) {
                filter = "averagin";
                filterValue = Integer.parseInt(args[i+1]);

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
            // Código para la codificación
            System.out.println("Empieza la codificación");
            System.out.println(filter);
            lector = new LectorImagenes(rutaArchivoEntrada, rutaArchivoSalida,  filter, filterValue, imagenesFiltradas);
            reproductor = new ReproductorImagenes(rutaArchivoSalida, fps);
            codificadorVideo = new CodificadorVideo(imagenesFiltradas, GOP, nTiles, seekRange, quality, rutaArchivoSalida);

            lector.lectorImagenes();
            reproductor.reproducir();
            codificadorVideo.GOPSeparation();
            codificadorVideo.recorrerGOP();


            System.out.println("Ha finalizado la compresión");
        } else if (decode) {
            // Código para la decodificación
            System.out.println("Empieza la decodificación");
            lector = new LectorImagenes(rutaArchivoEntrada, rutaArchivoSalida, filter, filterValue, imagenesFiltradas);
            decodificadorVideo = new DecodificadorVideo(fps, GOP, nTiles, rutaArchivoSalida, lector);
            decodificadorVideo.decode();

            System.out.println("Ha finalizado la descompresión");
            reproductor = new ReproductorImagenes("output/decompressed.zip", fps);
            reproductor.reproducir();
        }

        else{
            // Código para la codificación
            System.out.println("Sin decodificación");
            lector = new LectorImagenes(rutaArchivoEntrada, rutaArchivoSalida,  filter, filterValue, imagenesFiltradas);
            reproductor = new ReproductorImagenes(rutaArchivoSalida, fps);


            lector.lectorImagenes();

            reproductor.reproducir();

        }
    }

}
