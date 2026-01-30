package com.library.controller;

import com.library.Session;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.User;
import com.library.service.LibraryService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class MahasiswaController {

    private final LibraryService service = new LibraryService();

    // Tab 1
    @FXML private TextField searchField;
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, Integer> colId;
    @FXML private TableColumn<Book, String> colTitle;
    @FXML private TableColumn<Book, String> colAuthor;
    @FXML private TableColumn<Book, Integer> colStock;
    @FXML private Label msgLabel;

    // Tab 2
    @FXML private TableView<Loan> loanTable;
    @FXML private TableColumn<Loan, Integer> colLoanId;
    @FXML private TableColumn<Loan, String> colLoanBook;
    @FXML private TableColumn<Loan, String> colLoanDate;
    @FXML private TableColumn<Loan, String> colDueDate;
    @FXML private TableColumn<Loan, String> colStatus;
    @FXML private TableColumn<Loan, Integer> colFine;

    private final ObservableList<Book> bookData = FXCollections.observableArrayList();
    private final ObservableList<Loan> loanData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Kalau fx:id tidak match, ketahuan di sini (bukan LoadException samar)
        if (searchField == null || bookTable == null || loanTable == null || msgLabel == null) {
            throw new IllegalStateException("FXML injection gagal. Cek fx:id di Mahasiswa.fxml harus sama dengan field di MahasiswaController.");
        }

        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colTitle.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTitle()));
        colAuthor.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAuthor()));
        colStock.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getStock()));
        bookTable.setItems(bookData);

        colLoanId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colLoanBook.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getBookTitle()));
        colLoanDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getLoanDate())));
        colDueDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getDueDate())));
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
        colFine.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getFine()));
        loanTable.setItems(loanData);

        // biar aman setelah scene ready
        Platform.runLater(this::handleRefresh);
    }

    @FXML
    public void handleRefresh() {
        try {
            msgLabel.setText("");

            // validasi session
            User u = Session.getCurrentUser();
            if (u == null) {
                msgLabel.setText("Session login tidak ditemukan. Silakan login ulang.");
                return;
            }

            bookData.setAll(service.getBooks(null));
            loanData.setAll(service.getLoansByUser(u.getId()));
        } catch (Exception ex) {
            msgLabel.setText("Refresh gagal: " + ex.getMessage());
        }
    }

    @FXML
    public void handleSearch() {
        try {
            msgLabel.setText("");
            bookData.setAll(service.getBooks(searchField.getText()));
        } catch (Exception ex) {
            msgLabel.setText("Cari gagal: " + ex.getMessage());
        }
    }

    @FXML
    public void handleBorrowSelected() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            msgLabel.setText("Pilih buku dulu.");
            return;
        }

        User u = Session.getCurrentUser();
        if (u == null) {
            msgLabel.setText("Session login tidak ditemukan. Silakan login ulang.");
            return;
        }

        try {
            service.borrowBook(u.getId(), selected.getId());
            msgLabel.setText("Berhasil pinjam! Due date otomatis 7 hari.");
            handleRefresh();
        } catch (Exception ex) {
            msgLabel.setText(ex.getMessage());
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
}
