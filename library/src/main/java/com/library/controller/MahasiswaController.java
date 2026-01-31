package com.library.controller;

import com.library.Session;
import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.User;
import com.library.util.DialogUtil;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

    @FXML private Button btnReturn;

    private final BookDao bookDao = new BookDao();
    private final LoanDao loanDao = new LoanDao();
    private List<Book> allBooks = new ArrayList<>();

    @FXML
    public void initialize() {
        try {
            if (loginInfoLabel != null) loginInfoLabel.setText(buildLoginInfoText());
            setupLoanTable();

            if (btnReturn != null) {
                btnReturn.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                    Loan s = loanTable.getSelectionModel().getSelectedItem();
                    if (s == null) return true;
                    String st = s.getStatus();
                    if (st == null) return true;
                    st = st.toUpperCase();
                    return !(st.equals("DIPINJAM") || st.equals("TERLAMBAT"));
                }, loanTable.getSelectionModel().selectedItemProperty()));
            }

            goHome(null);

            loadBooksToTiles();
            loadLoans();

        } catch (Exception ex) {
            handleException("Gagal memuat halaman Mahasiswa", ex, true);
        }
    }

    private String buildLoginInfoText() {
        String base = "Login sebagai: (Mahasiswa)";
        try {
            User u = Session.getCurrentUser();
            if (u != null) {
                String name = u.getFullName() == null ? "" : u.getFullName().trim();
                String role = u.getRoleName() == null ? "" : u.getRoleName().trim();
                if (!name.isBlank() && !role.isBlank()) return "Login sebagai: " + name + " (" + role + ")";
                if (!name.isBlank()) return "Login sebagai: " + name;
            }
        } catch (Exception ignored) {}
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
        if (pageTitleLabel != null) pageTitleLabel.setText("Rekomendasi / Koleksi Buku");
        showBooksView();
        renderTiles(allBooks);
    }

    @FXML public void goCari(ActionEvent e) {
        if (pageTitleLabel != null) pageTitleLabel.setText("Cari & Pinjam Buku");
        showBooksView();

        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (q.isBlank()) renderTiles(allBooks);
        else renderTiles(filterBooks(q));
    }

    @FXML public void goPinjaman(ActionEvent e) {
        if (pageTitleLabel != null) pageTitleLabel.setText("Pinjaman Saya");
        showLoansView();
        loadLoans();
    }

    private void showBooksView() {
        if (bookScroll != null) { bookScroll.setVisible(true); bookScroll.setManaged(true); }
        if (loanPane != null) { loanPane.setVisible(false); loanPane.setManaged(false); }
    }

    private void showLoansView() {
        if (bookScroll != null) { bookScroll.setVisible(false); bookScroll.setManaged(false); }
        if (loanPane != null) { loanPane.setVisible(true); loanPane.setManaged(true); }
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
                DialogUtil.showInfo("Koleksi kosong", "Belum ada buku di database. Silakan hubungi pustakawan untuk menambahkan data buku.");
            }
        } catch (Exception ex) {
            handleException("Gagal memuat data buku", ex, false);
        }
    }

    private void renderTiles(List<Book> books) {
        if (tileBooks == null) return;

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
        if (loaded) coverBox.getChildren().add(iv);
        else {
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
                DialogUtil.showWarning("Stok habis", "Maaf, stok buku ini sedang habis sehingga tidak bisa dipinjam.");
                return;
            }
            confirmBorrow(b);
        });

        card.getChildren().addAll(coverBox, title, meta, btnBorrow);
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
            } catch (Exception ignored) {}
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

        String desc = getBookDescriptionSafe(b);
        Label descLabel = new Label(desc.isBlank() ? "Deskripsi belum tersedia." : desc);
        descLabel.setStyle("-fx-text-fill:#334155;");
        descLabel.setWrapText(true);

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

    private String getBookDescriptionSafe(Book b) {
        try {
            Object d = b.getClass().getMethod("getDescription").invoke(b);
            return d == null ? "" : d.toString().trim();
        } catch (Exception ignored) {}
        return "";
    }

    private String buildExtraInfoSafe(Book b) {
        StringBuilder sb = new StringBuilder();

        try {
            Object isbn = b.getClass().getMethod("getIsbn").invoke(b);
            if (isbn != null && !isbn.toString().isBlank()) sb.append("ISBN: ").append(isbn).append("\n");
        } catch (Exception ignored) {}

        try {
            Object pub = b.getClass().getMethod("getPublisher").invoke(b);
            if (pub != null && !pub.toString().isBlank()) sb.append("Publisher: ").append(pub).append("\n");
        } catch (Exception ignored) {}

        try {
            Object cat = b.getClass().getMethod("getCategory").invoke(b);
            if (cat != null && !cat.toString().isBlank()) sb.append("Kategori: ").append(cat).append("\n");
        } catch (Exception ignored) {}

        try {
            Object year = b.getClass().getMethod("getPublishYear").invoke(b);
            if (year != null) sb.append("Tahun: ").append(year).append("\n");
        } catch (Exception ignored) {}

        return sb.toString().trim();
    }

    private String nDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private void confirmBorrow(Book b) {
        if (b == null) {
            DialogUtil.showError("Data tidak valid", "Data buku tidak valid (null).");
            return;
        }

        Integer userId = getSessionUserId();
        if (userId == null) {
            DialogUtil.showError("Session habis", "Session user tidak terbaca. Silakan login ulang.");
            goToLoginSafely();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi");
        confirm.setHeaderText("Pinjam buku: " + n(b.getTitle()));
        confirm.setContentText("Due date otomatis (7 hari). Lanjut?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            loanDao.borrowBookTx(userId, b.getId(), 7);

            DialogUtil.showSuccess("Berhasil", "Buku berhasil dipinjam. Silakan cek menu Pinjaman Saya.");
            loadBooksToTiles();
            loadLoans();
            goPinjaman(null);

        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (msg.contains("stock") || msg.contains("stok")) {
                DialogUtil.showWarning("Stok habis", "Stok buku sudah habis saat proses peminjaman.");
            } else if (msg.contains("duplicate") || msg.contains("sudah") || msg.contains("already")) {
                DialogUtil.showWarning("Sudah dipinjam", "Buku ini sudah kamu pinjam / masih berstatus aktif.");
            } else {
                DialogUtil.showError("Gagal meminjam buku", ex);
            }
        }
    }

    @FXML
    public void handleReturnSelected(ActionEvent e) {
        Loan selected = loanTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showInfo("Belum dipilih", "Pilih pinjaman yang ingin dikembalikan terlebih dahulu.");
            return;
        }

        Integer userId = getSessionUserId();
        if (userId == null) {
            DialogUtil.showError("Session habis", "Silakan login ulang.");
            goToLoginSafely();
            return;
        }

        // pastikan milik user ini
        try {
            int loanUserId = selected.getUserId();
            if (loanUserId != userId) {
                DialogUtil.showError("Akses ditolak", "Anda hanya dapat mengembalikan pinjaman Anda sendiri.");
                return;
            }
        } catch (Exception ignored) {}

        String status = selected.getStatus() == null ? "" : selected.getStatus().toUpperCase();
        if (!status.equals("DIPINJAM") && !status.equals("TERLAMBAT")) {
            DialogUtil.showWarning("Tidak bisa dikembalikan", "Pinjaman ini tidak dalam status yang bisa dikembalikan.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Pengembalian");
        confirm.setHeaderText("Kembalikan buku: " + n(selected.getBookTitle()));
        confirm.setContentText("Jika terlambat, sistem akan menghitung denda otomatis.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            int finePerDay = 5000;
            loanDao.returnBookTx(selected.getId(), finePerDay);

            DialogUtil.showSuccess("Berhasil", "Buku berhasil dikembalikan.");
            loadBooksToTiles();
            loadLoans();
            goPinjaman(null);

        } catch (Exception ex) {
            DialogUtil.showError("Gagal mengembalikan buku", ex);
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
                DialogUtil.showInfo("Kata kunci kosong", "Kamu belum mengetik kata kunci. Menampilkan semua buku.");
                return;
            }

            List<Book> filtered = filterBooks(q);
            renderTiles(filtered);

            if (filtered.isEmpty()) {
                DialogUtil.showInfo("Tidak ditemukan", "Buku tidak ditemukan untuk kata kunci: " + q);
            } else {
                DialogUtil.showSuccess("Hasil pencarian", "Ditemukan " + filtered.size() + " buku untuk kata kunci: " + q);
            }

            goCari(null);

        } catch (Exception ex) {
            DialogUtil.showError("Pencarian gagal", ex);
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
            DialogUtil.showSuccess("Berhasil", "Data berhasil diperbarui.");
        } catch (Exception ex) {
            DialogUtil.showError("Refresh gagal", ex);
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
                loanTable.setItems(FXCollections.observableArrayList());
                if (overdueLabel != null) overdueLabel.setText("");
                return;
            }

            loanDao.markOverdueLoans();
            List<Loan> loans = loanDao.findLoansByUser(userId);
            int overdue = loanDao.countOverdueByUser(userId);

            if (overdueLabel != null) {
                if (overdue > 0) overdueLabel.setText("⚠ Pinjaman terlambat: " + overdue + " — segera kembalikan.");
                else overdueLabel.setText("");
            }

            ObservableList<Loan> data = FXCollections.observableArrayList(loans == null ? new ArrayList<>() : loans);
            loanTable.setItems(data);

        } catch (Exception ex) {
            DialogUtil.showError("Gagal memuat data pinjaman", ex);
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
            DialogUtil.showError("Logout gagal", ex);
        }
    }

    private void goToLoginSafely() {
        try {
            Stage stage = (Stage) tileBooks.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/library/view/Login.fxml"));
            stage.setScene(new Scene(root));
        } catch (Exception ignored) {}
    }

    private void handleException(String title, Exception ex, boolean fatal) {
        DialogUtil.showError(title, ex);
        if (fatal) goToLoginSafely();
    }

    private String n(String s) { return s == null ? "" : s; }
}
