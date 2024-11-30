module com.example.wikicraft {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject; // javascript
    requires org.jsoup; // html indentations
    requires com.jfoenix; // fancy looking stuff

    // not currently used:
//    requires org.reactfx;
//    requires flowless;
//    requires org.fxmisc.richtext;
//    requires org.controlsfx.controls;
//    requires com.dlsc.formsfx;
//    requires org.kordamp.ikonli.javafx;
//    requires org.kordamp.bootstrapfx.core;
//    requires eu.hansolo.tilesfx;

    opens com.example.wikicraft to javafx.fxml;
    exports com.example.wikicraft;
}