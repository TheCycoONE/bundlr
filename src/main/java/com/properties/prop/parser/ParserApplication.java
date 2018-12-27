package com.properties.prop.parser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ParserApplication extends Application {

	private ConfigurableApplicationContext springContext;
	private Parent root;
	private ParserController controller;

	@Override
	public void init() throws Exception {
		springContext = SpringApplication.run(ParserApplication.class);
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/parser.fxml"));
		fxmlLoader.setControllerFactory(springContext::getBean);
		root = fxmlLoader.load();
		controller=fxmlLoader.getController();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("bundlr");
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setMinWidth(1200);
		primaryStage.setMinHeight(180);
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

