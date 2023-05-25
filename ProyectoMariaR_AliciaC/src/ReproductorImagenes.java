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
 




    public ReproductorImagenes(String archivoZip, int fps) {
        this.archivoZip = archivoZip;
        this.fps = fps;
       
    }

    public void reproducir() throws IOException, InterruptedException {
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archivoZip));
        ZipEntry zipEntry ;

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
            BufferedImage imagen = ImageIO.read(zipInputStream);


                // Agregar la imagen a la lista
            imagenes.add(imagen);


            // Saltar la entrada si no es un archivo JPEG
            zipInputStream.closeEntry();
        }

        // Recorrer la lista de imágenes y mostrar cada una durante el tiempo de un fotograma
        for (int i = 0; i < imagenes.size(); i++) {
            BufferedImage imagen = imagenes.get(i);
            JLabel etiqueta = new JLabel(new ImageIcon(imagen));
            ventana.getContentPane().removeAll();
            ventana.getContentPane().add(etiqueta);
            ventana.pack();
            Thread.sleep(tiempoPorFotograma);

            // Si es la última imagen
            if (i == imagenes.size() - 1) {
                // Cierra la ventana
                ventana.dispose();
            }
        }

        // Cerrar los streams
        zipInputStream.close();

    }


}

