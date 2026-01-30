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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class PustakawanController {

    private final LibraryService service = new LibraryService();

    // Tab Buku
    @FXML private TextField searchBookField;
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, Integer> colId;
    @FXML private TableColumn<Book, String> colTitle;
    @FXML private TableColumn<Book, String> colAuthor;
    @FXML private TableColumn<Book, String> colCategory;
    @FXML private TableColumn<Book, Integer> colStock;
    @FXML private Label bookMsgLabel;

    private final ObservableList<Book> bookData = FXCollections.observableArrayList();

    // Tab Pengembalian
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
        // Buku
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colTitle.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTitle()));
        colAuthor.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAuthor()));
        colCategory.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getCategory() == null ? "" : c.getValue().getCategory()));
        colStock.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getStock()));
        bookTable.setItems(bookData);

        // Loan
        colLoanId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colLoanBook.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getBookTitle()));
        colLoanDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getLoanDate())));
        colDueDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getDueDate())));
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
        colFine.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getFine()));
        loanTable.setItems(loanData);

        handleRefresh();
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

    @FXML
    public void handleRefresh() {
        bookMsgLabel.setText("");
        loanMsgLabel.setText("");
        refreshBooks(null);
        refreshLoans();
    }

    @FXML
    public void handleSearchBook() {
        refreshBooks(searchBookField.getText());
    }

    private void refreshBooks(String keyword) {
        try {
            bookData.setAll(service.getBooks(keyword));
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

    @FXML
    public void handleAddBook() {
        Book b = showBookDialog(null);
        if (b == null) return;

        try {
            service.addBook(b);
            bookMsgLabel.setText("Berhasil tambah buku.");
            refreshBooks(null);
        } catch (Exception ex) {
            bookMsgLabel.setText(ex.getMessage());
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
            bookMsgLabel.setText(ex.getMessage());
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

    // Pengembalian: pilih loan aktif -> return -> status+fine otomatis
    @FXML
    public void handleReturnSelected() {
        Loan selected = loanTable.getSelectionModel().getSelectedItem();
        if (selected == null) { loanMsgLabel.setText("Pilih data peminjaman dulu."); return; }

        try {
            service.returnBook(selected.getId());
            loanMsgLabel.setText("Pengembalian sukses. Cek status & fine.");
            refreshLoans();
            refreshBooks(null);
        } catch (Exception ex) {
            loanMsgLabel.setText(ex.getMessage());
        }
    }

    private Book showBookDialog(Book existing) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Tambah Buku" : "Edit Buku");

        ButtonType saveBtn = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField isbn = new TextField(existing == null ? "" : n(existing.getIsbn()));
        TextField title = new TextField(existing == null ? "" : n(existing.getTitle()));
        TextField author = new TextField(existing == null ? "" : n(existing.getAuthor()));
        TextField publisher = new TextField(existing == null ? "" : n(existing.getPublisher()));
        TextField year = new TextField(existing == null || existing.getPublishYear() == null ? "" : String.valueOf(existing.getPublishYear()));
        TextField category = new TextField(existing == null ? "" : n(existing.getCategory()));
        TextField stock = new TextField(existing == null ? "0" : String.valueOf(existing.getStock()));

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.addRow(0, new Label("ISBN"), isbn);
        gp.addRow(1, new Label("Judul*"), title);
        gp.addRow(2, new Label("Penulis*"), author);
        gp.addRow(3, new Label("Penerbit"), publisher);
        gp.addRow(4, new Label("Tahun"), year);
        gp.addRow(5, new Label("Kategori"), category);
        gp.addRow(6, new Label("Stok"), stock);

        dialog.getDialogPane().setContent(gp);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            Book b = new Book();
            if (existing != null) b.setId(existing.getId());
            b.setIsbn(isbn.getText());
            b.setTitle(title.getText());
            b.setAuthor(author.getText());
            b.setPublisher(publisher.getText());
            b.setCategory(category.getText());

            String y = year.getText();
            b.setPublishYear(y == null || y.isBlank() ? null : Integer.parseInt(y.trim()));

            b.setStock(Integer.parseInt(stock.getText().trim()));
            return b;
        });

        try {
            Optional<Book> res = dialog.showAndWait();
            return res.orElse(null);
        } catch (NumberFormatException ex) {
            showError("Input salah", "Tahun/Stok harus angka.");
            return null;
        }
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
