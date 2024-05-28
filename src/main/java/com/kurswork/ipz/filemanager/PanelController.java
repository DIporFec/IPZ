package com.kurswork.ipz.filemanager;

import javafx.event.ActionEvent;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.media.Media;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class PanelController implements Initializable {
    @FXML
    private TableView<FileInfo> filesTable;

    @FXML
    private ComboBox<String> disksBox;

     @FXML
    private TextField searchField;

    @FXML
    private TextField pathField;

    private Path currentPath;

    public Path getCurrentPath() {
        return currentPath;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFileTable();
        setupDisksBox();
        updateList(Paths.get("."));
        refreshFileList();
    }

    private void setupFileTable() {
        TableColumn<FileInfo, ImageView> fileTypeColumn = createFileTypeColumn();
        TableColumn<FileInfo, String> filenameColumn = createFilenameColumn();
        TableColumn<FileInfo, Long> fileSizeColumn = createFileSizeColumn();
        TableColumn<FileInfo, String> fileDateColumn = createFileDateColumn();

        filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
        filesTable.setRowFactory(this::createTableRow);
        filesTable.setOnMouseClicked(this::handleFileTableClick);
    }

    private TableColumn<FileInfo, ImageView> createFileTypeColumn() {
        TableColumn<FileInfo, ImageView> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(getImageView(param.getValue().getType())));
        fileTypeColumn.setPrefWidth(32);
        return fileTypeColumn;
    }

    private TableColumn<FileInfo, String> createFilenameColumn() {
        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Назва");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(300.0f);
        return filenameColumn;
    }

    private TableColumn<FileInfo, Long> createFileSizeColumn() {
        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Розмір");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatFileSize(item));
            }
        });
        fileSizeColumn.setPrefWidth(120);
        return fileSizeColumn;
    }

    private TableColumn<FileInfo, String> createFileDateColumn() {
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата зміни");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);
        return fileDateColumn;
    }

    private TableRow<FileInfo> createTableRow(TableView<FileInfo> tv) {
        TableRow<FileInfo> row = new TableRow<>();
        row.setOnDragDetected(event -> startDragAndDrop(row));
        row.setOnDragOver(this::acceptDrag);
        row.setOnDragDropped(event -> handleFileDrop(event, row));
        return row;
    }

    private void handleFileTableClick(javafx.scene.input.MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY && filesTable.getSelectionModel().getSelectedItem() != null) {
            showContextMenu(event.getScreenX(), event.getScreenY());
        } else if (event.getClickCount() == 2 && filesTable.getSelectionModel().getSelectedItem() != null) {
            handleFileDoubleClick();
        }
    }

    private void startDragAndDrop(TableRow<FileInfo> row) {
        if (!row.isEmpty() && row.getItem() != null ) {
            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(row.getItem().getFilename());
            db.setContent(content);
        }
    }

    private void acceptDrag(javafx.scene.input.DragEvent event) {
        if (event.getGestureSource() != event.getSource() && event.getDragboard().hasString()) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
        event.consume();
    }

    private void handleFileDrop(javafx.scene.input.DragEvent event, TableRow<FileInfo> row) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasString() && row.getItem() != null) {
            Path srcPath = Paths.get(pathField.getText()).resolve(db.getString());
            Path destPath = getDestinationPath(row, srcPath);
            try {
                Files.move(srcPath, destPath);
                updateList(currentPath);
                success = true;
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Помилка переміщення файлу", "Не вдалося перемістити файл.");
            }
        }
        event.setDropCompleted(success);
        event.consume();
    }

    private Path getDestinationPath(TableRow<FileInfo> row, Path srcPath) {
        Path destPath = Paths.get(pathField.getText()).resolve(row.getItem().getFilename());
        return Files.isDirectory(destPath) ? destPath.resolve(srcPath.getFileName()) : destPath;
    }

    private void handleFileDoubleClick() {
        Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
        if (Files.isDirectory(path)) {
            updateList(path);
        } else {
            openFile(path);
        }
    }

    private void setupDisksBox() {
        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0);
        disksBox.setOnAction(this::selectDiskAction);
    }

    public void updateList() {
        updateList(currentPath);
    }

    @FXML
    public void searchFiles(ActionEvent actionEvent) {
        String searchQuery = searchField.getText().trim().toLowerCase();
        if (searchQuery.isEmpty()) {
            updateList(currentPath);
            return;
        }
        try {
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(currentPath)
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(searchQuery))
                    .map(FileInfo::new).toList());
        } catch (IOException e) {
            showAlert(Alert.AlertType.WARNING, "Помилка пошуку", "Не вдалося знайти файли.");
        }
    }

    public void updateList(Path path) {
        try {
            currentPath = path.normalize().toAbsolutePath();
            pathField.setText(currentPath.toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).toList());
        } catch (IOException e) {
            showAlert(Alert.AlertType.WARNING, "Помилка оновлення", "Не вдалося оновити список файлів.");
        }
    }

    @FXML
    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = currentPath.getParent();
        if (upperPath != null) {
            updateList(upperPath);
        }
    }

    @FXML
    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public String getSelectedFilename() {
        return filesTable.isFocused() ? filesTable.getSelectionModel().getSelectedItem().getFilename() : null;
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openFile(Path path) {
        String fileType = getFileType(path);
        switch (fileType) {
            case "text":
                openTextFile(path);
                break;
            case "image":
                openImageFile(path);
                break;
            case "video":
                openVideoFile(path);
                break;
            default:
                showAlert(Alert.AlertType.ERROR, "Невідомий формат файлу", "Неможливо відкрити файл.");
                break;
        }
    }

    private String getFileType(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            if (mimeType != null) {
                if (mimeType.startsWith("text")) {
                    return "text";
                } else if (mimeType.startsWith("image")) {
                    return "image";
                } else if (mimeType.startsWith("video")) {
                    return "video";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    private void openTextFile(Path path) {
    try {
        String content = Files.readString(path);

        Stage stage = new Stage();
        stage.setTitle(path.getFileName().toString());

        TextArea textArea = new TextArea(content);
        textArea.setEditable(true);

        MenuBar menuBar = createTextFileMenuBar(path, textArea);

        VBox vbox = new VBox(menuBar, textArea);
        Scene scene = new Scene(vbox, 400, 200);
        stage.setScene(scene);
        stage.show();
    } catch (IOException e) {
        showAlert(Alert.AlertType.ERROR, "Помилка відкриття файлу", "Не вдалося відкрити файл.");
        }
    }


    private MenuBar createTextFileMenuBar(Path path, TextArea textArea) {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("Файл");
        MenuItem saveItem = new MenuItem("Зберегти");
        saveItem.setOnAction(event -> saveFile(path, textArea.getText()));
        fileMenu.getItems().add(saveItem);
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    private void openImageFile(Path path) {
        Stage stage = new Stage();
        stage.setTitle(path.getFileName().toString());

        Image image = new Image(path.toUri().toString());
        ImageView imageView = new ImageView(image);

        setImageViewProperties(stage, imageView);

        VBox vbox = new VBox(imageView);
        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
    }

    private void setImageViewProperties(Stage stage, ImageView imageView) {
        imageView.setFitWidth(Screen.getPrimary().getVisualBounds().getWidth() * 0.8);
        imageView.setFitHeight(Screen.getPrimary().getVisualBounds().getHeight() * 0.8);
        imageView.setPreserveRatio(true);

        imageView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleFullScreenImage(stage, imageView);
            }
        });
    }

    private void toggleFullScreenImage(Stage stage, ImageView imageView) {
        if (stage.isFullScreen()) {
            stage.setFullScreen(false);
            setImageViewProperties(stage, imageView);
        } else {
            stage.setFullScreen(true);
            imageView.setFitWidth(Screen.getPrimary().getVisualBounds().getWidth());
            imageView.setFitHeight(Screen.getPrimary().getVisualBounds().getHeight());
        }
        imageView.setPreserveRatio(true);
    }

    private void openVideoFile(Path path) {
        Stage stage = new Stage();
        stage.setTitle(path.getFileName().toString());

        Media media = new Media(path.toUri().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);

        setMediaViewProperties(stage, mediaView);

        Button playButton = new Button("Грати");
        playButton.setOnAction(event -> mediaPlayer.play());

        Button pauseButton = new Button("Пауза");
        pauseButton.setOnAction(event -> mediaPlayer.pause());

        Button rewindButton = new Button("Спочатку");
        rewindButton.setOnAction(event -> mediaPlayer.seek(mediaPlayer.getStartTime()));

        HBox controls = new HBox(10, playButton, pauseButton, rewindButton);
        controls.setStyle("-fx-alignment: center;");

        VBox vbox = new VBox(mediaView, controls);
        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();

        mediaPlayer.play();
    }

    private void setMediaViewProperties(Stage stage, MediaView mediaView) {
        mediaView.setFitWidth(Screen.getPrimary().getVisualBounds().getWidth() * 0.8);
        mediaView.setFitHeight(Screen.getPrimary().getVisualBounds().getHeight() * 0.8);
        mediaView.setPreserveRatio(true);

        mediaView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleFullScreenVideo(stage, mediaView);
            }
        });
    }

    private void toggleFullScreenVideo(Stage stage, MediaView mediaView) {
        if (stage.isFullScreen()) {
            stage.setFullScreen(false);
            setMediaViewProperties(stage, mediaView);
        } else {
            stage.setFullScreen(true);
            mediaView.setFitWidth(Screen.getPrimary().getVisualBounds().getWidth());
            mediaView.setFitHeight(Screen.getPrimary().getVisualBounds().getHeight());
        }
        mediaView.setPreserveRatio(true);
    }

    private void saveFile(Path path, String content) {
        try {
            Files.writeString(path, content);
            showAlert(Alert.AlertType.INFORMATION, "Файл збережено", "Файл успішно збережено.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Помилка збереження файлу", "Не вдалося зберегти файл.");
        }
    }

    private void showContextMenu(double x, double y) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("Видалити");
        deleteItem.setOnAction(event -> deleteSelectedFile());
        contextMenu.getItems().add(deleteItem);

        MenuItem createFileItem = new MenuItem("Створити файл");
        createFileItem.setOnAction(event -> createNewFile());
        contextMenu.getItems().add(createFileItem);

        MenuItem createDirItem = new MenuItem("Створити папку");
        createDirItem.setOnAction(event -> createNewDirectory());
        contextMenu.getItems().add(createDirItem);

        contextMenu.show(filesTable.getScene().getWindow(), x, y);
    }

    private void createNewFile() {
        TextInputDialog dialog = new TextInputDialog("newFile.txt");
        dialog.setTitle("Створити файл");
        dialog.setHeaderText("Створити новий файл");
        dialog.setContentText("Введіть ім'я файлу:");

        dialog.showAndWait().ifPresent(fileName -> {
            Path path = Paths.get(fileName);
            try {
                Files.createFile(path);
                showAlert(Alert.AlertType.INFORMATION, "Файл створено", "Файл успішно створено: " + path.toString());
                refreshFileList();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Помилка створення файлу", "Не вдалося створити файл.");
            }
        });
    }

    private void createNewDirectory() {
        TextInputDialog dialog = new TextInputDialog("newDirectory");
        dialog.setTitle("Створити папку");
        dialog.setHeaderText("Створити нову папку");
        dialog.setContentText("Введіть ім'я папки:");

        dialog.showAndWait().ifPresent(directoryName -> {
            Path path = Paths.get(directoryName);
            try {
                Files.createDirectory(path);
                showAlert(Alert.AlertType.INFORMATION, "Папка створена", "Папка успішно створена: " + path.toString());
                refreshFileList();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Помилка створення папки", "Не вдалося створити папку.");
            }
        });
    }

    private void refreshFileList() {
    }

    private void deleteSelectedFile() {
        FileInfo selectedItem = filesTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Path pathToFile = Paths.get(pathField.getText()).resolve(selectedItem.getFilename());
            if (!Files.isDirectory(pathToFile)) {
                try {
                    Files.delete(pathToFile);
                    updateList(currentPath);
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR, "Помилка видалення файлу", "Не вдалося видалити вибраний файл.");
                }
            }
        }
    }

    private ImageView getImageView(FileInfo.FileType type) {
        String imagePath = type == FileInfo.FileType.DIRECTORY ? "/folder-icon.png" : "/file-icon.png";
        return new ImageView(new Image(imagePath));
    }

    private String formatFileSize(Long size) {
        return size == -1L ? "[DIR]" : String.format("%,d bytes", size);
    }
}

