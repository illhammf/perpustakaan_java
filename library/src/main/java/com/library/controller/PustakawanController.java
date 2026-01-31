package com.library.controller;

import java.time.LocalDate;
import java.util.Optional;

import com.library.Session;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.service.LibraryService;
import com.library.util.DialogUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PustakawanController {

    private final LibraryService service = new LibraryService();

    // ===== SIDEBAR / HEADER =====
    @FXML private Label pageTitleLabel;
    @FXML private Label loginInfoLabel;
    @FXML private Button btnMenuCrud;
    @FXML private Button btnMenuReturn;

    // ===== VIEW PANES =====
    @FXML private VBox crudPane;
    @FXML private VBox returnPane;

    // ===== CRUD BUKU =====
    @FXML private TextField searchBookField;
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, Integer> colId;
    @FXML private TableColumn<Book, String> colTitle;
    @FXML private TableColumn<Book, String> colAuthor;
    @FXML private TableColumn<Book, Integer> colStock;
    @FXML private Label bookMsgLabel;

    private final ObservableList<Book> bookData = FXCollections.observableArrayList();

    // ===== PENGEMBALIAN =====
    @FXML private TableView<Loan> loanTable;
    @FXML private TableColumn<Loan, Integer> colLoanId;
    @FXML private TableColumn<Loan, String> colLoanBook;
    @FXML private TableColumn<Loan, String> colLoanDate;
    @FXML private TableColumn<Loan, String> colDueDate;
    @FXML private TableColumn<Loan, String> colStatus;
    @FXML private TableColumn<Loan, Integer> colFine;
    @FXML private Label loanMsgLabel;

    private final ObservableList<Loan> loanData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (loginInfoLabel != null) loginInfoLabel.setText("Login sebagai: Pustakawan");

        // ===== Table Buku =====
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colTitle.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getTitle())));
        colAuthor.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getAuthor())));
        colStock.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getStock()));
        bookTable.setItems(bookData);

        // ===== Table Loan =====
        colLoanId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colLoanBook.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getBookTitle())));
        colLoanDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getLoanDate() == null ? "-" : c.getValue().getLoanDate().toString()
        ));
        colDueDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDueDate() == null ? "-" : c.getValue().getDueDate().toString()
        ));
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getStatus())));
        colFine.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getFine()));
        loanTable.setItems(loanData);

        if (searchBookField != null) searchBookField.setOnAction(e -> handleSearchBook());

        goCrud(null);
        handleRefresh(false);
    }

    // ==========================
    // NAV VIEW
    // ==========================
    @FXML
    public void goCrud(ActionEvent e) {
        if (pageTitleLabel != null) pageTitleLabel.setText("Manajemen Buku");
        showCrudView();
        setActiveMenu(btnMenuCrud);
    }

    @FXML
    public void goReturn(ActionEvent e) {
        if (pageTitleLabel != null) pageTitleLabel.setText("Pengembalian Buku");
        showReturnView();
        setActiveMenu(btnMenuReturn);
    }

    private void showCrudView() {
        if (crudPane != null) { crudPane.setVisible(true); crudPane.setManaged(true); }
        if (returnPane != null) { returnPane.setVisible(false); returnPane.setManaged(false); }
    }

    private void showReturnView() {
        if (crudPane != null) { crudPane.setVisible(false); crudPane.setManaged(false); }
        if (returnPane != null) { returnPane.setVisible(true); returnPane.setManaged(true); }
    }

    private void setActiveMenu(Button active) {
        if (btnMenuCrud != null) btnMenuCrud.getStyleClass().remove("sidebar-btn-active");
        if (btnMenuReturn != null) btnMenuReturn.getStyleClass().remove("sidebar-btn-active");
        if (active != null && !active.getStyleClass().contains("sidebar-btn-active")) {
            active.getStyleClass().add("sidebar-btn-active");
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
            DialogUtil.showError("Logout gagal", ex);
        }
    }

    // ==========================
    // REFRESH
    // ==========================
    @FXML
    public void handleRefresh() {
        handleRefresh(true);
    }

    private void handleRefresh(boolean showDialog) {
        clearBookMsg();
        clearLoanMsg();

        if (searchBookField != null) searchBookField.setText("");

        refreshBooks(null);
        refreshLoans();

        if (showDialog) DialogUtil.showSuccess("Berhasil", "Data pustakawan berhasil diperbarui.");
    }

    // ==========================
    // SEARCH BUKU
    // ==========================
    @FXML
    public void handleSearchBook() {
        try {
            String keyword = (searchBookField == null || searchBookField.getText() == null)
                    ? ""
                    : searchBookField.getText().trim();

            if (keyword.isBlank()) {
                refreshBooks(null);
                setBookMsgOk("Menampilkan semua buku.");
                DialogUtil.showInfo("Kata kunci kosong", "Menampilkan semua buku karena kata kunci belum diisi.");
                return;
            }

            refreshBooks(keyword);

            if (bookData.isEmpty()) {
                setBookMsgErr("Tidak ada buku untuk kata kunci: " + keyword);
                DialogUtil.showInfo("Tidak ditemukan", "Tidak ada buku yang cocok dengan: " + keyword);
            } else {
                setBookMsgOk("Ditemukan " + bookData.size() + " buku untuk: " + keyword);
            }

        } catch (Exception ex) {
            DialogUtil.showError("Cari buku gagal", ex);
        }
    }

    private void refreshBooks(String keyword) {
        try {
            if (keyword == null || keyword.isBlank()) bookData.setAll(service.getAllBooks());
            else bookData.setAll(service.searchBooksByTitle(keyword));
        } catch (Exception ex) {
            DialogUtil.showError("Gagal load data buku", ex);
        }
    }

    private void refreshLoans() {
        try {
            loanData.setAll(service.getActiveLoans());
        } catch (Exception ex) {
            DialogUtil.showError("Gagal load data peminjaman", ex);
        }
    }

    // ==========================
    // CRUD BUKU
    // ==========================
    @FXML
    public void handleAddBook() {
        Book b = showBookDialog(null);
        if (b == null) return;

        try {
            service.addBook(b);
            setBookMsgOk("Berhasil tambah buku.");
            DialogUtil.showSuccess("Sukses", "Buku berhasil ditambahkan.");
            refreshBooks(null);
        } catch (Exception ex) {
            setBookMsgErr("Gagal tambah: " + ex.getMessage());
            DialogUtil.showError("Gagal tambah buku", ex);
        }
    }

    @FXML
    public void handleEditBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setBookMsgErr("Pilih buku dulu.");
            DialogUtil.showInfo("Belum dipilih", "Pilih buku yang ingin diedit terlebih dahulu.");
            return;
        }

        Book edited = showBookDialog(selected);
        if (edited == null) return;

        try {
            service.updateBook(edited);
            setBookMsgOk("Berhasil edit buku.");
            DialogUtil.showSuccess("Sukses", "Data buku berhasil diperbarui.");
            refreshBooks(null);
        } catch (Exception ex) {
            setBookMsgErr("Gagal edit: " + ex.getMessage());
            DialogUtil.showError("Gagal edit buku", ex);
        }
    }

    @FXML
    public void handleDeleteBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setBookMsgErr("Pilih buku dulu.");
            DialogUtil.showInfo("Belum dipilih", "Pilih buku yang ingin dihapus terlebih dahulu.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi");
        confirm.setHeaderText("Hapus buku?");
        confirm.setContentText("Judul: " + selected.getTitle());

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            service.deleteBook(selected.getId());
            setBookMsgOk("Berhasil hapus buku.");
            DialogUtil.showSuccess("Sukses", "Buku berhasil dihapus.");
            refreshBooks(null);
        } catch (Exception ex) {
            setBookMsgErr("Gagal hapus: " + ex.getMessage());
            DialogUtil.showError("Gagal hapus buku", ex);
        }
    }

    // ==========================
    // ✅ EDIT TANGGAL PINJAM & DUE DATE (UPDATE KE DB)
    // ==========================
    @FXML
    public void handleEditLoanDates() {
        Loan selected = loanTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setLoanMsgErr("Pilih data peminjaman dulu.");
            DialogUtil.showInfo("Belum dipilih", "Pilih peminjaman yang ingin diubah tanggalnya.");
            return;
        }

        if (selected.getReturnDate() != null) {
            setLoanMsgErr("Tidak bisa edit tanggal: pinjaman sudah dikembalikan.");
            DialogUtil.showWarning("Tidak bisa", "Pinjaman yang sudah dikembalikan tidak dapat diedit.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Tanggal Peminjaman");
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

            setLoanMsgOk("Tanggal berhasil diperbarui (ID: " + selected.getId() + ")");
            DialogUtil.showSuccess("Berhasil", "Tanggal pinjam & jatuh tempo berhasil diperbarui.");

        } catch (Exception ex) {
            setLoanMsgErr("Gagal edit tanggal: " + ex.getMessage());
            DialogUtil.showError("Gagal edit tanggal", ex);
        }
    }

    // ==========================
    // RETURN
    // ==========================
    @FXML
    public void handleReturnSelected() {
        Loan selected = loanTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setLoanMsgErr("Pilih data peminjaman dulu.");
            DialogUtil.showInfo("Belum dipilih", "Pilih data peminjaman yang ingin dikembalikan.");
            return;
        }

        try {
            service.returnBook(selected.getId(), 1000);
            setLoanMsgOk("Pengembalian sukses. Status & denda terupdate.");
            DialogUtil.showSuccess("Berhasil", "Buku berhasil dikembalikan.");
            refreshLoans();
            refreshBooks(null);
        } catch (Exception ex) {
            setLoanMsgErr("Gagal: " + ex.getMessage());
            DialogUtil.showError("Gagal pengembalian", ex);
        }
    }

    // ===== dialog tambah/edit buku =====
    private Book showBookDialog(Book existing) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Tambah Buku" : "Edit Buku");

        ButtonType saveBtn = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField title = new TextField(existing == null ? "" : n(existing.getTitle()));
        TextField author = new TextField(existing == null ? "" : n(existing.getAuthor()));
        TextField stock = new TextField(existing == null ? "0" : String.valueOf(existing.getStock()));
        TextField coverPath = new TextField(existing == null ? "" : n(existing.getCoverPath()));
        TextArea description = new TextArea(existing == null ? "" : n(existing.getDescription()));
        description.setPrefRowCount(4);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(12));

        gp.addRow(0, new Label("Judul*"), title);
        gp.addRow(1, new Label("Penulis*"), author);
        gp.addRow(2, new Label("Stok*"), stock);
        gp.addRow(3, new Label("Cover Path"), coverPath);
        gp.addRow(4, new Label("Deskripsi"), description);

        dialog.getDialogPane().setContent(gp);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            String t = title.getText();
            String a = author.getText();
            String s = stock.getText();

            if (t == null || t.isBlank() || a == null || a.isBlank()) {
                DialogUtil.showWarning("Input kurang", "Judul dan Penulis wajib diisi.");
                return null;
            }

            int st;
            try {
                st = Integer.parseInt(s.trim());
            } catch (Exception ex) {
                DialogUtil.showWarning("Input salah", "Stok harus berupa angka.");
                return null;
            }

            if (st < 0) {
                DialogUtil.showWarning("Input salah", "Stok tidak boleh negatif.");
                return null;
            }

            Book b = new Book();
            if (existing != null) b.setId(existing.getId());
            b.setTitle(t.trim());
            b.setAuthor(a.trim());
            b.setStock(st);
            b.setCoverPath(coverPath.getText() == null ? null : coverPath.getText().trim());
            b.setDescription(description.getText() == null ? null : description.getText().trim());
            return b;
        });

        Optional<Book> res = dialog.showAndWait();
        return res.orElse(null);
    }

    // ==========================
    // UI HELPERS
    // ==========================
    private void clearBookMsg() { if (bookMsgLabel != null) bookMsgLabel.setText(""); }
    private void clearLoanMsg() { if (loanMsgLabel != null) loanMsgLabel.setText(""); }

    private void setBookMsgOk(String msg) {
        if (bookMsgLabel == null) return;
        bookMsgLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: 800;");
        bookMsgLabel.setText(msg);
    }

    private void setBookMsgErr(String msg) {
        if (bookMsgLabel == null) return;
        bookMsgLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 800;");
        bookMsgLabel.setText(msg);
    }

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

    private String n(String s) { return s == null ? "" : s; }
}
