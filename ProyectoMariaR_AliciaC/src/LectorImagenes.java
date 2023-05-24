import javax.imageio.ImageIO;
import java.awt.image.WritableRaster;
import java.util.*;
import java.util.zip.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;


public class LectorImagenes {

    private final String rutaArchivoEntrada;
    private final String rutaArchivoSalida;
    private CountDownLatch countDownLatch;

    private final String filter;
    private final int filterValue;

    public LectorImagenes(String rutaArchivoEntrada, String rutaArchivoSalida, CountDownLatch countDownLatch, String filter, int filterValue) {
        this.rutaArchivoEntrada = rutaArchivoEntrada;
        this.rutaArchivoSalida = rutaArchivoSalida;
        this.countDownLatch = countDownLatch;
        this.filter = filter;
        this.filterValue = filterValue;
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
                    BufferedImage imageWithoutFilter = ImageIO.read(inputZip.getInputStream(entry));

                    BufferedImage image;

                    if (filter.equals("binarization")) {
                        image = binarization(imageWithoutFilter, filterValue);
                    } else if (filter.equals("negative")) {
                        image = negative(imageWithoutFilter);
                    } else if (filter.equals("averaging")) {
                        image = average(imageWithoutFilter, filterValue);
                    } else {
                        image = imageWithoutFilter;
                    }


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

    public static BufferedImage binarization(BufferedImage image, int threshold) {
        // Array de 3 posiciones donde se almacenan los valores R, G, B de cada píxel
        int[] pixelColors;
        int[] black = new int[3];
        int[] white = new int[3];

        // Inicializar los valores de los arrays de color
        for (int i = 0; i < 3; i++) {
            black[i] = 0;
            white[i] = 255;
        }

        // Creamos un "tesela" para poder modificar los píxeles de la imagen
        WritableRaster raster = (WritableRaster) image.getData();
        WritableRaster subRaster = raster.createWritableChild(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight(), 0, 0, null);

        // Iteración por cada píxel de la imagen
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                // Obtenemos los valores de color del píxel
                pixelColors = getPixelColor(image, x, y);

                // Calculamos la media de los valores de color para determinar si es negro o blanco
                double mean = pixelColors[0] + pixelColors[1] + pixelColors[2];
                mean = mean / 3;

                // Comparamos con el umbral y establecemos el color correspondiente
                if (mean <= threshold) {
                    raster.setPixel(x, y, black);
                } else {
                    raster.setPixel(x, y, white);
                }
            }
        }

        // Creamos una nueva imagen binarizada y la devolvemos
        BufferedImage binarizedImage = new BufferedImage(image.getColorModel(), subRaster, image.isAlphaPremultiplied(), null);
        return binarizedImage;
    }

    // Aplica el filtro de negativo a una BufferedImage
    public static BufferedImage negative(BufferedImage image) {
        // Array de 3 posiciones donde se almacenan los valores R, G, B de cada píxel
        int[] pixelColors;
        // Array de 3 posiciones donde se almacenarán los nuevos valores R, G, B negativos
        int[] negativeValues = new int[3];

        // Creamos un "tesela" a partir de la imagen original para poder modificar sus píxeles
        WritableRaster raster = (WritableRaster) image.getData();
        WritableRaster subRaster = raster.createWritableChild(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight(), 0, 0, null);

        // Iteración por cada píxel de la imagen
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                // Obtenemos los valores de color del píxel
                pixelColors = getPixelColor(image, x, y);

                // Aplicamos el filtro de negativo a cada componente de color
                for (int i = 0; i < 3; i++) {
                    // El filtro de negativo se logra restando cada componente del color original a 255
                    negativeValues[i] = 255 - pixelColors[i];
                }

                // Establecemos los nuevos valores de color negativo en el píxel correspondiente
                raster.setPixel(x, y, negativeValues);
            }
        }

        // Creamos una nueva imagen con el negativo y la devolvemos
        BufferedImage negativeImage = new BufferedImage(image.getColorModel(), subRaster, image.isAlphaPremultiplied(), null);
        return negativeImage;
    }

    // Aplica un filtro de promediado (averaging) a una BufferedImage, utilizando una ventana de tamaño 'value'
    public static BufferedImage average(BufferedImage image, int value) {
        // Array de 3 posiciones donde se almacenan los valores R, G, B de cada píxel
        int[] pixelColors;
        // Array de 3 posiciones donde se almacenarán los valores promedio de cada canal de color
        int[] meanColor = new int[3];
        // Valores promedio para cada canal de color
        double meanRed, meanGreen, meanBlue;

        // Creamos un "tesela" a partir de la imagen original para poder modificar sus píxeles
        WritableRaster raster = (WritableRaster) image.getData();
        WritableRaster subRaster = raster.createWritableChild(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight(), 0, 0, null);

        // Iteración por cada píxel de la imagen
        for (int x = 0; x < image.getWidth() - 1; x++) {
            for (int y = 0; y < image.getHeight() - 1; y++) {
                int red = 0, green = 0, blue = 0;

                // Iteración por cada píxel de la ventana
                for (int f = -value; f <= value; f++) {
                    for (int k = -value; k <= value; k++) {
                        // Obtenemos las coordenadas de cada píxel de la ventana,
                        // pero primero comprobamos si realmente existen (no fuera de los límites)
                        if (y + f >= 0 && x + k >= 0 && y + f < image.getHeight() && x + k < image.getWidth()) {
                            // Almacenamos temporalmente el valor de cada canal de color para luego calcular el promedio
                            pixelColors = getPixelColor(image, x + k, y + f);
                            red += pixelColors[0];
                            green += pixelColors[1];
                            blue += pixelColors[2];
                        }
                    }
                }

                // Para calcular el promedio de cada canal de color, debemos dividir por la cantidad de píxeles en la ventana
                int distance = (value - (-value) + 1) * (value - (-value) + 1);
                // Para cada canal de color, calculamos el nuevo valor del píxel
                meanRed = red / (double) distance;
                meanColor[0] = (int) meanRed;
                meanGreen = green / (double) distance;
                meanColor[1] = (int) meanGreen;
                meanBlue = blue / (double) distance;
                meanColor[2] = (int) meanBlue;

                // Establecemos los nuevos valores promedio de color en el píxel correspondiente
                raster.setPixel(x, y, meanColor);
            }
        }

        // Creamos una nueva imagen con el promedio y la devolvemos
        BufferedImage averagedImage = new BufferedImage(image.getColorModel(), subRaster, image.isAlphaPremultiplied(), null);
        return averagedImage;
    }

    // Función auxiliar para obtener los valores de color de un píxel
    private static int[] getPixelColor(BufferedImage image, int x, int y) {
        int color = image.getRGB(x, y);
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return new int[]{red, green, blue};
    }












}



