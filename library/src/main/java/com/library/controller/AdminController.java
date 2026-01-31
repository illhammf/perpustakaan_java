package com.library.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.library.Session;
import com.library.model.Loan;
import com.library.model.Role;
import com.library.model.User;
import com.library.service.AdminService;
import com.library.util.DialogUtil;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class AdminController {

    private final AdminService service = new AdminService();

    // ===== USER UI =====
    @FXML private TextField searchField;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private Label msgLabel;
    @FXML private Label loginInfoLabel;

    // ===== LOAN UI (BARU) =====
    @FXML private TableView<Loan> loanTable;
    @FXML private TableColumn<Loan, Integer> colLoanId;
    @FXML private TableColumn<Loan, Integer> colLoanUser;
    @FXML private TableColumn<Loan, String> colLoanBook;
    @FXML private TableColumn<Loan, String> colLoanDate;
    @FXML private TableColumn<Loan, String> colDueDate;
    @FXML private TableColumn<Loan, String> colStatus;
    @FXML private TableColumn<Loan, Integer> colFine;
    @FXML private Label loanMsgLabel;

    @FXML
    public void initialize() {
        // login info
        try {
            User current = Session.getCurrentUser();
            if (current != null) loginInfoLabel.setText("Login sebagai: " + n(current.getFullName()) + " (Admin)");
            else loginInfoLabel.setText("Login sebagai: Admin");
        } catch (Exception ignored) {
            loginInfoLabel.setText("Login sebagai: Admin");
        }

        // ===== USER TABLE =====
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colUsername.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getUsername())));
        colFullName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getFullName())));
        colRole.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getRoleName())));

        if (searchField != null) searchField.setOnAction(e -> handleSearch());
        refreshUsers(null, false);

        // ===== LOAN TABLE =====
        if (colLoanId != null) {
            colLoanId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        }
        if (colLoanUser != null) {
            colLoanUser.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getUserId()));
        }
        if (colLoanBook != null) {
            colLoanBook.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getBookTitle())));
        }
        if (colLoanDate != null) {
            colLoanDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                    c.getValue().getLoanDate() == null ? "-" : c.getValue().getLoanDate().toString()
            ));
        }
        if (colDueDate != null) {
            colDueDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                    c.getValue().getDueDate() == null ? "-" : c.getValue().getDueDate().toString()
            ));
        }
        if (colStatus != null) {
            colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getStatus())));
        }
        if (colFine != null) {
            colFine.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getFine()));
        }

        handleRefreshLoansSilently();
    }

    // ==========================
    // USER: LOAD / SEARCH
    // ==========================
    @FXML
    public void handleRefresh() {
        clearMsg();
        refreshUsers(null, true);

        // sekalian refresh loans biar admin 1 tombol beres
        handleRefreshLoans();
    }

    @FXML
    public void handleSearch() {
        clearMsg();

        String q = searchField == null ? null : searchField.getText();
        q = (q == null) ? null : q.trim();
        if (q != null && q.isBlank()) q = null;

        refreshUsers(q, true);
    }

    private void refreshUsers(String keyword, boolean showMessage) {
        try {
            List<User> users = service.getUsers(keyword);
            userTable.getItems().setAll(users);

            if (showMessage) {
                if (keyword == null) {
                    setMsgOk("Data user diperbarui.");
                } else {
                    if (users == null || users.isEmpty()) setMsgErr("Tidak ada user untuk: " + keyword);
                    else setMsgOk("Ditemukan " + users.size() + " user untuk: " + keyword);
                }
            }
        } catch (Exception ex) {
            setMsgErr("Gagal load user: " + ex.getMessage());
            DialogUtil.showError("Gagal load user", ex);
        }
    }

    // ==========================
    // USER: CRUD
    // ==========================
    @FXML
    public void handleAdd() {
        clearMsg();
        try {
            User u = showUserDialog(null);
            if (u == null) return;

            if (isBlank(u.getUsername())) { setMsgErr("Username wajib diisi."); DialogUtil.showWarning("Input kurang", "Username wajib diisi."); return; }
            if (isBlank(u.getPasswordHash())) { setMsgErr("Password wajib diisi."); DialogUtil.showWarning("Input kurang", "Password wajib diisi."); return; }
            if (isBlank(u.getFullName())) { setMsgErr("Nama lengkap wajib diisi."); DialogUtil.showWarning("Input kurang", "Nama lengkap wajib diisi."); return; }
            if (u.getRoleId() <= 0) { setMsgErr("Role wajib dipilih."); DialogUtil.showWarning("Input kurang", "Role wajib dipilih."); return; }

            service.addUser(u);
            setMsgOk("Berhasil tambah user.");
            DialogUtil.showSuccess("Sukses", "User berhasil ditambahkan.");
            refreshUsers(null, false);
        } catch (Exception ex) {
            setMsgErr("Gagal tambah: " + ex.getMessage());
            DialogUtil.showError("Gagal tambah user", ex);
        }
    }

    @FXML
    public void handleEdit() {
        clearMsg();
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setMsgErr("Pilih user dulu.");
            DialogUtil.showInfo("Belum dipilih", "Pilih user yang ingin diedit terlebih dahulu.");
            return;
        }

        try {
            User edited = showUserDialog(selected);
            if (edited == null) return;

            if (isBlank(edited.getUsername())) { setMsgErr("Username wajib diisi."); DialogUtil.showWarning("Input kurang", "Username wajib diisi."); return; }
            if (isBlank(edited.getFullName())) { setMsgErr("Nama lengkap wajib diisi."); DialogUtil.showWarning("Input kurang", "Nama lengkap wajib diisi."); return; }
            if (edited.getRoleId() <= 0) { setMsgErr("Role wajib dipilih."); DialogUtil.showWarning("Input kurang", "Role wajib dipilih."); return; }

            service.updateUser(edited);
            setMsgOk("Berhasil edit user.");
            DialogUtil.showSuccess("Sukses", "User berhasil diperbarui.");
            refreshUsers(null, false);
        } catch (Exception ex) {
            setMsgErr("Gagal edit: " + ex.getMessage());
            DialogUtil.showError("Gagal edit user", ex);
        }
    }

    @FXML
    public void handleDelete() {
        clearMsg();
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setMsgErr("Pilih user dulu.");
            DialogUtil.showInfo("Belum dipilih", "Pilih user yang ingin dihapus terlebih dahulu.");
            return;
        }

        try {
            User current = Session.getCurrentUser();
            if (current != null && current.getId() == selected.getId()) {
                setMsgErr("Tidak bisa menghapus akun yang sedang login.");
                DialogUtil.showWarning("Aksi ditolak", "Tidak bisa menghapus akun yang sedang login.");
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
            DialogUtil.showSuccess("Sukses", "User berhasil dihapus.");
            refreshUsers(null, false);
        } catch (Exception ex) {
            setMsgErr("Gagal hapus: " + ex.getMessage());
            DialogUtil.showError("Gagal hapus user", ex);
        }
    }

    // ==========================
    // LOANS: REFRESH
    // ==========================
    @FXML
    public void handleRefreshLoans() {
        try {
            clearLoanMsg();
            List<Loan> loans = service.getActiveLoans();
            if (loanTable != null) loanTable.getItems().setAll(loans);

            if (loans == null || loans.isEmpty()) {
                setLoanMsgOk("Tidak ada peminjaman aktif.");
            } else {
                setLoanMsgOk("Memuat " + loans.size() + " peminjaman aktif.");
            }
        } catch (Exception ex) {
            setLoanMsgErr("Gagal load loans: " + ex.getMessage());
            DialogUtil.showError("Gagal load peminjaman", ex);
        }
    }

    private void handleRefreshLoansSilently() {
        try {
            if (loanTable != null) loanTable.getItems().setAll(service.getActiveLoans());
        } catch (Exception ignored) {}
    }

    // ==========================
    // ✅ LOANS: EDIT TANGGAL (UPDATE KE DB)
    // ==========================
    @FXML
    public void handleEditLoanDates() {
        Loan selected = loanTable == null ? null : loanTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setLoanMsgErr("Pilih peminjaman dulu.");
            DialogUtil.showInfo("Belum dipilih", "Pilih data peminjaman yang ingin diubah tanggalnya.");
            return;
        }

        if (selected.getReturnDate() != null) {
            setLoanMsgErr("Tidak bisa edit tanggal: sudah dikembalikan.");
            DialogUtil.showWarning("Tidak bisa", "Pinjaman yang sudah dikembalikan tidak dapat diedit.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Tanggal Peminjaman (Admin)");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        DatePicker dpLoan = new DatePicker(selected.getLoanDate() != null ? selected.getLoanDate() : LocalDate.now());
        DatePicker dpDue  = new DatePicker(selected.getDueDate() != null ? selected.getDueDate() : LocalDate.now().plusDays(7));

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(12));
        gp.addRow(0, new Label("Tanggal Pinjam"), dpLoan);
        gp.addRow(1, new Label("Jatuh Tempo"), dpDue);

        dialog.getDialogPane().setContent(gp);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        LocalDate newLoan = dpLoan.getValue();
        LocalDate newDue  = dpDue.getValue();

        if (newLoan == null || newDue == null) {
            DialogUtil.showWarning("Input kosong", "Tanggal pinjam dan jatuh tempo wajib diisi.");
            return;
        }
        if (newDue.isBefore(newLoan)) {
            DialogUtil.showWarning("Input salah", "Jatuh tempo tidak boleh sebelum tanggal pinjam.");
            return;
        }

        try {
            service.updateLoanDates(selected.getId(), newLoan, newDue);

            // update object lokal biar table langsung refresh
            selected.setLoanDate(newLoan);
            selected.setDueDate(newDue);
            loanTable.refresh();

            setLoanMsgOk("Tanggal berhasil diupdate (Loan ID: " + selected.getId() + ")");
            DialogUtil.showSuccess("Berhasil", "Tanggal pinjam & jatuh tempo berhasil diperbarui.");
        } catch (Exception ex) {
            setLoanMsgErr("Gagal edit tanggal: " + ex.getMessage());
            DialogUtil.showError("Gagal edit tanggal", ex);
        }
    }

    // ==========================
    // LOANS: RETURN (ADMIN BISA)
    // ==========================
    @FXML
    public void handleReturnSelected() {
        Loan selected = loanTable == null ? null : loanTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setLoanMsgErr("Pilih peminjaman dulu.");
            DialogUtil.showInfo("Belum dipilih", "Pilih data peminjaman yang ingin dikembalikan.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Pengembalian");
        confirm.setHeaderText("Kembalikan buku?");
        confirm.setContentText("Loan ID: " + selected.getId() + " | Buku: " + n(selected.getBookTitle()));

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            service.returnBook(selected.getId(), 1000);
            DialogUtil.showSuccess("Berhasil", "Buku berhasil dikembalikan.");
            handleRefreshLoans();
        } catch (Exception ex) {
            setLoanMsgErr("Gagal kembalikan: " + ex.getMessage());
            DialogUtil.showError("Gagal pengembalian", ex);
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
            DialogUtil.showError("Logout gagal", ex);
        }
    }

    // ==========================
    // DIALOG USER
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
            u.setPasswordHash(n(password.getText())); // boleh kosong saat edit
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
    private void clearMsg() { if (msgLabel != null) msgLabel.setText(""); }
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

    private void clearLoanMsg() { if (loanMsgLabel != null) loanMsgLabel.setText(""); }
    private void setLoanMsgOk(String msg) {
        if (loanMsgLabel == null) return;
        loanMsgLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: 800;");
        loanMsgLabel.setText(msg);
    }
    private void setLoanMsgErr(String msg) {
        if (loanMsgLabel == null) return;
        loanMsgLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 800;");
        loanMsgLabel.setText(msg);
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private String n(String s) { return s == null ? "" : s; }
}
