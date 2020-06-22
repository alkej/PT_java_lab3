package pl.edu.pg;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class PrimaryController implements Initializable {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TableView<ImageProcessingJob> imageTableView;

    @FXML
    private TableColumn<ImageProcessingJob, String> imageNameColumn;

    @FXML
    private TableColumn<ImageProcessingJob, Double> progressColumn;

    @FXML
    private TableColumn<ImageProcessingJob, String> statusColumn;

    @FXML
    private Button convertButton;

    @FXML
    private Button filechooseButton;

    @FXML
    private ComboBox<String> poolchooseComboBox;

    @FXML
    private Text statusText;

    @FXML
    private Text modeText;

    @FXML
    private Button dirchooseButton;

    @FXML
    private AnchorPane anchorPane;

    private String currentMode;
    private File destinationDirectory;
    private ObservableList<ImageProcessingJob> jobs;
    private boolean comparedModeOn = false;
    private Map<Integer,Long> timeHistory;


    @FXML
    public void initialize(URL url, ResourceBundle rb) {
        assert imageNameColumn != null : "fx:id=\"imageNameColumn\" was not injected: check your FXML file 'primary.fxml'.";
        assert progressColumn != null : "fx:id=\"progressColumn\" was not injected: check your FXML file 'primary.fxml'.";
        assert statusColumn != null : "fx:id=\"statusColumn\" was not injected: check your FXML file 'primary.fxml'.";
        assert convertButton != null : "fx:id=\"convertButton\" was not injected: check your FXML file 'primary.fxml'.";
        assert filechooseButton != null : "fx:id=\"filechooseButton\" was not injected: check your FXML file 'primary.fxml'.";
        assert poolchooseComboBox != null : "fx:id=\"poolchooseComboBox\" was not injected: check your FXML file 'primary.fxml'.";
        assert statusText != null : "fx:id=\"statusText\" was not injected: check your FXML file 'primary.fxml'.";
        assert modeText != null : "fx:id=\"modeText\" was not injected: check your FXML file 'primary.fxml'.";


        this.jobs = FXCollections.observableArrayList();


        this.currentMode = "Sequentially";
        // adding values
        poolchooseComboBox.getItems().add("Compare*");
        poolchooseComboBox.getItems().add("Sequentially");
        poolchooseComboBox.getItems().add("CommonPool");
        poolchooseComboBox.getSelectionModel().select(this.currentMode);
        for (int i=1; i <= 4; i++)
            poolchooseComboBox.getItems().add(Integer.toString((int)Math.pow(2, i)) + " threads");


        imageNameColumn.setCellValueFactory( //nazwa pliku
                p -> new SimpleStringProperty(p.getValue().getFile().getName()));
        statusColumn.setCellValueFactory( //status przetwarzania
                p -> p.getValue().getStatusProperty());
        progressColumn.setCellFactory( //wykorzystanie paska postępu
                ProgressBarTableCell.<ImageProcessingJob>forTableColumn());
        progressColumn.setCellValueFactory( //postęp przetwarzania
                p -> p.getValue().getProgressProperty().asObject());

        imageTableView.setItems(this.jobs);


    }


    @FXML
    void chooseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JPG images", "*.jpg"));

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(filechooseButton.getScene().getWindow());

        if (selectedFiles != null){
            jobs.clear();

            selectedFiles.forEach(file -> jobs.add(new ImageProcessingJob(file)));

            if (this.destinationDirectory != null){
                jobs.forEach(job -> job.setDirectory(this.destinationDirectory));
            }
        }
    }

    @FXML
    void chooseDir(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        this.destinationDirectory = dirChooser.showDialog(convertButton.getScene().getWindow());
        if (this.destinationDirectory  != null){
            jobs.forEach(job -> job.setDirectory(destinationDirectory));
        }
    }

    @FXML
    void convert(ActionEvent event) {
       if (this.comparedModeOn){
           this.currentMode = "Sequentially";
           this.timeHistory = new HashMap<Integer, Long>();
           compareModeConvert();
       } else {
           startConverting(setUpPool(this.currentMode));
       }
    }


    @FXML
    void poolChoose(ActionEvent event) {
        String value = poolchooseComboBox.getValue();
        setUpPool(value);

        if (!value.equals("Compare*"))
            this.comparedModeOn = false;
    }

    private void startConverting(int nThreads){
        if (this.destinationDirectory != null){
            ForkJoinPool pool = null;
            if (nThreads == -1){ //CommonPool
                pool = new ForkJoinPool();
            }else {
                pool = new ForkJoinPool(nThreads);
            }
            pool.submit(() -> backgroundJob(nThreads));

        }else if(jobs.isEmpty()) {
            this.statusText.setText("Choose files!");
        }else{
            this.statusText.setText("Choose directory!");
        }
    }

    private int setUpPool(String value){
        int nThreads;

        switch (value) {
            case "Sequentially":
                nThreads = 1;
                break;

            case "CommonPool":
                nThreads = -1;
                break;

            case "Compare*":
                nThreads = -2;
                this.comparedModeOn = true;
                break;

            default:
                nThreads  = Integer.parseInt(value.split(" ")[0]);
                break;
        }

        if (!value.equals("Compare*")){
            if (!this.comparedModeOn)
                this.statusText.setText("Set " + value + " mode");
            this.currentMode = value;
        }else{
            this.statusText.setText("Compare modes execution time");
        }

        return nThreads;
    }

    private void compareModeConvert(){
        List<String> values = new ArrayList<>(this.poolchooseComboBox.getItems());
        values.remove("Compare*");

        for (String val : values){
            startConverting(setUpPool(val));
        }
    }

    private synchronized void backgroundJob(int nThreads){
        elementsVisability(false);

        String msg = nThreads == -1 ? "CommonPool": Integer.toString(nThreads);

        this.statusText.setText("Proccesing " + msg + " threads mode");

        long start = System.currentTimeMillis();

        jobs.forEach(ImageProcessingJob::clearStatus);
        jobs.parallelStream().forEach(ImageProcessingJob::convertToGrayscale);

        long end = System.currentTimeMillis();
        long duration = end-start;

        if (this.comparedModeOn && this.timeHistory != null){
            this.timeHistory.put(nThreads, duration);
        }

        elementsVisability(true);
        this.statusText.setText("Done in " + (double)duration/1000 + " s");

        if (this.timeHistory != null && this.comparedModeOn &&
                this.timeHistory.size() == this.poolchooseComboBox.getItems().size() -1){
            Platform.runLater(this::showCompareModeResult);
        }

    }

    private void elementsVisability(boolean status){
        convertButton.setDisable(!status);
        dirchooseButton.setDisable(!status);
        filechooseButton.setDisable(!status);

        poolchooseComboBox.setDisable(!status);
    }


    private void showCompareModeResult(){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Compare results");
        alert.setHeaderText(null);

        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<Integer, Long> entry : this.timeHistory.entrySet()){

            String msg;
            int keyValue = entry.getKey();

            if (keyValue == -1){
                msg = "CommonPool";
            }else if (entry.getKey().equals(1)){
                msg = "Sequentially";
            }else{
                msg = Integer.toString(keyValue) + " threads";
            }

            String tmp = msg + " mode done in " + (double)entry.getValue()/1000 + " s \n";
            stringBuilder.append(tmp);
        }

        alert.setContentText(stringBuilder.toString());
        alert.showAndWait();
    }
}
