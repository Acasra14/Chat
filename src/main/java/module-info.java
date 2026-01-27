module org.pspro_activ2_castellano_ramos_adrian.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    opens org.pspro_activ2_castellano_ramos_adrian.chat to javafx.fxml;
    exports org.pspro_activ2_castellano_ramos_adrian.chat;
}