package com.library.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DialogUtil {

    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    // ===== Public helpers =====
    public static void showInfo(String title, String message) {
        show(Type.INFO, title, message);
    }
    public static void showSuccess(String title, String message) {
        show(Type.SUCCESS, title, message);
    }
    public static void showWarning(String title, String message) {
        show(Type.WARNING, title, message);
    }
    public static void showError(String title, String message) {
        show(Type.ERROR, title, message);
    }

    public static void showError(String title, Exception ex) {
        String msg = (ex == null || ex.getMessage() == null || ex.getMessage().isBlank())
                ? "Terjadi kesalahan. Silakan coba lagi."
                : ex.getMessage();
        show(Type.ERROR, title, msg);
    }

    // ===== Core dialog =====
    public static void show(Type type, String title, String message) {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        // overlay
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.35);");
        overlay.setPadding(new Insets(18));

        // card
        BorderPane card = new BorderPane();
        card.setMaxWidth(460);
        card.setMinWidth(420);
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 14;" +
                "-fx-border-radius: 14;" +
                "-fx-border-color: rgba(148,163,184,0.45);" +
                "-fx-border-width: 1;"
        );
        card.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.25)));

        // header (left color bar + title)
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 14 14 0 0;");

        Region bar = new Region();
        bar.setPrefWidth(8);
        bar.setMinWidth(8);
        bar.setMaxWidth(8);
        bar.setPrefHeight(42);
        bar.setStyle("-fx-background-radius: 8; -fx-background-color: " + color(type) + ";");

        Label titleLabel = new Label(title == null ? "" : title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(icon(type));
        badge.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + color(type) + ";");

        header.getChildren().addAll(bar, titleLabel, spacer, badge);

        // body
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 16, 6, 16));

        Label msg = new Label(message == null ? "" : message);
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        msg.setMaxWidth(420);

        body.getChildren().add(msg);

        // footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 16, 14, 16));

        Button ok = new Button("OK");
        ok.setDefaultButton(true);
        ok.setStyle(
                "-fx-background-radius: 10;" +
                "-fx-padding: 8 18;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: " + color(type) + ";"
        );
        ok.setOnAction(e -> dialog.close());

        footer.getChildren().add(ok);

        card.setTop(header);
        card.setCenter(body);
        card.setBottom(footer);

        overlay.getChildren().add(card);

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);

        // allow close by ESC / click outside
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) dialog.close(); });
        overlay.setOnMouseClicked(e -> {
            // klik luar card = tutup
            if (e.getTarget() == overlay) dialog.close();
        });

        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }

    private static String color(Type type) {
        return switch (type) {
            case INFO -> "#0ea5e9";
            case SUCCESS -> "#22c55e";
            case WARNING -> "#f59e0b";
            case ERROR -> "#ef4444";
        };
    }

    private static String icon(Type type) {
        return switch (type) {
            case INFO -> "i";
            case SUCCESS -> "✓";
            case WARNING -> "!";
            case ERROR -> "×";
        };
    }
    public static boolean confirm(String title, Object object, String msg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'confirm'");
    }
}
