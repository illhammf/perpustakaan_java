package com.library.controller;

import com.library.Session;
import com.library.model.User;
import com.library.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleLogin(ActionEvent event) {

        clearError();

        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        /* ================= VALIDASI INPUT ================= */
        if (username.isEmpty() && password.isEmpty()) {
            setError("Username dan password wajib diisi.");
            return;
        }
        if (username.isEmpty()) {
            setError("Username tidak boleh kosong.");
            return;
        }
        if (password.isEmpty()) {
            setError("Password tidak boleh kosong.");
            return;
        }

        try {
            /* ================= AUTH ================= */
            User user = authService.login(username, password);

            if (user == null) {
                setError("Username atau password salah.");
                return;
            }

            /* ================= SIMPAN SESSION ================= */
            Session.setCurrentUser(user);

            /* ================= ROUTING ROLE ================= */
            String role = user.getRoleName();
            String target;

            if (role == null) {
                showAlert("Error", "Role user tidak ditemukan.", Alert.AlertType.ERROR);
                return;
            }

            switch (role.toUpperCase()) {
                case "ADMIN" -> target = "/com/library/view/Admin.fxml";
                case "PUSTAKAWAN" -> target = "/com/library/view/Pustakawan.fxml";
                case "MAHASISWA" -> target = "/com/library/view/Mahasiswa.fxml";
                default -> {
                    showAlert("Error", "Role tidak dikenali: " + role, Alert.AlertType.ERROR);
                    return;
                }
            }

            Parent root = FXMLLoader.load(getClass().getResource(target));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(
                    "Error Sistem",
                    "Terjadi kesalahan saat login.\nSilakan coba lagi.",
                    Alert.AlertType.ERROR
            );
        }
    }

    /* ================= ACTION LAIN ================= */

    @FXML
    public void handleForgotPassword() {
        showAlert(
                "Lupa Password",
                "Silakan hubungi Admin untuk reset password.",
                Alert.AlertType.INFORMATION
        );
    }

    @FXML
    public void handleGoogleLogin() {
        showAlert(
                "Info",
                "Login Google belum tersedia pada versi ini.",
                Alert.AlertType.INFORMATION
        );
    }

    /* ================= UTIL ================= */

    private void setError(String msg) {
        errorLabel.setText(msg);
    }

    private void clearError() {
        errorLabel.setText("");
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
