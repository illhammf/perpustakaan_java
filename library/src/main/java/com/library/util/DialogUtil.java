package com.library.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import java.util.Optional;

public final class DialogUtil {

    private static final String THEME_CSS = "/com/library/view/dialog.css";

    private DialogUtil() {}

    private static Alert create(Alert.AlertType type, String title, String header, String content) {
        Alert a = new Alert(type);
        a.setTitle(title == null ? "" : title);
        a.setHeaderText(header);
        a.setContentText(content == null ? "" : content);

        try {
            DialogPane dp = a.getDialogPane();
            String css = DialogUtil.class.getResource(THEME_CSS).toExternalForm();
            dp.getStylesheets().add(css);
            dp.getStyleClass().add("dialog-pane");
        } catch (Exception ignored) {}

        return a;
    }

    public static void showInfo(String title, String content) {
        Alert a = create(Alert.AlertType.INFORMATION, title, null, content);
        a.showAndWait();
    }

    public static void showError(String title, String content) {
        Alert a = create(Alert.AlertType.ERROR, title, null, content);
        a.showAndWait();
    }

    public static void showError(String title, Exception ex) {
        String msg = ex == null ? "Terjadi kesalahan." : ex.getMessage();
        Alert a = create(Alert.AlertType.ERROR, title, null, msg);
        a.showAndWait();
    }

    public static boolean confirm(String title, String header, String content) {
        Alert a = create(Alert.AlertType.CONFIRMATION, title, header, content);
        Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }
}
