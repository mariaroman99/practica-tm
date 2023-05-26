
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class CodificadorVideo {

    HashMap<Integer, Image> unzippedImg = new HashMap<Integer, Image>();
    int GOP = 10, seekRange, ntiles, quality;
    ArrayList<ArrayList> listaListasGOP = new ArrayList<ArrayList>();
    ArrayList<Marco> listaGOP = new ArrayList<Marco>();
    int height, width;
    ArrayList<Marco> comprimides = new ArrayList<Marco>();
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
                        this.listaGOP.add(new Marco((BufferedImage) unzippedImg.get(x), x)); // Agrega la última imagen a la lista GOP si es necesario
                    };
                    saveZip(listaGOP, "tempZip.zip"); // Guarda la lista GOP en un archivo zip llamado "nouZip.zip"
                    listaListasGOP.add(listaGOP); // Agrega la lista GOP actual a una lista de listas
                }
                listaGOP = new ArrayList<Marco>(); // Reinicia la lista GOP para empezar una nueva lista
            }
            this.listaGOP.add(new Marco((BufferedImage) unzippedImg.get(x), x)); // Agrega la imagen actual a la lista GOP
        }
        System.out.println("Ha finalizado la separación de imagenes"); // Imprime un mensaje indicando que el proceso ha finalizado
    }


    public void saveZip(ArrayList<Marco> imgList, String outName){

        ZipOutputStream outputStreamZip = null;

        try {
            outputStreamZip = new ZipOutputStream(new FileOutputStream(outName));

            for (Marco image : imgList) {
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
     El método "recorrerGOP" selecciona "Tiles" compatibles con un "Marco",
     calcula el promedio de color y guarda las imágenes resultantes en una lista comprimida.
     */
    public void recorrerGOP() {
        System.out.println("Recorriendo las imagenes en GOP");
        int x = 0;
        Marco n;
        Marco n_1;

        double totalIterations = listaListasGOP.size() * (listaListasGOP.get(0).size() - 1);
        double currentIteration = 0;

        for (int p = 0; p < listaListasGOP.size(); p++) {
            for (int z = 0; z < listaListasGOP.get(p).size() - 1; z++) {
                // Recorre cada elemento de una lista dentro de todas las listas
                n = (Marco) listaListasGOP.get(p).get(z);

                if (z == 0) {
                    // Primera imagen que no puede aplicar el códec
                    comprimides.add(n);
                }

                n_1 = (Marco) listaListasGOP.get(p).get(z + 1);
                // Calculamos el tamaño de las teselas
                this.width = n_1.getImage().getWidth() / this.ntiles;
                this.height = n_1.getImage().getHeight() / this.ntiles;

                // Subdivide la primera imagen en teselas
                n.setTiles(subdividirImgTiles(n.getImage()));

                // Compara las dos imágenes: las teselas y la siguiente imagen
                n.setTiles(matchTiles(n, n_1.getImage()));

                // Crea un nuevo Marco con los colores promedio de las teselas encontradas
                Marco resultado = new Marco(setColorPFrames(n.getTiles(), n_1.getImage()), 5);
                comprimides.add(resultado);
                // Actualiza el progreso
                double progressPercentage = currentIteration / totalIterations;
                ProgressDemo.updateProgress(progressPercentage);
                currentIteration++;

                for (Tiles t : n.getTiles()) {
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
        int contador = 0;

        // Itera sobre las coordenadas de las teselas
        for (float y = 0; y < Math.round(image.getHeight()); y += this.height) {
            for (float x = 0; x < Math.round(image.getWidth()); x += this.width) {
                x = Math.round(x); // Asegura que x sea un número entero
                y = Math.round(y); // Asegura que y sea un número entero

                // Crea una nueva tesela a partir de la subimagen correspondiente  con las coordenadas y la altura y ancho
                // el contador será nuestro id
                tile = new Tiles(image.getSubimage((int) x, (int) y, (int) this.width, (int) this.height), contador);

                // Agrega la tesela a la lista de teselas
                tilesList.add(tile);
                contador++;
            }
        }

        return tilesList; // Devuelve la lista de teselas generada
    }


    /**
     * La función matchTiles, dado un marco y una imagen, compara
     * las similitudes en les Tiles generadas en la imatge  con el Marco , y
     * en caso de encontrar las similitudes, guarda les Tiles resultantes en una
     * variable de retorno.
     */
    private ArrayList<Tiles> matchTiles(Marco iFrame, BufferedImage pFrame) {
        float maxPSNR;
        int xMaxValue = 0;
        int yMaxValue = 0;
        int x, y, minX, maxX, minY, maxY, id;
        ArrayList<Tiles> teselesResultants = new ArrayList<>();

        for (Tiles t : iFrame.getTiles()) {
            maxPSNR = Float.MIN_VALUE;
            id = t.getId();
            // Tamaño de las teselas
            x = ((int) Math.ceil((double) id / ntiles)) * height;
            y = (id % ntiles) * width;
            // Valor mínimo después de la exploración
            minX = Math.max((x - seekRange), 0);
            minY = Math.max((y - seekRange), 0);
            // Valor máximo después de la exploración
            maxX = Math.min((x + height + seekRange), (((BufferedImage) unzippedImg.get(0)).getHeight()));
            maxY = Math.min((y + width + seekRange), (((BufferedImage) unzippedImg.get(0)).getWidth()));

            // Iteramos sobre las teselas y buscamos coincidencias mediante PSNR
            for (int i = minX; i <= maxX - height; i++) {
                for (int j = minY; j <= maxY - width; j++) {
                    float psnr = calcularRatioPSNR(t, pFrame.getSubimage(j, i, width, height));
                    if (psnr > maxPSNR && psnr >= quality) {
                        maxPSNR = psnr;
                        xMaxValue = i;
                        yMaxValue = j;
                    }
                }
            }

            // Si no encontramos más coincidencias, pasamos a la siguiente tesela.
            if (maxPSNR != Float.MIN_VALUE) {
                t.setXDest(xMaxValue);
                t.setYDest(yMaxValue);
            }
            else {
                t.setXDest(-1);
                t.setYDest(-1);
            }
            teselesResultants.add(t);
        }

        return teselesResultants;
    }


    /**
     * La funcio calcularRatioPSNR calcula donat un Marco I i una imatge J, la relació
     * Peak signal-to-noise ratio entre el Marco I i les Tiles de la imatge J.
     */
    private float calcularRatioPSNR(Tiles tile, BufferedImage pframe) {
        float dif = 0, mse = 0, psnr = 0;
        BufferedImage iFrame = tile.getTiles();

        // Calcula la diferencia al cuadrado entre los componentes RGB de cada píxel
        for (int i = 0; i < iFrame.getHeight(); i++) {
            for (int j = 0; j < iFrame.getWidth(); j++) {
                Color iframe_rgb = new Color(iFrame.getRGB(j, i));
                Color pframe_rgb = new Color(pframe.getRGB(j, i));


                // Extraer los componentes RGB de los píxeles y calculamos la la diferencia al cuadrado para cada componente RGB
                dif = (float) (dif + Math.pow(pframe_rgb.getRed() - iframe_rgb.getRed(), 2));
                dif = (float) (dif + Math.pow(pframe_rgb.getGreen() - iframe_rgb.getGreen(), 2));
                dif = (float) (dif + Math.pow(pframe_rgb.getBlue() - iframe_rgb.getBlue(), 2));
            }
        }

        // Calcula el error cuadrático medio (MSE) y el ratio PSNR
        mse = dif/ (iFrame.getHeight() * iFrame.getWidth() * 3);
        psnr = (float) (10 * Math.log10((255 * 255) / mse));

        return psnr; // Devuelve el ratio PSNR calculado
    }


    /**
     * La funció medianaColor retorna la mediana del color dels pixels de una imatge.
     */
    private Color medianaColor(BufferedImage im) {
        Color color;
        int sumR = 0;
        int sumG = 0;
        int sumB = 0;
        int pixelCount = 0;
        int red, green, blue;

        // Calcula la suma de los componentes RGB de cada píxel
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                color = new Color(im.getRGB(x, y));
                pixelCount++;
                sumR = sumR + color.getRed();
                sumG = sumG + color.getGreen();
                sumB = sumB + color.getBlue();
            }
        }

        // Calcula el valor medio de los componentes RGB
        red = sumR / pixelCount;
        green = sumG / pixelCount;
        blue = sumB / pixelCount;

        return new Color(red, green, blue); // Devuelve el color medio calculado
    }


    /**
     * La funcio setColorPFrames aplica el color encontrado por la mediana de color del pFrame a
     * las teselas que dieron coincidencia.
     */
    private BufferedImage setColorPFrames(ArrayList<Tiles> tileList, BufferedImage pFrame) {
        BufferedImage result = pFrame;

        // Itera sobre las teselas y establece el color medio en la imagen resultante
        tileList.forEach((t) -> {
            int x = t.getXDest();
            int y = t.getYDest();

            // Verifica si la tesela tiene una posición válida
            if (x != -1 && y != -1) {
                Color c = medianaColor(t.getTiles()); // Calcula el color medio de la tesela

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


    /**
     * La funcion guardarImagenes garda las imagenes una vez codificadas.
     */
    private void guardarImagenes() {
        for (ArrayList<Marco> p : listaListasGOP) {
            p.forEach((f) -> {
                try {
                    compressInJPEG(f.getImage(), "output/Compressed/",  "marco" +String.format("%02d", f.getId()) + ".jpeg");
                } catch (IOException ex) {
                    System.err.println("Excepcion IO detectada" + ex);
                }
            });
        }
    }

    public static void compressInJPEG(BufferedImage image, String name, String outputIName) throws FileNotFoundException, IOException {
        File comprimidoImageFile = new File(name + "/" + outputIName);
        OutputStream outputStream = new FileOutputStream(comprimidoImageFile);
        float imageQuality = 1.0f;
        BufferedImage bufferedImage = image;

        // Obtener los escritores de imagen para el formato "jpg"
        Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName("jpg");
        if (!imageWriters.hasNext()) {
            throw new IllegalStateException("¡Escritores no encontrados!");
        }
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
    private void guardarZIP() {
        new File("output//Compressed").mkdirs();
        crearCoordenadas(this.TilesAcum);
        this.guardarImagenes();
        crearCarpetaZIP("output/Compressed", "output/"+ this.output);
        File outptFile = new File("output/" + this.output);
        deleteDirectory(new File("output/Compressed"));
    }

    public void crearCoordenadas(ArrayList<Tiles> acumTiles) {
        BufferedWriter bw = null;
        try {
            String name = "output/Compressed/coords.txt";
            bw = new BufferedWriter(new FileWriter(name));

            // Iterar sobre las teselas acumuladas y escribir las coordenadas en el archivo de texto
            for (Tiles t : acumTiles) {
                bw.write(t.getId() + " " + t.getXDest() + " " + t.getYDest() + "\n");
            }

            bw.flush();
            bw.close();
        } catch (IOException ex) {
            System.err.println("Excepción IO: " + ex);
        }
    }



    public void crearCarpetaZIP(String outputFolder, String destZipFile) {
        try {
            ZipOutputStream zip = null;
            FileOutputStream fileWriter = null;

            // Crear el flujo de salida para el archivo ZIP
            fileWriter = new FileOutputStream(destZipFile);

            // Crear el objeto ZipOutputStream utilizando el flujo de salida
            zip = new ZipOutputStream(fileWriter);

            // Llamar al método auxiliar para añadir el contenido del directorio a comprimir
            addDirectory("", outputFolder, zip);

            zip.flush();
            zip.close();
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