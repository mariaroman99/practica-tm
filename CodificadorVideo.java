import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class CodificadorVideo {

    private final String rutaArchivoEntrada;
    private final String rutaArchivoSalida;
    private final int GOP;


    public CodificadorVideo(String rutaArchivoEntrada, String rutaArchivoSalida, int GOP) {
        this.rutaArchivoEntrada = rutaArchivoEntrada;
        this.rutaArchivoSalida = rutaArchivoSalida;
        this.GOP = GOP;

    }

    public void codificador() throws IOException {

        try (ZipFile zipFile = new ZipFile(rutaArchivoSalida)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            List<ZipEntry> imageEntries = new ArrayList<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    imageEntries.add(entry);
                }
            }

            if (!imageEntries.isEmpty()) {
                Collections.sort(imageEntries, Comparator.comparing(ZipEntry::getName));

                int totalImages = imageEntries.size();
                int zipFilesCount = (int) Math.ceil((double) totalImages / GOP);

                int imageIndex = 0;
                int zipIndex = 0;

                while (imageIndex < totalImages) {
                    String zipFileName = zipIndex + "_" +  rutaArchivoSalida ;
                    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFileName))) {
                        for (int i = 0; i < GOP && imageIndex < totalImages; i++) {
                            ZipEntry imageEntry = imageEntries.get(imageIndex);
                            InputStream inputStream = zipFile.getInputStream(imageEntry);

                            BufferedImage image = ImageIO.read(inputStream);
                            String imageName = imageEntry.getName();


                            zipOutputStream.putNextEntry(new ZipEntry(imageName));
                            ImageIO.write(image, "jpg", zipOutputStream);

                            inputStream.close();
                            zipOutputStream.closeEntry();
                            imageIndex++;
                        }
                    }

                    zipIndex++;
                }

                System.out.println("Splitting ZIP file into image files completed.");
            } else {
                System.out.println("No image files found in the ZIP file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
