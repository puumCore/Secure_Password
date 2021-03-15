package com.puumInc._securePassword._controller;

import com.puumInc._securePassword.Main;
import com.puumInc._securePassword._outsourced._hash.BCrypt;
import com.puumInc._securePassword._outsourced._hash.ReallyStrongSecuredPassword;
import com.puumInc._securePassword._outsourced._hash.SCryptUtil;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;

/**
 * @author Muriithi_Mandela
 * @version 1.0.1
 */

public class Controller implements Initializable {

    private Task<String> task;
    private final UnaryOperator<TextFormatter.Change> integerFilter = change -> {
        String newText = change.getControlNewText();
        /*integers_with_negative="-?([1-9][0-9]*)?"*/
        if (newText.matches("([0-9][0-9]*)?")) {
            return change;
        } else if ("-".equals(change.getText())) {
            if (change.getControlText().startsWith("-")) {
                change.setText("");
                change.setRange(0, 1);
                change.setCaretPosition(change.getCaretPosition() - 2);
                change.setAnchor(change.getAnchor() - 2);
            } else {
                change.setRange(0, 0);
            }
            return change;
        }
        return null;
    };
    private final StringConverter<Integer> converter = new IntegerStringConverter() {
        @Override
        public Integer fromString(String string) {
            if (string.isEmpty()) {
                return 0;
            }
            return super.fromString(string);
        }
    };

    @FXML
    private TextField plainTextField;

    @FXML
    private ComboBox<String> comboBox;

    @FXML
    private Group silverGroup;

    @FXML
    private Spinner<Integer> logRoundsSpinner;

    @FXML
    private Group bronzeGroup;

    @FXML
    private TextField bronzeParamTF;

    @FXML
    private Group ironGroup;

    @FXML
    private Spinner<Integer> lengthSpinner;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label statusLabel;

    @FXML
    private TextArea hashedResultTextArea;

    @FXML
    void generate_random_text(ActionEvent event) {
        plainTextField.setText(RandomStringUtils.randomAlphabetic(10));
        event.consume();
    }

    @FXML
    private void show_result(ActionEvent event) {
        if (task != null) {
            if (task.isRunning()) {
                error_message_alert("Failed to continue", "We can't start a new task since since one is already started.").show();
                event.consume();
                return;
            }
        }
        if (comboBox.getSelectionModel().getSelectedItem() == null) {
            comboBox.getSelectionModel().select(0);
        }
        if (!comboBox.getSelectionModel().getSelectedItem().equals(SecurityType.RANDOM_ALPHANUMERIC.getMyType())) {
            if (plainTextField.getText().trim().isEmpty() || plainTextField.getText() == null) {
                generate_random_text(new ActionEvent());
            }
        }
        hashedResultTextArea.clear();
        try {
            ((Button) event.getSource()).setDisable(true);
            String string = null;
            if (!comboBox.getSelectionModel().getSelectedItem().equals(SecurityType.RANDOM_ALPHANUMERIC.getMyType())) {
                string = plainTextField.getText().trim();
                if (comboBox.getSelectionModel().getSelectedItem() == null) {
                    comboBox.getSelectionModel().clearAndSelect(0);
                }
            }
            switch (Arrays.stream(SecurityType.values())
                    .filter(securityType -> comboBox.getSelectionModel().getSelectedItem().equals(securityType.getMyType()))
                    .findAny().orElse(SecurityType.RANDOM_ALPHANUMERIC)
            ) {
                case SCRYPT:
                    int anInt = Integer.parseInt(bronzeParamTF.getText());
                    if (the_param_is_a_power_of_two(anInt)) {
                        task = scrypt_algorithm(string, anInt);
                    } else {
                        error_message_alert("Bad Param", "Only provide a number that is a power of 2(two).\n" +
                                "Like 2, 4, 8, ... e.t.c.").show();
                        task = null;
                    }
                    break;
                case REALLY_STRONG_SECURED_PASSWORD:
                    task = most_secure_hashing(string);
                    break;
                case BCRYPT:
                    task = bcrypt_algorithm(string, logRoundsSpinner.getValue());
                    break;
                case RANDOM_ALPHANUMERIC:
                default:
                    task = random_alphanumeric(lengthSpinner.getValue());
                    break;
            }
            if (task != null) {
                progressBar.setProgress(-1.0f);
                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0),
                                new KeyValue(progressBar.progressProperty(), 0)),
                        new KeyFrame(Duration.seconds(1), new KeyValue(progressBar.progressProperty(), 1), new KeyValue(progressBar.progressProperty(), 1)));
                timeline.setOnFinished(event1 -> {
                    try {
                        if (task.isRunning()) {
                            timeline.playFromStart();
                        } else {
                            hashedResultTextArea.setText(task.get());
                            statusLabel.setText("Done");
                            statusLabel.setTextFill(Color.web("#33E411"));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        statusLabel.setText("???? FAILED ????");
                        statusLabel.setTextFill(Color.web("#E60606"));
                    }
                });
                task.setOnFailed(event1 -> {
                    statusLabel.setText("???? FAILED ????");
                    statusLabel.setTextFill(Color.web("#E60606"));
                });
                task.exceptionProperty().addListener(((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        Exception exception = (Exception) newValue;
                        exception.printStackTrace();
                        programmer_error(exception).show();
                    }
                }));
                task.setOnRunning(event1 -> {
                    statusLabel.setText("...processing...");
                    statusLabel.setTextFill(Color.web("#1D1A1A"));
                });
                new Thread(task).start();
                timeline.play();
            }
            ((Button) event.getSource()).setDisable(false);
        } catch (Exception e) {
            e.printStackTrace();
            programmer_error(e).show();
        }
        event.consume();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logRoundsSpinner.setValueFactory(get_spinner_factory(10, 20, 12));
        lengthSpinner.setValueFactory(get_spinner_factory(8, 100, 10));

        accept_only_numbers(bronzeParamTF);

        bronzeParamTF.setText("2");

        comboBox.getItems().removeAll();
        Arrays.stream(SecurityType.values()).forEach(securityType -> comboBox.getItems().add(securityType.getMyType()));
        comboBox.setOnAction(event -> {
            if (comboBox.getSelectionModel().getSelectedItem().equals(SecurityType.BCRYPT.getMyType())) {
                silverGroup.setDisable(false);
                bronzeGroup.setDisable(true);
                ironGroup.setDisable(true);
            } else if (comboBox.getSelectionModel().getSelectedItem().equals(SecurityType.SCRYPT.getMyType())) {
                silverGroup.setDisable(true);
                bronzeGroup.setDisable(false);
                ironGroup.setDisable(true);
            } else if (comboBox.getSelectionModel().getSelectedItem().equals(SecurityType.RANDOM_ALPHANUMERIC.getMyType())) {
                silverGroup.setDisable(true);
                bronzeGroup.setDisable(true);
                ironGroup.setDisable(false);
            } else {
                silverGroup.setDisable(true);
                bronzeGroup.setDisable(true);
                ironGroup.setDisable(true);
            }
            event.consume();
        });
    }

    private boolean the_param_is_a_power_of_two(int param) {
        if (param == 0) {
            return false;
        }
        while (param != 1) {
            param = param /2;
            if (param % 2 != 0 && param != 1) {
                return false;
            }
        }
        return true;
    }

    private void accept_only_numbers(TextField... textFields) {
        for (TextField jfxTextField : textFields) {
            TextFormatter<Integer> textFormatter = new TextFormatter<>(converter, 0, integerFilter);
            jfxTextField.setTextFormatter(textFormatter);
        }
    }

    private SpinnerValueFactory.IntegerSpinnerValueFactory get_spinner_factory(int min, int max, int initialValue) {
        SpinnerValueFactory.IntegerSpinnerValueFactory integerSpinnerValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initialValue, 1);
        integerSpinnerValueFactory.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return object.toString();
            }

            @Override
            public Integer fromString(String string) {
                if (string.trim().isEmpty()) {
                    return 0;
                } else {
                    return Integer.valueOf(string.trim());
                }
            }
        });
        return integerSpinnerValueFactory;
    }

    @NotNull
    private Task<String> random_alphanumeric(int length) {
        return new Task<String>() {
            @Override
            protected String call() {
                updateMessage("Running...");
                return RandomStringUtils.randomAlphanumeric(length);
            }
        };
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    private Task<String> bcrypt_algorithm(String originalPassword, int log_rounds) {
        return new Task<String>() {
            @Override
            protected String call() {
                updateMessage("Running...");
                return BCrypt.hashpw(originalPassword, BCrypt.gensalt(log_rounds));
            }
        };
    }

    @NotNull
    private Task<String> scrypt_algorithm(String originalPassword, int NrpValues) {
        return new Task<String>() {
            @Override
            protected String call() {
                return SCryptUtil.scrypt(originalPassword, NrpValues, NrpValues, NrpValues);
            }
        };
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    private Task<String> most_secure_hashing(String originalPassword) {
        return new Task<String>() {
            @Override
            protected String call() throws Exception {
                return ReallyStrongSecuredPassword.generateStrongPasswordHash(originalPassword);
            }
        };
    }

    @NotNull
    private Alert error_message_alert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(Main.stage);
        alert.setTitle(Main.stage.getTitle());
        alert.setHeaderText(header);
        alert.setContentText(message);
        return alert;
    }

    protected final Alert programmer_error(@NotNull Object object) {
        Exception exception = (Exception) object;
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(Main.stage);
        alert.setTitle("WATCH DOG");
        alert.setHeaderText("ERROR TYPE : " + exception.getClass());
        alert.setContentText("This dialog is a detailed explanation of the error that has occurred");
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        String exceptionText = stringWriter.toString();
        Label label = new Label("The exception stacktrace was: ");
        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        VBox vBox = new VBox();
        vBox.getChildren().add(label);
        vBox.getChildren().add(textArea);
        alert.getDialogPane().setExpandableContent(vBox);
        return alert;
    }

    private enum SecurityType {
        REALLY_STRONG_SECURED_PASSWORD("Gold"), BCRYPT("Silver"), SCRYPT("Bronze"), RANDOM_ALPHANUMERIC("Alpha Numeric");

        private final String myType;

        SecurityType(String type) {
            this.myType = type;
        }

        public String getMyType() {
            return myType;
        }
    }

}
