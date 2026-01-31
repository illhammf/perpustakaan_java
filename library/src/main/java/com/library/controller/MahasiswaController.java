package com.library.controller;

import com.library.Session;
import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MahasiswaController {

    // TOP
    @FXML private TextField searchField;

    // CONTENT
    @FXML private Label pageTitleLabel;
    @FXML private ScrollPane bookScroll;
    @FXML private TilePane tileBooks;

    // LOGIN INFO
    @FXML private Label loginInfoLabel;
    @FXML private Label overdueLabel;

    // PINJAMAN VIEW
    @FXML private VBox loanPane;
    @FXML private TableView<Loan> loanTable;
    @FXML private TableColumn<Loan, Integer> colLoanId;
    @FXML private TableColumn<Loan, String> colBookTitle;
    @FXML private TableColumn<Loan, String> colLoanDate;
    @FXML private TableColumn<Loan, String> colDueDate;
    @FXML private TableColumn<Loan, String> colStatus;
    @FXML private TableColumn<Loan, Integer> colFine;

    private final BookDao bookDao = new BookDao();
    private final LoanDao loanDao = new LoanDao();

    @FXML private Button btnReturn;

    private List<Book> allBooks = new ArrayList<>();

    @FXML
    public void initialize() {
        try {
            // tampilkan info login (kalau bisa ambil dari Session)
            loginInfoLabel.setText(buildLoginInfoText());

            setupLoanTable();

            // disable return button when no selection or selection not returnable
            btnReturn.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                Loan s = loanTable.getSelectionModel().getSelectedItem();
                if (s == null) return true;
                String st = s.getStatus();
                if (st == null) return true;
                st = st.toUpperCase();
                return !(st.equals("DIPINJAM") || st.equals("TERLAMBAT"));
            }, loanTable.getSelectionModel().selectedItemProperty()));

            // default: Home
            goHome(null);

            // load data
            loadBooksToTiles();
            loadLoans();

        } catch (Exception ex) {
            handleException("Gagal memuat halaman Mahasiswa", ex, true);
        }
    }

    private String buildLoginInfoText() {
        // default aman
        String base = "Login sebagai: (Mahasiswa)";

        try {
            User u = Session.getCurrentUser();
            if (u != null) {
                String name = u.getFullName() == null ? "" : u.getFullName();
                String role = u.getRoleName() == null ? "" : u.getRoleName();
                if (!name.isBlank() && !role.isBlank()) return "Login sebagai: " + name + " (" + role + ")";
                if (!name.isBlank()) return "Login sebagai: " + name;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return base;
    }

    private void setupLoanTable() {
        colLoanId.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getId()));
        colBookTitle.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(n(data.getValue().getBookTitle())));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        colLoanDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getLoanDate() == null ? "-" : data.getValue().getLoanDate().format(fmt)
        ));
        colDueDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDueDate() == null ? "-" : data.getValue().getDueDate().format(fmt)
        ));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(n(data.getValue().getStatus())));
        colFine.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getFine()));
    }

    // ==========================
    // NAV (SIDEBAR)
    // ==========================
    @FXML public void goHome(ActionEvent e) {
        pageTitleLabel.setText("Rekomendasi / Koleksi Buku");
        showBooksView();
        renderTiles(allBooks);
    }

    @FXML public void goCari(ActionEvent e) {
        pageTitleLabel.setText("Cari & Pinjam Buku");
        showBooksView();

        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (q.isBlank()) renderTiles(allBooks);
        else renderTiles(filterBooks(q));
    }

    @FXML public void goPinjaman(ActionEvent e) {
        pageTitleLabel.setText("Pinjaman Saya");
        showLoansView();
        loadLoans();
    }

    private void showBooksView() {
        bookScroll.setVisible(true);
        bookScroll.setManaged(true);

        loanPane.setVisible(false);
        loanPane.setManaged(false);
    }

    private void showLoansView() {
        bookScroll.setVisible(false);
        bookScroll.setManaged(false);

        loanPane.setVisible(true);
        loanPane.setManaged(true);
    }

    // ==========================
    // LOAD BUKU
    // ==========================
    private void loadBooksToTiles() {
        try {
            allBooks = bookDao.findAll();
            if (allBooks == null) allBooks = new ArrayList<>();
            renderTiles(allBooks);

            if (allBooks.isEmpty()) {
                showInfo("Info", "Belum ada buku di database.");
            }
        } catch (Exception ex) {
            handleException("Gagal memuat data buku", ex, false);
        }
    }

    private void renderTiles(List<Book> books) {
        tileBooks.getChildren().clear();
        if (books == null) return;

        for (Book b : books) {
            if (b == null) continue;
            tileBooks.getChildren().add(createBookCard(b));
        }
    }

    private VBox createBookCard(Book b) {
        VBox card = new VBox(10);
        card.getStyleClass().add("book-card");
        card.setPrefWidth(260);
        card.setMinHeight(360);

        StackPane coverBox = new StackPane();
        coverBox.getStyleClass().add("book-cover");
        coverBox.setPrefSize(232, 150);
        coverBox.setMinSize(232, 150);
        coverBox.setMaxSize(232, 150);

        ImageView iv = new ImageView();
        iv.setFitWidth(232);
        iv.setFitHeight(150);
        iv.setPreserveRatio(false);

        boolean loaded = tryLoadCoverById(iv, b.getId());
        if (loaded) {
            coverBox.getChildren().add(iv);
        } else {
            Label no = new Label("No Cover");
            no.getStyleClass().add("no-cover");
            coverBox.getChildren().add(no);
        }

        Label title = new Label(n(b.getTitle()));
        title.getStyleClass().add("book-title");
        title.setWrapText(true);

        Label meta = new Label((b.getAuthor() == null ? "-" : b.getAuthor()) + " • Stok: " + b.getStock());
        meta.getStyleClass().add("book-meta");
        meta.setWrapText(true);

        Button btnBorrow = new Button("Pinjam");
        btnBorrow.getStyleClass().add("btn-primary");
        btnBorrow.setDisable(b.getStock() <= 0);

        btnBorrow.setOnAction(e -> {
            if (b.getStock() <= 0) {
                showInfo("Info", "Stok buku habis. Tidak bisa dipinjam.");
                return;
            }
            confirmBorrow(b);
        });

        card.getChildren().addAll(coverBox, title, meta, btnBorrow);

        // klik card -> detail
        card.setOnMouseClicked(e -> showBookDetail(b));

        return card;
    }

    private boolean tryLoadCoverById(ImageView iv, int bookId) {
        String[] exts = {"jpg", "jpeg", "png"};
        for (String ext : exts) {
            String path = "/com/library/assets/covers/book_" + bookId + "." + ext;
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is == null) continue;
                iv.setImage(new Image(is));
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

private void showBookDetail(Book b) {
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle("Detail Buku");
    dialog.initModality(Modality.APPLICATION_MODAL);

    ButtonType closeBtn = new ButtonType("Tutup", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().add(closeBtn);

    VBox root = new VBox(12);
    root.setStyle("-fx-padding:16; -fx-background-color:white;");

    Label t = new Label(n(b.getTitle()));
    t.setStyle("-fx-font-size:18px; -fx-font-weight:900; -fx-text-fill:#0f172a;");

    Label a = new Label("Penulis: " + nDash(b.getAuthor()));
    a.setStyle("-fx-text-fill:#334155; -fx-font-weight:700;");

    Label s = new Label("Stok: " + b.getStock());
    s.setStyle("-fx-text-fill:#64748b; -fx-font-weight:800;");

    // ✅ ambil deskripsi dari Book
    String desc = getBookDescriptionSafe(b);
    Label descLabel = new Label(desc.isBlank() ? "Deskripsi belum tersedia." : desc);
    descLabel.setStyle("-fx-text-fill:#334155;");
    descLabel.setWrapText(true);

    // ✅ extra info lain (isbn/publisher/category/year) kalau ada
    String extra = buildExtraInfoSafe(b);
    Label extraLabel = new Label(extra.isBlank() ? "" : extra);
    extraLabel.setStyle("-fx-text-fill:#64748b;");
    extraLabel.setWrapText(true);

    Button borrow = new Button("Pinjam Buku");
    borrow.getStyleClass().add("btn-primary");
    borrow.setDisable(b.getStock() <= 0);
    borrow.setOnAction(e -> {
        dialog.close();
        confirmBorrow(b);
    });

    root.getChildren().addAll(t, a, s, new Separator(), new Label("Deskripsi:"), descLabel);

    if (!extra.isBlank()) {
        root.getChildren().addAll(new Separator(), new Label("Info tambahan:"), extraLabel);
    }

    root.getChildren().add(borrow);

    dialog.getDialogPane().setContent(root);
    dialog.showAndWait();
}

    // ✅ prioritas: getDescription()
    private String getBookDescriptionSafe(Book b) {
        try {
            Object d = b.getClass().getMethod("getDescription").invoke(b);
            return d == null ? "" : d.toString().trim();
        } catch (Exception ex) {
            // reflection failures are non-fatal; print for debugging
            ex.printStackTrace();
        }
        return "";
    }

    private String buildExtraInfoSafe(Book b) {
        StringBuilder sb = new StringBuilder();

        try {
            Object isbn = b.getClass().getMethod("getIsbn").invoke(b);
            if (isbn != null && !isbn.toString().isBlank()) sb.append("ISBN: ").append(isbn).append("\n");
        } catch (Exception ex) { ex.printStackTrace(); }

        try {
            Object pub = b.getClass().getMethod("getPublisher").invoke(b);
            if (pub != null && !pub.toString().isBlank()) sb.append("Publisher: ").append(pub).append("\n");
        } catch (Exception ex) { ex.printStackTrace(); }

        try {
            Object cat = b.getClass().getMethod("getCategory").invoke(b);
            if (cat != null && !cat.toString().isBlank()) sb.append("Kategori: ").append(cat).append("\n");
        } catch (Exception ex) { ex.printStackTrace(); }

        try {
            Object year = b.getClass().getMethod("getPublishYear").invoke(b);
            if (year != null) sb.append("Tahun: ").append(year).append("\n");
        } catch (Exception ex) { ex.printStackTrace(); }

        return sb.toString().trim();
    }

    private String nDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }


    private void confirmBorrow(Book b) {
        if (b == null) {
            showError("Tidak bisa pinjam", "Data buku tidak valid (null).");
            return;
        }

        Integer userId = getSessionUserId();
        if (userId == null) {
            showError("Session tidak ditemukan", "Silakan login ulang.");
            goToLoginSafely();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi");
        confirm.setHeaderText("Pinjam buku: " + n(b.getTitle()));
        confirm.setContentText("Due date otomatis (7 hari). Lanjut?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            // kemungkinan error:
            // - stok habis (race condition)
            // - buku sudah dipinjam
            // - DB error
            loanDao.borrowBookTx(userId, b.getId(), 7);

            showInfo("Berhasil", "Buku berhasil dipinjam.\nCek menu Pinjaman Saya.");
            loadBooksToTiles();
            loadLoans();
            goPinjaman(null);

        } catch (Exception ex) {
            // kasih pesan ramah + tetap simpan detail untuk debug
            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();

            if (msg.contains("stock") || msg.contains("stok")) {
                showError("Gagal meminjam", "Stok buku sudah habis.");
            } else if (msg.contains("duplicate") || msg.contains("sudah") || msg.contains("already")) {
                showError("Gagal meminjam", "Buku ini sudah kamu pinjam / masih aktif.");
            } else {
                handleException("Gagal meminjam buku", ex, false);
            }
        }
    }

    @FXML
    public void handleReturnSelected(ActionEvent e) {
        Loan selected = loanTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Pilih pinjaman", "Pilih pinjaman yang ingin dikembalikan.");
            return;
        }

        // only allow return if this loan belongs to current user
        Integer userId = getSessionUserId();
        if (userId == null) {
            showError("Session tidak ditemukan", "Silakan login ulang.");
            goToLoginSafely();
            return;
        }
        if (selected.getUserId() != userId) {
            showError("Akses ditolak", "Anda hanya dapat mengembalikan pinjaman Anda sendiri.");
            return;
        }

        String status = selected.getStatus() == null ? "" : selected.getStatus().toUpperCase();
        if (!status.equals("DIPINJAM") && !status.equals("TERLAMBAT")) {
            showInfo("Tidak dapat dikembalikan", "Pinjaman ini tidak dalam status yang dapat dikembalikan.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Pengembalian");
        confirm.setHeaderText("Kembalikan buku: " + n(selected.getBookTitle()));
        confirm.setContentText("Apakah Anda yakin ingin mengembalikan buku ini? Jika terlambat, akan dikenakan denda.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            // fine per day: konfigurasi sederhana, 5000
            int finePerDay = 5000;
            loanDao.returnBookTx(selected.getId(), finePerDay);

            com.library.util.DialogUtil.showInfo("Berhasil", "Buku berhasil dikembalikan.");
            loadBooksToTiles();
            loadLoans();
            goPinjaman(null);

        } catch (Exception ex) {
            com.library.util.DialogUtil.showError("Gagal mengembalikan buku", ex);
        }
    }

    private Integer getSessionUserId() {
        try { return (Integer) Session.class.getMethod("getUserId").invoke(null); } catch (Exception ignored) {}
        try { return (Integer) Session.class.getField("userId").get(null); } catch (Exception ignored) {}
        try {
            Object u = Session.class.getMethod("getCurrentUser").invoke(null);
            if (u != null) return (Integer) u.getClass().getMethod("getId").invoke(u);
        } catch (Exception ignored) {}
        return null;
    }

    // ==========================
    // TOP SEARCH & REFRESH
    // ==========================
    @FXML
    public void handleSearchTop(ActionEvent e) {
        try {
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

            if (q.isBlank()) {
                renderTiles(allBooks);
                showInfo("Info", "Kata kunci kosong. Menampilkan semua buku.");
                return;
            }

            List<Book> filtered = filterBooks(q);
            renderTiles(filtered);

            if (filtered.isEmpty()) {
                showInfo("Info", "Buku tidak ditemukan untuk kata kunci: " + q);
            }

            goCari(null);

        } catch (Exception ex) {
            handleException("Gagal melakukan pencarian", ex, false);
        }
    }

    private List<Book> filterBooks(String q) {
        List<Book> filtered = new ArrayList<>();
        for (Book b : allBooks) {
            if (b == null) continue;
            String t = b.getTitle() == null ? "" : b.getTitle().toLowerCase();
            String a = b.getAuthor() == null ? "" : b.getAuthor().toLowerCase();
            if (t.contains(q) || a.contains(q)) filtered.add(b);
        }
        return filtered;
    }

    @FXML
    public void handleRefresh(ActionEvent e) {
        try {
            searchField.setText("");
            loadBooksToTiles();
            loadLoans();
            goHome(null);
            showInfo("Info", "Data berhasil diperbarui.");
        } catch (Exception ex) {
            handleException("Gagal refresh data", ex, false);
        }
    }

    // ==========================
    // LOAD LOANS
    // ==========================
    @FXML
    public void loadLoans() {
        try {
            Integer userId = getSessionUserId();
            if (userId == null) {
                // bukan error fatal, tapi jelasin
                loanTable.setItems(FXCollections.observableArrayList());
                showInfo("Session", "Session user tidak ditemukan. Silakan login ulang.");
                return;
            }

            // update overdue status first so data returned reflects current overdue state
            loanDao.markOverdueLoans();
            List<Loan> loans = loanDao.findLoansByUser(userId);
            int overdue = loanDao.countOverdueByUser(userId);
            if (overdue > 0) {
                // show persistent inline notification in sidebar
                try { overdueLabel.setText("Pinjaman terlambat: " + overdue + " — segera kembalikan."); } catch (Exception ignored) {}
            } else {
                try { overdueLabel.setText(""); } catch (Exception ignored) {}
            }
            ObservableList<Loan> data = FXCollections.observableArrayList(loans);
            loanTable.setItems(data);

            if (loans == null || loans.isEmpty()) {
                // info halus (tidak ganggu)
                // showInfo("Info", "Belum ada pinjaman.");
            }

        } catch (Exception ex) {
            handleException("Gagal memuat data pinjaman", ex, false);
        }
    }

    // ==========================
    // LOGOUT
    // ==========================
    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            try { Session.class.getMethod("clear").invoke(null); } catch (Exception ignored) {}

            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/library/view/Login.fxml"));
            stage.setScene(new Scene(root));

        } catch (Exception ex) {
            handleException("Logout gagal", ex, false);
        }
    }

    private void goToLoginSafely() {
        try {
            // cari stage dari salah satu node yang pasti ada
            Stage stage = (Stage) tileBooks.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/library/view/Login.fxml"));
            stage.setScene(new Scene(root));
        } catch (Exception ignored) {
            // kalau pun gagal, minimal gak crash
        }
    }

    private String n(String s) { return s == null ? "" : s; }

    /* ==========================
       ERROR HANDLING UTAMA
       ========================== */
    private void handleException(String title, Exception ex, boolean fatal) {
        ex.printStackTrace();

        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) msg = "Terjadi kesalahan. Silakan coba lagi.";

        showError(title, msg);

        if (fatal) {
            // kalau fatal, paksa balik login biar aman
            goToLoginSafely();
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
