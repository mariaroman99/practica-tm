import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.zip.*;
import java.util.concurrent.CountDownLatch;

import javax.imageio.*;
import javax.swing.*;

public class ReproductorImagenes {

    private final String archivoZip;
    private final int fps;
    private CountDownLatch countDownLatch;




    public ReproductorImagenes(String archivoZip, int fps, CountDownLatch countDownLatch) {
        this.archivoZip = archivoZip;
        this.fps = fps;
        this.countDownLatch = countDownLatch;

    }

    public void reproducir() throws IOException, InterruptedException {
        countDownLatch.await();
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archivoZip));
        ZipEntry zipEntry;

        // Obtener la frecuencia de actualización de la imagen
        long tiempoPorFotograma = 1000 / fps;


        // Crear la ventana para mostrar las imágenes
        JFrame ventana = new JFrame();
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setVisible(true);

        // Lista para almacenar las imágenes
        List<BufferedImage> imagenes = new ArrayList<>();

        // Recorrer las entradas del archivo ZIP
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            String nombreArchivo = zipEntry.getName();

            BufferedImage imagen = ImageIO.read(zipInputStream);


                // Agregar la imagen a la lista
            imagenes.add(imagen);


            // Saltar la entrada si no es un archivo JPEG
            zipInputStream.closeEntry();
        }

        // Recorrer la lista de imágenes y mostrar cada una durante el tiempo de un fotograma
        for (BufferedImage imagen : imagenes) {
            JLabel etiqueta = new JLabel(new ImageIcon(imagen));
            ventana.getContentPane().removeAll();
            ventana.getContentPane().add(etiqueta);
            ventana.pack();
            Thread.sleep(tiempoPorFotograma);
        }

        // Cerrar los streams
        zipInputStream.close();

    }


}

