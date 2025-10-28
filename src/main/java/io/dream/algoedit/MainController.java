package io.dream.algoedit;

import javafx.application.Platform;
import javafx.event.ActionEvent;

public class MainController
{
    public void Quit(ActionEvent event)
    {
//        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
        Platform.exit();
    }
}
