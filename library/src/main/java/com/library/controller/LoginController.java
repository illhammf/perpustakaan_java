package com.library.controller;

import com.library.Session;
import com.library.model.User;
import com.library.service.AuthService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

            User user = authService.login(
                    username == null ? null : username.trim(),
                    password
            );

            if (user == null) {
                errorLabel.setText("Username atau password salah.");
                return;
            }

            Session.setCurrentUser(user);

            String role = user.getRoleName() == null ? "" : user.getRoleName().trim().toUpperCase();

            String fxml;
            switch (role) {
                case "ADMIN" -> fxml = "/com/library/view/Admin.fxml";
                case "PUSTAKAWAN" -> fxml = "/com/library/view/Pustakawan.fxml";
                case "MAHASISWA" -> fxml = "/com/library/view/Mahasiswa.fxml";
                default -> throw new IllegalStateException("Role tidak dikenali: " + role);
            }

            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            stage.getScene().setRoot(root);

        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Terjadi kesalahan saat login/pindah halaman");
            String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            if (ex.getCause() != null) {
                msg += "\nCause: " + ex.getCause().getClass().getSimpleName() + ": " + ex.getCause().getMessage();
            }
            alert.setContentText(msg);
            alert.showAndWait();
        }
    }
}
