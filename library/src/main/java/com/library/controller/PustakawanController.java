package com.library.controller;

import java.util.Optional;

import com.library.Session;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.service.LibraryService;

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
        colLoanDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getLoanDate())));
        colDueDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getDueDate())));
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(n(c.getValue().getStatus())));
        colFine.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getFine()));
        loanTable.setItems(loanData);

        // default view: CRUD
        goCrud(null);

        handleRefresh();
    }

    // ==========================
    // NAV VIEW (SIDEBAR)
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
        crudPane.setVisible(true);
        crudPane.setManaged(true);
        returnPane.setVisible(false);
        returnPane.setManaged(false);
    }

    private void showReturnView() {
        crudPane.setVisible(false);
        crudPane.setManaged(false);
        returnPane.setVisible(true);
        returnPane.setManaged(true);
    }

    private void setActiveMenu(Button active) {
        if (btnMenuCrud != null) btnMenuCrud.getStyleClass().remove("sidebar-btn-active");
        if (btnMenuReturn != null) btnMenuReturn.getStyleClass().remove("sidebar-btn-active");
        if (active != null && !active.getStyleClass().contains("sidebar-btn-active")) {
            active.getStyleClass().add("sidebar-btn-active");
        }
    }

    // ==========================
    // TOP ACTIONS
    // ==========================
    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            Session.clear();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/library/view/Login.fxml"));
            stage.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
            com.library.util.DialogUtil.showError("Logout gagal", ex.getMessage());
        }
    }

    @FXML
    public void handleRefresh() {
        if (bookMsgLabel != null) bookMsgLabel.setText("");
        if (loanMsgLabel != null) loanMsgLabel.setText("");

        // reset search biar jelas
        if (searchBookField != null) searchBookField.setText("");

        refreshBooks(null);
        refreshLoans();
    }

    // ==========================
    // ✅ SEARCH BUKU (FIX)
    // ==========================
    @FXML
    public void handleSearchBook() {
        try {
            String keyword = (searchBookField == null || searchBookField.getText() == null)
                    ? ""
                    : searchBookField.getText().trim();

            if (keyword.isBlank()) {
                bookMsgLabel.setText("Menampilkan semua buku.");
                refreshBooks(null);
                return;
            }

            refreshBooks(keyword);
            bookMsgLabel.setText("Hasil pencarian untuk: " + keyword);

        } catch (Exception ex) {
            showError("Cari buku gagal", ex.getMessage());
        }
    }

    private void refreshBooks(String keyword) {
        try {
            if (keyword == null || keyword.isBlank()) {
                bookData.setAll(service.getAllBooks());
            } else {
                bookData.setAll(service.searchBooksByTitle(keyword));
            }
        } catch (Exception ex) {
            showError("Gagal load data buku", ex.getMessage());
        }
    }

    private void refreshLoans() {
        try {
            loanData.setAll(service.getActiveLoans());
        } catch (Exception ex) {
            showError("Gagal load data peminjaman", ex.getMessage());
        }
    }

    // ==========================
    // CRUD ACTIONS
    // ==========================
    @FXML
    public void handleAddBook() {
        Book b = showBookDialog(null);
        if (b == null) return;

        try {
            service.addBook(b);
            bookMsgLabel.setText("Berhasil tambah buku.");
            refreshBooks(null);
        } catch (Exception ex) {
            bookMsgLabel.setText("Gagal tambah: " + ex.getMessage());
        }
    }

    @FXML
    public void handleEditBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) { bookMsgLabel.setText("Pilih buku dulu."); return; }

        Book edited = showBookDialog(selected);
        if (edited == null) return;

        try {
            service.updateBook(edited);
            bookMsgLabel.setText("Berhasil edit buku.");
            refreshBooks(null);
        } catch (Exception ex) {
            bookMsgLabel.setText("Gagal edit: " + ex.getMessage());
        }
    }

    @FXML
    public void handleDeleteBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) { bookMsgLabel.setText("Pilih buku dulu."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi");
        confirm.setHeaderText("Hapus buku?");
        confirm.setContentText("Judul: " + selected.getTitle());

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            service.deleteBook(selected.getId());
            bookMsgLabel.setText("Berhasil hapus buku.");
            refreshBooks(null);
        } catch (Exception ex) {
            bookMsgLabel.setText("Gagal hapus: " + ex.getMessage());
        }
    }

    // ==========================
    // RETURN ACTION
    // ==========================
    @FXML
    public void handleReturnSelected() {
        Loan selected = loanTable.getSelectionModel().getSelectedItem();
        if (selected == null) { loanMsgLabel.setText("Pilih data peminjaman dulu."); return; }

        try {
            service.returnBook(selected.getId(), 1000);
            loanMsgLabel.setText("Pengembalian sukses. Cek status & denda.");
            refreshLoans();
            refreshBooks(null);
        } catch (Exception ex) {
            loanMsgLabel.setText("Gagal: " + ex.getMessage());
            com.library.util.DialogUtil.showError("Gagal pengembalian", ex);
        }
    }

    // ===== dialog tambah/edit =====
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
                showError("Input salah", "Judul dan Penulis wajib diisi.");
                return null;
            }

            int st;
            try {
                st = Integer.parseInt(s.trim());
            } catch (Exception ex) {
                showError("Input salah", "Stok harus angka.");
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

    private String n(String s) { return s == null ? "" : s; }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
