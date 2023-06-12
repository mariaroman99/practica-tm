import javax.imageio.ImageIO;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Procesador {

    private boolean encode = false;

    private boolean decode = false;
    private String filter = "";

    private int filterValue = 0;
    private int nTiles = 5;
    private int seekRange = 5;
    private int GOP = 10;
    private int quality = 10;
    private boolean batch = false;
    private String rutaArchivoEntrada = null;
    private String rutaArchivoSalida = null;
    //Velocidad estandar
    private int fps = 24;
    private LectorImagenes lector;

    private boolean help = false;

    private ReproductorImagenes reproductor;

    private CodificadorVideo codificadorVideo;

    private DecodificadorVideo decodificadorVideo;

    public HashMap<Integer, Image> imagenesFiltradas = new HashMap<Integer, Image>();

    public  long calcularTamañoCarpeta(String ruta){
        File zipFile = new File(ruta);
        long totalSize = 0;
        try (ZipFile zip = new ZipFile(zipFile)) {


            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    totalSize += entry.getSize();
                }
            }

            return totalSize;
        } catch (IOException e) {
            e.printStackTrace();
        }return totalSize;
    }
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
            else if (args[i].equals("-help") || args[i].equals("--help") ) {
                help = true;
            }

        }
        if (help){
            System.out.println("Aquí estan les possibles comandes:");
            System.out.println("-i, --input  <path to file.zip>");
            System.out.println("-o, --output <path to file>");
            System.out.println("-e, --encode, -d, --decode");
            System.out.println("--fps <value> , --binarization <value>, --negative, --averaging <value>");
            System.out.println("--nTiles <value,...> ,--seekRange <value>, --GOP <value>");
            System.out.println("--quality <value>, -b, --batch");
        }

        if (rutaArchivoEntrada == null || rutaArchivoSalida == null) {
            System.out.println("Uso: java LectorImagenes -i/--input <rutaArchivoEntrada> -o/--output <rutaArchivoSalida> [-e/--encode]");
            System.exit(1);
        }

        if (encode) {
            // Código para la codificación
            long startTime = System.currentTimeMillis();
            System.out.println("Empieza la codificación");
            System.out.println(filter);
            lector = new LectorImagenes(rutaArchivoEntrada, rutaArchivoSalida,  filter, filterValue, imagenesFiltradas);
            reproductor = new ReproductorImagenes(rutaArchivoSalida, fps);
            codificadorVideo = new CodificadorVideo(imagenesFiltradas, GOP, nTiles, seekRange, quality, rutaArchivoSalida);

            lector.lectorImagenes();
            reproductor.reproducir();
            codificadorVideo.code();


            System.out.println("\nHa finalizado la compresión");
            // Obtener el tiempo actual después de ejecutar el código
            long endTime = System.currentTimeMillis();

            // Calcular la diferencia para obtener el tiempo de ejecución en milisegundos
            long executionTime = endTime - startTime;

            long tamañoComprimido = calcularTamañoCarpeta("ImagenesComprimidas.zip");
            long tamañoSinComprimido = calcularTamañoCarpeta(rutaArchivoSalida);

            // Calcular el porcentaje de factor de compresión
            double porcentajeFactorCompresion = ((tamañoSinComprimido - tamañoComprimido) / (double) tamañoSinComprimido) * 100;


            // Imprimir el tiempo de ejecución
            System.out.println("Tiempo de ejecución: " + executionTime + " milisegundos");
            // Imprimir el porcentaje de factor de compresión
            System.out.println("Porcentaje de factor de compresión: " + porcentajeFactorCompresion + "%");

        } else if (decode) {
            // Código para la decodificación
            System.out.println("Empieza la decodificación");
            lector = new LectorImagenes(rutaArchivoEntrada, rutaArchivoSalida, filter, filterValue, imagenesFiltradas);
            decodificadorVideo = new DecodificadorVideo(fps, GOP, nTiles, rutaArchivoSalida, lector);
            decodificadorVideo.decode();

            System.out.println("\nHa finalizado la descompresión");
            reproductor = new ReproductorImagenes("ImagenesDescomprimidas.zip", fps);
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
