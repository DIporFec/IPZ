package com.kurswork.ipz.filemanager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Controller {
    @FXML
    private VBox leftPanel, rightPanel;


    @FXML
    public void copyBtnAction(ActionEvent actionEvent) {
        handleFileAction(this::copyFile);
    }

    @FXML
    public void deleteBtnAction(ActionEvent actionEvent) {
        handleFileAction(this::deleteFile);
    }

    private void handleFileAction(FileAction action) {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            showAlert(Alert.AlertType.ERROR, "Помилка", "Файл не вибрано.");
            return;
        }

        PanelController srcPC = leftPC.getSelectedFilename() != null ? leftPC : rightPC;
        PanelController dstPC = leftPC.getSelectedFilename() != null ? rightPC : leftPC;

        Path srcFile = srcPC.getCurrentPath().resolve(srcPC.getSelectedFilename());
        Path dstDir = dstPC.getCurrentPath();

        try {
            action.execute(srcFile, dstDir);
            dstPC.updateList();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Помилка", "Помилка операції з файлом.");
        }
    }

    private void copyFile(Path srcFile, Path dstDir) throws IOException {
        Files.copy(srcFile, dstDir.resolve(srcFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteFile(Path srcFile, Path dstDir) throws IOException {
        Files.delete(srcFile);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FunctionalInterface
    private interface FileAction {
        void execute(Path srcFile, Path dstDir) throws IOException;
    }
}
