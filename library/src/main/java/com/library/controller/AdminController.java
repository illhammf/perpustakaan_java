package com.library.controller;

import java.util.List;
import java.util.Optional;

import com.library.Session;
import com.library.model.Role;
import com.library.model.User;
import com.library.service.AdminService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class AdminController {

    private final AdminService service = new AdminService();

    @FXML private TextField searchField;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private Label msgLabel;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colUsername.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getUsername()));
        colFullName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFullName()));
        colRole.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRoleName()));
        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        msgLabel.setText("");
        try {
            userTable.getItems().setAll(service.getUsers(null));
        } catch (Exception ex) {
            msgLabel.setText("Gagal load user: " + ex.getMessage());
        }
    }

    @FXML
    public void handleSearch() {
        msgLabel.setText("");
        try {
            userTable.getItems().setAll(service.getUsers(searchField.getText()));
        } catch (Exception ex) {
            msgLabel.setText("Cari gagal: " + ex.getMessage());
        }
    }

    @FXML
    public void handleAdd() {
        try {
            User u = showUserDialog(null);
            if (u == null) return;

            service.addUser(u);
            msgLabel.setText("Berhasil tambah user.");
            handleRefresh();
        } catch (Exception ex) {
            msgLabel.setText(ex.getMessage());
        }
    }

    @FXML
    public void handleEdit() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("Pilih user dulu."); return; }

        try {
            User edited = showUserDialog(selected);
            if (edited == null) return;

            service.updateUser(edited);
            msgLabel.setText("Berhasil edit user.");
            handleRefresh();
        } catch (Exception ex) {
            msgLabel.setText(ex.getMessage());
        }
    }

    @FXML
    public void handleDelete() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { msgLabel.setText("Pilih user dulu."); return; }

        // Jangan biarkan admin menghapus dirinya sendiri
        User current = Session.getCurrentUser();
        if (current != null && current.getId() == selected.getId()) {
            msgLabel.setText("Tidak bisa menghapus akun yang sedang login.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi");
        confirm.setHeaderText("Hapus user?");
        confirm.setContentText("Username: " + selected.getUsername());

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            service.deleteUser(selected.getId());
            msgLabel.setText("Berhasil hapus user.");
            handleRefresh();
        } catch (Exception ex) {
            msgLabel.setText("Gagal hapus: " + ex.getMessage());
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            Session.clear();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/library/view/Login.fxml"));
            stage.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage());
            a.setHeaderText("Logout gagal");
            a.showAndWait();
        }
    }

    private User showUserDialog(User existing) throws Exception {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Tambah User" : "Edit User");

        ButtonType saveBtn = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField username = new TextField(existing == null ? "" : n(existing.getUsername()));
        PasswordField password = new PasswordField();
        password.setPromptText(existing == null ? "Wajib diisi" : "Kosongkan jika tidak ganti");
        TextField fullName = new TextField(existing == null ? "" : n(existing.getFullName()));

        ComboBox<Role> roleBox = new ComboBox<>();
        List<Role> roles = service.getRoles();
        roleBox.getItems().setAll(roles);
        roleBox.setPromptText("Pilih role");

        if (existing != null) {
            for (Role r : roles) {
                if (r.getId() == existing.getRoleId()) {
                    roleBox.getSelectionModel().select(r);
                    break;
                }
            }
        }

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.addRow(0, new Label("Username*"), username);
        gp.addRow(1, new Label(existing == null ? "Password*" : "Password (opsional)"), password);
        gp.addRow(2, new Label("Nama lengkap*"), fullName);
        gp.addRow(3, new Label("Role*"), roleBox);

        dialog.getDialogPane().setContent(gp);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            User u = new User();
            if (existing != null) u.setId(existing.getId());

            u.setUsername(username.getText());
            u.setPasswordHash(password.getText()); // kita pakai field ini untuk input password baru (plain)
            u.setFullName(fullName.getText());

            Role selectedRole = roleBox.getSelectionModel().getSelectedItem();
            if (selectedRole != null) {
                u.setRoleId(selectedRole.getId());
                u.setRoleName(selectedRole.getName());
            }
            return u;
        });

        Optional<User> res = dialog.showAndWait();
        return res.orElse(null);
    }

    private String n(String s) { return s == null ? "" : s; }
}
