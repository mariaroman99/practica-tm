import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class CodificadorVideo {

    HashMap<Integer, Image> unzippedImg = new HashMap<Integer, Image>();
    int GOP = 10, seekRange, ntiles, quality;
    ArrayList<ArrayList> listaListasGOP = new ArrayList<ArrayList>();
    ArrayList<Imagen> listaGOP = new ArrayList<Imagen>();
    int height, width;
    ArrayList<Imagen> comprimides = new ArrayList<Imagen>();
    ArrayList<Tiles> TilesAcum = new ArrayList<>();

    public int count;
    String output;
    public CodificadorVideo(HashMap<Integer, Image> bufferWithUnzippedImg, int GOP, int ntiles, int seekRange, int quality, String output) {

        this.GOP = GOP;
        this.output = output;
        this.ntiles = ntiles;
        this.seekRange = seekRange;
        this.quality = quality;
        this.unzippedImg = bufferWithUnzippedImg;

    }


    public void GOPSeparation() {

        System.out.println("Separación en " + this.GOP + " imágenes"); // Imprime la cantidad de imágenes en cada separación GOP
        for (int x = 0; x < unzippedImg.size(); x++) {

            if (x % this.GOP == 0 || x + 1 >= unzippedImg.size()) {
                // Verifica si el índice actual es divisible por GOP o si es la última imagen

                if (!listaGOP.isEmpty()) {
                    if (x + 1 >= unzippedImg.size()) {
                        this.listaGOP.add(new Imagen((BufferedImage) unzippedImg.get(x), x)); // Agrega la última imagen a la lista GOP si es necesario
                    };
                    saveZip(listaGOP, "tempZip.zip"); // Guarda la lista GOP en un archivo zip llamado "nouZip.zip"
                    listaListasGOP.add(listaGOP); // Agrega la lista GOP actual a una lista de listas
                }
                listaGOP = new ArrayList<Imagen>(); // Reinicia la lista GOP para empezar una nueva lista
            }
            this.listaGOP.add(new Imagen((BufferedImage) unzippedImg.get(x), x)); // Agrega la imagen actual a la lista GOP
        }
        System.out.println("Ha finalizado la separación de imagenes"); // Imprime un mensaje indicando que el proceso ha finalizado
    }


    public void saveZip(ArrayList<Imagen> imgList, String outName){

        ZipOutputStream outputStreamZip = null;

        try {
            outputStreamZip = new ZipOutputStream(new FileOutputStream(outName));

            for (Imagen image : imgList) {
                // Crea una nueva entrada en el archivo zip para cada imagen
                ZipEntry entry = new ZipEntry("outImage" + image.getId() + ".jpg");
                outputStreamZip.putNextEntry(entry);
                // Escribe la imagen en el flujo del archivo zip
                ImageIO.write(image.getImage(), "jpg", outputStreamZip);
            }
            outputStreamZip.flush();
            outputStreamZip.close();
            this.count++; // Incrementa una variable de contador (supongo que está declarada fuera de este método)
        }
        catch (Exception e) {
            System.out.println("S'ha produit un error tancant la connexio"); // Imprime un mensaje de error en caso de excepción
        }

    }


    /**
     El método "recorrerGOP" selecciona "Tiles" compatibles con un "Imagen",
     calcula el promedio de color y guarda las imágenes resultantes en una lista comprimida.
     */
    public void code() throws IOException {
        System.out.println("Recorriendo las imagenes en GOP");
        GOPSeparation();

        Imagen imagen, imagen_siguiente;

        double totalIterations = listaListasGOP.size() * (listaListasGOP.get(0).size() - 1);
        double currentIteration = 0;
        Imagen im = (Imagen) listaListasGOP.get(0).get(0);
        this.width = im.getImage().getWidth() /this.ntiles;
        this.height = im.getImage().getHeight() / this.ntiles;
        for (int i = 0; i < listaListasGOP.size(); i++) {
            for (int z = 0; z < listaListasGOP.get(i).size() - 1; z++) {
                // Recorre cada elemento de una lista dentro de todas las listas
                imagen = (Imagen) listaListasGOP.get(i).get(z);

                if (z == 0) {
                    // Primera imagen que no puede aplicar el códec
                    comprimides.add(imagen);
                }

                imagen_siguiente = (Imagen) listaListasGOP.get(i).get(z + 1);

                // Subdivide la primera imagen en teselas
                imagen.setTiles(subdividirImgTiles(imagen.getImage()));

                // Compara las dos imágenes: las teselas y la siguiente imagen
                imagen.setTiles(matchTiles(imagen, imagen_siguiente.getImage()));

                // Crea un nuevo Imagen con los colores promedio de las teselas encontradas
                Imagen resultado = new Imagen(setColorbases(imagen.getTiles(), imagen_siguiente.getImage()), 5);
                comprimides.add(resultado);
                // Actualiza el progreso
                double progressPercentage = currentIteration / totalIterations;
                ProgressDemo.updateProgress(progressPercentage);
                currentIteration++;

                for (Tiles t : imagen.getTiles()) {
                    this.TilesAcum.add(t);
                }
            }
        }

        this.guardarZIP(); // Guarda los resultados en un archivo comprimido
    }


    /**
     El método subdividirImgTiles, a partir de una imagen pasada como parámetro,
     divide dicha imagen en teselas y las guarda en un ArrayList,
     el cual se devuelve como resultado de la función.

     */
    public ArrayList<Tiles> subdividirImgTiles(BufferedImage image) {
        ArrayList<Tiles> tilesList = new ArrayList<>();
        Tiles tile;
        int id = 0;

        // Itera sobre las coordenadas de las teselas
        for (float y = 0; y < Math.round(image.getHeight()); y += this.height) {
            for (float x = 0; x < Math.round(image.getWidth()); x += this.width) {
                x = Math.round(x); // Asegura que x sea un número entero
                y = Math.round(y); // Asegura que y sea un número entero

                // Crea una nueva tesela a partir de la subimagen correspondiente  con las coordenadas y la altura y ancho
                // el contador será nuestro id
                tile = new Tiles(image.getSubimage((int) x, (int) y, this.width, this.height), id);

                // Agrega la tesela a la lista de teselas
                tilesList.add(tile);
                id++;
            }
        }

        return tilesList; // Devuelve la lista de teselas generada
    }


    /**
     * La función matchTiles, dado un Imagen y una imagen, compara
     * las similitudes en les Tiles generadas en la imatge  con el Imagen , y
     * en caso de encontrar las similitudes, guarda les Tiles resultantes en una
     * variable de retorno.
     */
    private ArrayList<Tiles> matchTiles(Imagen destino, BufferedImage frames) {
        int coordX = 0;
        int coordY = 0;
        double valorFinal = 0;
        int x;
        int y;
        int id;

        ArrayList<Tiles> resultTiles = new ArrayList<>();

        int tileWidth = frames.getWidth()/ ntiles;
        int tileHeight = frames.getHeight()/ ntiles;

        for (Tiles t : destino.getTiles()) {
            valorFinal = 0;
            id = t.getId();
            // Coordenadas de la tesela
            x = ((int) Math.ceil((double) id / ntiles)) * tileHeight;
            y = (id % ntiles) * tileWidth;

            // Iteramos sobre las teselas y buscamos coincidencias mediante PSNR
            for (int i = Math.max((x - seekRange), 0); i <= Math.min((x + tileHeight + seekRange), frames.getHeight()) - tileHeight; i++) {
                for (int j = Math.max((y - seekRange), 0); j <= Math.min((y + tileWidth + seekRange), frames.getWidth()) - tileWidth; j++) {
                    double valor = funcioComparadora(t, frames.getSubimage(j, i, tileWidth, tileHeight));
                    if (valor >= quality) {
                        valorFinal = valor;
                        coordX = i;
                        coordY = j;
                    }
                }
            }
            // Si encontramos una coincidencia, establecemos las coordenadas de destino de la tesela
            if (valorFinal !=  0) {
                t.setXDest(coordX);
                t.setYDest(coordY);

            }
            else {
                t.setXDest(-1);
                t.setYDest(-1);
            } resultTiles.add(t);

        }

        return resultTiles;
    }


    /**
     * La funcio calcularRatioPSNR calcula donat un Imagen I i una imatge J, la relació
     * Peak signal-to-noise ratio entre el Imagen I i les Tiles de la imatge J.
     */
    private double funcioComparadora(Tiles tile, BufferedImage base) {
        BufferedImage destino = tile.getTiles();
        float redD = 0, greenD = 0, blueD = 0, redB = 0, greenB = 0, blueB = 0;
        // Calcula la diferencia al cuadrado entre los componentes RGB de cada píxel
        for (int i = 0; i < destino.getHeight(); i++) {
            for (int j = 0; j < destino.getWidth(); j++) {
                Color destino_rgb = new Color(destino.getRGB(j, i));
                redD += destino_rgb.getRed();
                greenD += destino_rgb.getGreen();
                blueD += destino_rgb.getBlue();

                Color base_rgb = new Color(base.getRGB(j, i));
                redB += base_rgb.getRed();
                greenB += base_rgb.getGreen();
                blueB += base_rgb.getBlue();

    }
        }
        float n  = this.width * this.height;
        return 10.0 * (Math.sqrt(redB/n - redD/n) + Math.sqrt(greenB/n - greenD/n) + Math.sqrt(blueB/n - blueD/n));

    }




/**
 * La funció medianaColor retorna la mediana del color dels pixels de una imatge.
 */
private Color calcularColorMedio(BufferedImage im) {
    int sumR = 0;
    int sumG = 0;
    int sumB = 0;
    int pixelCount = 0;

    // Iterar sobre los píxeles de la imagen y sumar los componentes RGB
    for (int y = 0; y < im.getHeight(); y++) {
        for (int x = 0; x < im.getWidth(); x++) {
            int rgb = im.getRGB(x, y);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

            sumR += red;
            sumG += green;
            sumB += blue;

            pixelCount++;
        }
    }

    // Calcular el promedio de los componentes RGB
    int avgR = sumR / pixelCount;
    int avgG = sumG / pixelCount;
    int avgB = sumB / pixelCount;

    return new Color(avgR, avgG, avgB); // Devolver el color medio calculado
}



    /**
     * La funcio setColorbases aplica el color encontrado por la mediana de color del base a
     * las teselas que dieron coincidencia.
     */
    private BufferedImage setColorbases(ArrayList<Tiles> tileList, BufferedImage base) {
        BufferedImage result = base;

        // Itera sobre las teselas y establece el color medio en la imagen resultante
        tileList.forEach((t) -> {
            int x = t.getXDest();
            int y = t.getYDest();

            // Verifica si la tesela tiene una posición válida
            if (x != -1 && y != -1) {
                Color c = calcularColorMedio(t.getTiles()); // Calcula el color medio de la tesela

                // Establece el color medio en los píxeles correspondientes en la imagen resultante
                for (int xCoord = x; xCoord < (x + height); xCoord++) {
                    for (int yCoord = y; yCoord < (y + width); yCoord++) {
                        result.setRGB(yCoord, xCoord, c.getRGB());
                    }
                }
            }
        });

        return result; // Devuelve la imagen resultante con los colores establecidos
    }




    public static void comprimirJPEG(BufferedImage image, String name, String outputIName) throws FileNotFoundException, IOException {
        File comprimidoImageFile = new File(name + "/" + outputIName);
        OutputStream outputStream = new FileOutputStream(comprimidoImageFile);
        float imageQuality = 1.0f;
        BufferedImage bufferedImage = image;

        // Obtener los escritores de imagen para el formato "jpg"
        Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName("jpg");

        ImageWriter imageWriter = (ImageWriter) imageWriters.next();

        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
        imageWriter.setOutput(imageOutputStream);
        ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();

        // Establecer la calidad de compresión
        imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        imageWriteParam.setCompressionQuality(imageQuality);

        // Escribir la imagen comprimida en el flujo de salida
        imageWriter.write(null, new IIOImage(bufferedImage, null, null), imageWriteParam);

        outputStream.close();
        imageOutputStream.close();
        imageWriter.dispose();
    }



    /**
     * la función guardarZIP se encarga de crear un zip con las imagenes y las coordenadas para
     * poder realizar la decodificacion.
     */
    private void guardarZIP() throws IOException {
        BufferedWriter bw = null;
        new File("output//ImagenesComprimidas").mkdirs();
        try {
            String name = "output/ImagenesComprimidas/coords.txt";
            bw = new BufferedWriter(new FileWriter(name));

            // Iterar sobre las teselas acumuladas y escribir las coordenadas en el archivo de texto
            for (Tiles t : this.TilesAcum) {
                bw.write(t.getId() + " " + t.getXDest() + " " + t.getYDest() + "\n");
            }

            bw.flush();
            bw.close();
        } catch (IOException ex) {
            System.err.println("Excepción IO: " + ex);
        }


        for (ArrayList<Imagen> p : listaListasGOP) {
            p.forEach((f) -> {
                try {
                    comprimirJPEG(f.getImage(), "output/ImagenesComprimidas/",  "imagen" +String.format("%02d", f.getId()) + ".jpeg");
                } catch (IOException ex) {
                    System.err.println("Excepcion IO detectada" + ex);
                }
            });
        }


        generateZip("output/ImagenesComprimidas", "output/"+ this.output);
        deleteDirectory(new File("output/ImagenesComprimidas"));
    }



    public void generateZip(String outputFolder, String destZipFile) {
        try {
            ZipOutputStream zipOut = null;
            FileOutputStream fileWriter = null;

            // Crear el flujo de salida para el archivo ZIP
            // Crear el objeto ZipOutputStream utilizando el flujo de salida
            zipOut = new ZipOutputStream(new FileOutputStream(destZipFile));

            // Llamar al método auxiliar para añadir el contenido del directorio a comprimir
            File folder = new File(outputFolder);

            for (String fileName : folder.list()) {
                if ("".equals("")) {
                    addFile(folder.getName(), outputFolder + "/" + fileName, zipOut);
                } else {
                    addFile("" + "/" + folder.getName(), outputFolder + "/" + fileName, zipOut);
                }
            }

            zipOut.flush();
            zipOut.close();
        } catch (FileNotFoundException ex) {
            // Manejar la excepción en caso de que no se encuentre el archivo
        } catch (IOException ex) {
            // Manejar la excepción de entrada/salida
        }
    }


    public boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String children1 : children) {
                boolean success = deleteDirectory(new File(dir, children1));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public void addDirectory(String path, String outputFolder, ZipOutputStream zip) {
        File folder = new File(outputFolder);

        for (String fileName : folder.list()) {
            if (path.equals("")) {
                addFile(folder.getName(), outputFolder + "/" + fileName, zip);
            } else {
                addFile(path + "/" + folder.getName(), outputFolder + "/" + fileName, zip);
            }
        }
    }

    public void addFile(String path, String outputFile, ZipOutputStream zip) {
        File folder = new File(outputFile);
        if (folder.isDirectory()) {
            // Si el archivo es un directorio, llamar al método addDirectory para añadir su contenido al archivo ZIP
            addDirectory(path, outputFile, zip);
        } else {
            FileInputStream in = null;
            try {
                byte[] buf = new byte[1024];
                int len;
                in = new FileInputStream(outputFile);
                // Crear una nueva entrada ZipEntry para el archivo y establecer su ruta en el archivo ZIP
                zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                while ((len = in.read(buf)) > 0) {
                    // Leer y escribir los datos del archivo en el archivo ZIP
                    zip.write(buf, 0, len);
                }
            } catch (FileNotFoundException ex) {
                // Manejar la excepción en caso de que no se encuentre el archivo
            } catch (IOException ex) {
                // Manejar la excepción de entrada/salida
            } finally {
                try {
                    // Cerrar el flujo de entrada
                    in.close();
                } catch (IOException ex) {
                    // Manejar la excepción de entrada/salida
                }
            }
        }
    }





}