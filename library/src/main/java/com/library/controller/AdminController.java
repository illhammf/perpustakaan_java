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
import javafx.scene.control.*;
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

    @FXML private Label loginInfoLabel;

    @FXML
    public void initialize() {
        // login info
        try {
            User current = Session.getCurrentUser();
            if (current != null) {
                loginInfoLabel.setText("Login sebagai: " + n(current.getFullName()) + " (Admin)");
            } else {
                loginInfoLabel.setText("Login sebagai: Admin");
            }
        } catch (Exception ignored) {
            loginInfoLabel.setText("Login sebagai: Admin");
        }

        // table binding
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colUsername.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getUsername())));
        colFullName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getFullName())));
        colRole.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getRoleName())));

        // ✅ ENTER di search field langsung cari
        if (searchField != null) {
            searchField.setOnAction(e -> handleSearch());
        }

        // ✅ load awal TANPA pesan
        refreshUsers(null, false);
    }

    // ==========================
    // LOAD / SEARCH
    // ==========================
    @FXML
    public void handleRefresh() {
        clearMsg();
        // refresh manual → boleh tampil pesan
        refreshUsers(null, true);
    }

    @FXML
    public void handleSearch() {
        clearMsg();
        String q = searchField == null ? null : searchField.getText();
        q = (q == null) ? null : q.trim();

        // ✅ kalau kosong, treat as null → tampil semua
        if (q != null && q.isBlank()) q = null;

        refreshUsers(q, true);
    }

    private void refreshUsers(String keyword, boolean showMessage) {
        try {
            userTable.getItems().setAll(service.getUsers(keyword));

            if (showMessage) {
                if (keyword == null) setMsgOk("Menampilkan semua user.");
                else setMsgOk("Hasil pencarian untuk: " + keyword);
            }
        } catch (Exception ex) {
            setMsgErr("Gagal load user: " + ex.getMessage());
            showError("Gagal load user", ex);
        }
    }

    // ==========================
    // CRUD
    // ==========================
    @FXML
    public void handleAdd() {
        clearMsg();
        try {
            User u = showUserDialog(null);
            if (u == null) return;

            // validasi add
            if (isBlank(u.getUsername())) { setMsgErr("Username wajib diisi."); return; }
            if (isBlank(u.getPasswordHash())) { setMsgErr("Password wajib diisi."); return; }
            if (isBlank(u.getFullName())) { setMsgErr("Nama lengkap wajib diisi."); return; }
            if (u.getRoleId() <= 0) { setMsgErr("Role wajib dipilih."); return; }

            service.addUser(u);
            setMsgOk("Berhasil tambah user.");
            refreshUsers(null, false); // setelah sukses, reload tanpa menimpa pesan sukses
        } catch (Exception ex) {
            setMsgErr("Gagal tambah: " + ex.getMessage());
            showError("Gagal tambah user", ex);
        }
    }

    @FXML
    public void handleEdit() {
        clearMsg();
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setMsgErr("Pilih user dulu."); return; }

        try {
            User edited = showUserDialog(selected);
            if (edited == null) return;

            // validasi edit
            if (isBlank(edited.getUsername())) { setMsgErr("Username wajib diisi."); return; }
            if (isBlank(edited.getFullName())) { setMsgErr("Nama lengkap wajib diisi."); return; }
            if (edited.getRoleId() <= 0) { setMsgErr("Role wajib dipilih."); return; }

            service.updateUser(edited);
            setMsgOk("Berhasil edit user.");
            refreshUsers(null, false);
        } catch (Exception ex) {
            setMsgErr("Gagal edit: " + ex.getMessage());
            showError("Gagal edit user", ex);
        }
    }

    @FXML
    public void handleDelete() {
        clearMsg();
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setMsgErr("Pilih user dulu."); return; }

        // cegah hapus diri sendiri
        try {
            User current = Session.getCurrentUser();
            if (current != null && current.getId() == selected.getId()) {
                setMsgErr("Tidak bisa menghapus akun yang sedang login.");
                return;
            }
        } catch (Exception ignored) {}

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi");
        confirm.setHeaderText("Hapus user?");
        confirm.setContentText("Username: " + selected.getUsername());

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            service.deleteUser(selected.getId());
            setMsgOk("Berhasil hapus user.");
            refreshUsers(null, false);
        } catch (Exception ex) {
            setMsgErr("Gagal hapus: " + ex.getMessage());
            showError("Gagal hapus user", ex);
        }
    }

    // ==========================
    // LOGOUT
    // ==========================
    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            Session.clear();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/library/view/Login.fxml"));
            stage.getScene().setRoot(root);
        } catch (Exception ex) {
            setMsgErr("Logout gagal: " + ex.getMessage());
            showError("Logout gagal", ex);
        }
    }

    // ==========================
    // DIALOG
    // ==========================
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
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(12));

        gp.addRow(0, new Label("Username*"), username);
        gp.addRow(1, new Label(existing == null ? "Password*" : "Password (opsional)"), password);
        gp.addRow(2, new Label("Nama lengkap*"), fullName);
        gp.addRow(3, new Label("Role*"), roleBox);

        dialog.getDialogPane().setContent(gp);

        Button btnSave = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        btnSave.disableProperty().bind(roleBox.getSelectionModel().selectedItemProperty().isNull());

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            User u = new User();
            if (existing != null) u.setId(existing.getId());

            u.setUsername(n(username.getText()).trim());
            u.setPasswordHash(n(password.getText())); // plain password (boleh kosong saat edit)
            u.setFullName(n(fullName.getText()).trim());

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

    // ==========================
    // UI HELPERS
    // ==========================
    private void clearMsg() {
        if (msgLabel == null) return;
        msgLabel.setText("");
    }

    private void setMsgOk(String msg) {
        if (msgLabel == null) return;
        msgLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: 800;");
        msgLabel.setText(msg);
    }

    private void setMsgErr(String msg) {
        if (msgLabel == null) return;
        msgLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 800;");
        msgLabel.setText(msg);
    }

    private void showError(String header, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(ex == null ? "Terjadi kesalahan." : ex.getMessage());
        a.showAndWait();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String n(String s) {
        return s == null ? "" : s;
    }
}
