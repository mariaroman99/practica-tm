import javax.imageio.ImageIO;
import java.util.*;
import java.util.zip.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;


public class LectorImagenes {

    private final String rutaArchivoEntrada;
    private final String rutaArchivoSalida;
    private CountDownLatch countDownLatch;

    public LectorImagenes(String rutaArchivoEntrada, String rutaArchivoSalida, CountDownLatch countDownLatch) {
        this.rutaArchivoEntrada = rutaArchivoEntrada;
        this.rutaArchivoSalida = rutaArchivoSalida;
        this.countDownLatch = countDownLatch;
    }

    public  void lectorImagenes() {
        try {
            // Crear un flujo de salida para el archivo ZIP de salida
            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(rutaArchivoSalida));
            int contador = 1;
            int numEntradas = 0;
            // Abrir el archivo ZIP de entrada
            ZipFile inputZip = new ZipFile(rutaArchivoEntrada);
            Enumeration<? extends ZipEntry> entries_count = inputZip.entries();
            while (entries_count.hasMoreElements()) {
                entries_count.nextElement();
                numEntradas++;
            }

            // Recorrer todos los archivos dentro del archivo ZIP de entrada
            Enumeration<? extends ZipEntry> entries = inputZip.entries();
            System.out.print('[');
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                double progress = contador;
                printProgressBar(progress);
                contador ++;

                // Obtener el nombre del archivo y su extensión
                String fileName = entry.getName();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                // Comprobar si el archivo es una imagen y tiene una extensión válida
                if (extension.matches("jpg|jpeg|png|bmp|gif")) {

                    // Leer la imagen desde el archivo ZIP de entrada
                    BufferedImage image = ImageIO.read(inputZip.getInputStream(entry));

                    // Crear una entrada ZIP para el archivo de imagen JPG de salida
                    ZipEntry jpgEntry = new ZipEntry(fileName.substring(0, fileName.lastIndexOf(".")) + ".jpg");
                    zipOut.putNextEntry(jpgEntry);

                    // Escribir la imagen en el archivo de salida como JPG
                    ImageIO.write(image, "jpg", zipOut);


                    // Cerrar la entrada ZIP y pasar a la siguiente imagen
                    zipOut.closeEntry();
                }
            }

            // Cerrar el archivo ZIP de entrada y salida
            inputZip.close();
            zipOut.close();

            System.out.println("");
            System.out.println("Archivos de imagen convertidos y comprimidos en el archivo ZIP de salida.");
            countDownLatch.countDown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printProgressBar(double progress) {
        StringBuilder progressBar = new StringBuilder();
        progressBar.append("[");
        int filledLength = (int) progress * 100;
        System.out.print(filledLength);
        for (int i = 0; i < 100; i++) {
            if (i < filledLength) {
                progressBar.append("=");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("] " + (int) progress + "%");
        System.out.print("\r" + progressBar.toString());
    }

}



