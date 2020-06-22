package pl.edu.pg;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import java.awt.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class ImageProcessingJob {

    private SimpleStringProperty status;
    private DoubleProperty progress;
    private File file;

    private File directory;

    ImageProcessingJob(File file){
        this.file = file;
        this.status = new SimpleStringProperty("Waiting");
        this.progress = new SimpleDoubleProperty(0);
    }

    File getFile(){
        return this.file;
    }

    SimpleStringProperty getStatusProperty(){
        return this.status;
    }

    DoubleProperty getProgressProperty(){
        return this.progress;
    }

    void setDirectory(File dir){
        this.directory = dir;
    }

    void clearStatus(){
        this.status.set("Waiting");
        this.progress.setValue(0);
    }

    void convertToGrayscale() {
        try {
            BufferedImage original = ImageIO.read(this.file);
            BufferedImage grayscale = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());

            for (int i = 0; i < original.getWidth(); i++) {
                for (int j = 0; j < original.getHeight(); j++) {

                    int red = new Color(original.getRGB(i, j)).getRed();
                    int green = new Color(original.getRGB(i, j)).getGreen();
                    int blue = new Color(original.getRGB(i, j)).getBlue();


                    int luminosity = (int) (0.21*red + 0.71*green + 0.07*blue);
                    int newPixel = new Color(luminosity, luminosity, luminosity).getRGB();
                    grayscale.setRGB(i, j, newPixel);
                }
                double progress = (1.0 + i) / original.getWidth();
                Platform.runLater(() -> {
                    this.progress.set(progress);
                    this.status.set("Processing");
                });
            }
            Path outputPath = Paths.get(this.directory.getAbsolutePath(), this.file.getName());

            ImageIO.write(grayscale, "jpg", outputPath.toFile());

            Platform.runLater(() -> this.status.set("Done"));

        } catch (IOException ex) {
            ex.getStackTrace();
        }
    }

}
