package com.properties.prop.parser;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ParserApplication extends Application {

	private ConfigurableApplicationContext springContext;
	private Parent root;
	private ParserController controller;
	private FXMLLoader fxmlLoader;

	@Override
	public void init() throws Exception {
		springContext = SpringApplication.run(ParserApplication.class);
		fxmlLoader = new FXMLLoader(getClass().getResource("/parser.fxml"));
		fxmlLoader.setControllerFactory(springContext::getBean);
		root = fxmlLoader.load();
		controller=fxmlLoader.getController();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("BundleBee");
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setMinWidth(1200);
		primaryStage.setMinHeight(180);
		primaryStage.setOnCloseRequest(event -> {
			if(fxmlLoader!=null){
				ParserController parserController=(ParserController)fxmlLoader.getController();
				parserController.shutdownWatchers();
			}
		});
		primaryStage.show();
	}

	@Override
	public void stop() throws Exception {
		springContext.stop();
	}

	public static void main(String[] args) {
		launch(ParserApplication.class, args);
	}

}

