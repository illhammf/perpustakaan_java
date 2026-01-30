package com.library.controller;

import com.library.model.User;
import com.library.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleLogin(ActionEvent event) {
        errorLabel.setText("");

        try {
            String username = usernameField.getText();
            String password = passwordField.getText();

            User user = authService.login(username, password);

            if (user == null) {
                errorLabel.setText("Username atau password salah.");
                return;
            }

            // Redirect berdasarkan role
            String role = user.getRoleName();
            String fxml;

            switch (role) {
                case "ADMIN" -> fxml = "/com/library/view/Admin.fxml";
                case "PUSTAKAWAN" -> fxml = "/com/library/view/Pustakawan.fxml";
                case "MAHASISWA" -> fxml = "/com/library/view/Mahasiswa.fxml";
                default -> throw new IllegalStateException("Role tidak dikenali: " + role);
            }

            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            stage.getScene().setRoot(root);

        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage()); // "wajib diisi"
        } catch (Exception ex) {
            // Ini memenuhi revisi: kalau error harus ada pesan
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Terjadi kesalahan");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }
}
