package com.properties.prop.parser;

import com.properties.prop.parser.model.Bundle;
import com.properties.prop.parser.model.Resource;
import com.properties.prop.parser.model.Tuple;
import com.properties.prop.parser.service.BundleService;
import com.properties.prop.parser.service.FileService;
import com.properties.prop.parser.service.ResourceIndexService;
import com.properties.prop.widget.EditCell3;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ParserController {
    private static final String CHOOSE_COLUMN = "Choose column";
    private static final String FILTER_BUNDLES = "Filter bundles";
    private static final String ENTER_FILTER_CRITERIA = "Enter filter criteria";
    private static final String ENTER_SEARCH_TEXT = "Enter search text";
    private static final String RESET_BUNDLE_FILTER = "Reset bundle filter";
    private static final String GET_ALL_PROPERTIES = "Get all properties";
    private static final String DELETE_BUNDLE = "Delete bundle";
    private static final String SEARCH_PROPERTIES = "Search properties";
    private static final String CHOOSE_BUNDLE = "Choose bundle";

    @FXML private AnchorPane anchorId;
    @FXML private Pane tablePane;
    @FXML private TextField searchBar;
    @FXML private ComboBox bundleBox;
    @FXML private TextField bundleSearchField;
    @FXML private Button openFolderBtn;
    @FXML private Button bundleSearchBtn;
    @FXML private Button resetFilterBtn;
    @FXML private Button allBtn;
    @FXML private Button searchResourcesBtn;
    @FXML private Button deleteBundleBtn;
    @FXML private ComboBox searchOptionsBox;

    private ObservableList<Resource> resources;
    private Bundle currentBundle;
    private ObservableList<Bundle> bundles=FXCollections.synchronizedObservableList(FXCollections.observableArrayList(Collections.emptyList()));
    private TableView parserTable;
    Map<String,ObservableList<Resource>> searchResourcesMap;
    private String searchOption;
    private ChangeListener<Bundle> bundleChangeListener;

    @Autowired
    private FileService fileService;

    @Autowired
    private ResourceIndexService resourceIndexService;

    @Autowired
    private BundleService bundleService;

    @FXML
    public void initialize() throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        parserTable=new TableView<Resource>();
        parserTable.getColumns().add(new TableColumn<>("code"));
        tablePane.getChildren().add(parserTable);
        parserTable.prefWidthProperty().bind(tablePane.widthProperty());
        parserTable.prefHeightProperty().bind(tablePane.heightProperty());
        parserTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        searchResourcesMap=Collections.emptyMap();
        bundleBox.setTooltip(new Tooltip(CHOOSE_BUNDLE));
        searchOptionsBox.setTooltip(new Tooltip(CHOOSE_COLUMN));
        bundleSearchBtn.setTooltip(new Tooltip(FILTER_BUNDLES));
        bundleSearchField.setTooltip(new Tooltip(ENTER_FILTER_CRITERIA));
        searchBar.setTooltip(new Tooltip(ENTER_SEARCH_TEXT));
        resetFilterBtn.setTooltip(new Tooltip(RESET_BUNDLE_FILTER));
        allBtn.setTooltip(new Tooltip(GET_ALL_PROPERTIES));
        deleteBundleBtn.setTooltip(new Tooltip(DELETE_BUNDLE));
        searchResourcesBtn.setTooltip(new Tooltip(SEARCH_PROPERTIES));
        Callback<ListView<Bundle>,ListCell<Bundle>> cellFactory= new Callback<>() {
            @Override
            public ListCell<Bundle> call(ListView param) {
                return new ListCell<>() {
                    {
                        super.autosize();
                    }
                    @Override
                    protected void updateItem(Bundle item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                        } else {
                            setText(item.getName());
                        }
                    }
                };
            }
        };
        bundleBox.setButtonCell(cellFactory.call(null));
        bundleBox.setCellFactory(cellFactory);

        try {
            bundles=bundleService.loadBundles();
            FXCollections.sort(bundles,Comparator.comparing(Bundle::getName));
            resourceIndexService.loadStores(bundles.stream().map(Bundle::getName).collect(Collectors.toList()));
            updateIndexes();
        } catch (IOException | ExecutionException | InterruptedException e) {
            bundles = FXCollections.observableArrayList();
        }
        bundleChangeListener=(observable, oldValue, newValue) -> {
            if(newValue!=null){
                try {
                    changeBundle(newValue);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ConfigurationException e) {
                    e.printStackTrace();
                }
            }
        };
        bundleBox.getSelectionModel().selectedItemProperty().addListener(bundleChangeListener);
        searchOptionsBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue!=null){
                if(searchResourcesMap!=null&&!searchResourcesMap.isEmpty()){
                    ObservableList<Resource> resources=searchResourcesMap.get(newValue);
                    if(resources!=null&&!resources.isEmpty()){
                        parserTable.setItems(resources);
                    }
                }
            }
        });
        if(bundles!=null&&!bundles.isEmpty()) {
            currentBundle = bundles.get(0);
            List<File> files = bundles.stream().map(bundle -> Path.of(currentBundle.getPath()).getParent().toFile()).distinct().collect(Collectors.toList());
            List<CompletableFuture> completableFutures = new ArrayList<>();
            for (File file : files) {
                CompletableFuture<Void> asyncCompletableFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        processDirectory(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ConfigurationException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
                asyncCompletableFuture.exceptionally(ex -> {
                    return null;
                });
                completableFutures.add(asyncCompletableFuture);
            }
            CompletableFuture<Void> completableFuture = CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new));
            completableFuture.exceptionally((ex) -> {
                return null;
            });
            completableFuture.get();
            FXCollections.sort(bundles, Comparator.comparing(Bundle::getName));
            bundleBox.setItems(bundles);
            setBundle(bundles.get(0));
        }
        bundleSearchField.setOnKeyPressed(event -> {
            try {
                triggerFilterBundles(event);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        searchBar.setOnKeyPressed(event -> {
            triggerSearchFile(event);
        });
        searchResourcesBtn.setOnKeyPressed(event -> {
            triggerSearchFile(event);
        });
        deleteBundleBtn.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER){
                try {
                    deleteBundle();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ConfigurationException e) {
                    e.printStackTrace();
                }
            }
        });
        openFolderBtn.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER){
                try {
                    openDirectory();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ConfigurationException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        bundleSearchBtn.setOnKeyPressed(event -> {
            try {
                triggerFilterBundles(event);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        resetFilterBtn.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER){
                try {
                    resetFilter();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        allBtn.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER){
                try {
                    getAllResources();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        bundleBox.setOnKeyPressed(event -> {
            if(event.getCode()==KeyCode.ENTER){
                bundleBox.show();
            }
        });
        setSortPolicy();
        searchOptionsBox.setOnKeyPressed(event -> {
            if(event.getCode()==KeyCode.ENTER){
                searchOptionsBox.show();
            }
        });
        parserTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void triggerSearchFile(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            try {
                searchFile();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    private void triggerFilterBundles(KeyEvent event) throws IOException {
        if (event.getCode() == KeyCode.ENTER) {
            try {
                if(bundleSearchField.getText().equals("")){
                    resetFilter();
                }else {
                    filterBundles();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void changeBundle(Bundle bundle) throws IOException, ConfigurationException {
            currentBundle = bundle;
            if (bundle.getFileMap().isEmpty()) {
                updateStoreUI(bundle);
            } else {
                changeColumnNames(bundle.getFileMap());
                loadData(bundle.getName());
            }
            bundleBox.getSelectionModel().select(bundle);
    }
    private void setBundle(Bundle bundle){
        bundleBox.getSelectionModel().select(bundle);
    }
    private void softChangeBundle(Bundle bundle) throws IOException, ConfigurationException {
        currentBundle=bundle;
        changeColumnNames(bundle.getFileMap());
        specialLoadData(bundle.getName());
        bundleBox.getSelectionModel().selectedItemProperty().removeListener(bundleChangeListener);
        bundleBox.getSelectionModel().select(bundle);
        bundleBox.getSelectionModel().selectedItemProperty().addListener(bundleChangeListener);
    }

    private void updateIndexes() throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        Iterator<Bundle> bundleIterator=bundles.listIterator();
        while (bundleIterator.hasNext()){
            Bundle bundle=bundleIterator.next();
            Path path=Paths.get(bundle.getPath());
            if(Files.notExists(path)){
                resourceIndexService.deleteStore(bundle.getName());
                bundleService.deleteBundle(bundle);
                bundleIterator.remove();
            }
        }
        List<CompletableFuture<Void>> completableFutures=new ArrayList<>();
        for(Bundle bundle : bundles){
            CompletableFuture<Void> asyncCompletableFuture=CompletableFuture.supplyAsync(() ->{
                File file=new File(bundle.getPath());
                File[] fileArray=file.listFiles();
                long lastModified=-1;
                boolean folderWasModified=file.lastModified()!=bundle.getLastModified();
                boolean subFileMasModified=Arrays.stream(fileArray).anyMatch(subFile -> subFile.lastModified()!=bundle.getLastModified());
                boolean wasModified=folderWasModified||subFileMasModified;
                if(folderWasModified){
                    lastModified=file.lastModified();
                }else if(subFileMasModified) {
                    lastModified=Arrays.stream(fileArray).filter(subFile -> subFile.lastModified()!=bundle.getLastModified()).map(subFile -> subFile.lastModified()).findFirst().orElse(-1L);
                }
                if(wasModified) {
                    if (fileArray != null) {
                        List<File> files = Arrays.stream(fileArray) //
                                .filter(subFile -> FilenameUtils.getBaseName(subFile.getName()).startsWith(bundle.getName())) //
                                .collect(Collectors.toList());
                        Map<String, String> fileMap = new LinkedHashMap<>();
                        for (File currentFile : files) {
                            fileMap.put(FilenameUtils.getBaseName(currentFile.getName()), currentFile.getPath());
                        }
                        bundle.setFileMap(fileMap);
                        resourceIndexService.createLanguageBasedAnalyzer(bundle.getName(), fileMap.keySet());
                        List<Resource> resources = null;
                        try {
                            resources = fileService.loadRowData(files);
                        } catch (ConfigurationException e) {
                            e.printStackTrace();
                        }
                        try {
                            resourceIndexService.reloadDocuments(bundle.getName(), resources);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    bundle.setLastModified(lastModified);
                }
                return null;
            });
            asyncCompletableFuture.exceptionally(ex ->{
                return null;
            });
            completableFutures.add(asyncCompletableFuture);
        }
        CompletableFuture<Void> voidCompletableFuture=CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new));
        voidCompletableFuture.exceptionally((ex)->{
            return null;
        });
        voidCompletableFuture.get();
    }

    private void updateStoreUI(Bundle bundle) throws IOException, ConfigurationException {
        Map<String,String> fileMap=bundle.getFileMap();
        File file=new File(bundle.getPath());
        String bundleName=bundle.getName();
        if(fileMap.isEmpty()){
            if (file != null) {
                File[] fileArray = file.listFiles();
                if (fileArray != null) {
                    List<File> files = Arrays.stream(fileArray) //
                            .filter(subFile -> FilenameUtils.getBaseName(subFile.getName()).startsWith(bundleName)) //
                            .collect(Collectors.toList());
                    try (Stream<Path> paths = Files.walk(Paths.get(file.getAbsolutePath()))) {
                        updateFileMap(bundle.getName(),fileMap, paths);
                        changeColumnNames(fileMap);
                        loadData(bundle.getName(), files);
                    }
                }
            }
        }
    }

    private void updateFileMap(String bundleName,Map<String, String> fileMap, Stream<Path> paths) {
        List<Path> pathList = paths
                .filter(Files::isRegularFile)
                .filter(filePath -> FilenameUtils.getBaseName(filePath.toString()).startsWith(bundleName))
                .filter(filePath -> FilenameUtils.getExtension(filePath.toString()).equals("properties"))
                .collect(Collectors.toList());
        for (Path path : pathList) {
            fileMap.put(FilenameUtils.getBaseName(path.toString()), path.toString());
        }
    }

    private void processBundleWithoutUI(Bundle bundle) throws IOException, ConfigurationException {
        Map<String,String> fileMap=bundle.getFileMap();
        File file=new File(bundle.getPath());
        String bundleName=bundle.getName();
        if(fileMap.isEmpty()){
            if (file != null) {
                File[] fileArray = file.listFiles();
                if (fileArray != null) {
                    List<File> files = Arrays.stream(fileArray) //
                            .filter(subFile -> FilenameUtils.getBaseName(subFile.getName()).startsWith(bundleName)) //
                            .collect(Collectors.toList());
                    try (Stream<Path> paths = Files.walk(Paths.get(file.getAbsolutePath()))) {
                        updateFileMap(bundle.getName(),fileMap, paths);
                        addResourcesToIndex(bundle,files);
                    }
                }
            }
        }
    }

    private void clearParseTable(){
        ObservableList<TableColumn> columns=(ObservableList<TableColumn>)parserTable.getColumns();
        columns.removeIf(tableColumn -> !tableColumn.getText().equals("code"));
        parserTable.getItems().clear();
    }

    private String[] getFieldsArray() {
        return getFields(currentBundle);
    }
    private String[] getFieldsArray(Bundle bundle) {
        return getFields(bundle);
    }

    private String[] getFields(Bundle bundle) {
        List<String> fields = new ArrayList<>(bundle.getFileMap().keySet());
        fields.add("code");
        sortFields(fields);
        return fields.toArray(String[]::new);
    }

    private void sortFields(List<String> fields) {
        Collections.sort(fields, (o1, o2) -> {
            String s1 = o1.length()>4 ? o1.substring(o1.length() - 5): o1;
            String s2 = o2.length()>4 ? o2.substring(o2.length() - 5): o2;
            if (s1.equals(s2)) {
                return 0;
            }
            return s1.compareTo(s2);
        });
    }

    private void loadData(String storeName,List<File> files) throws IOException, ConfigurationException {
        if(resources!=null) {
            resources.clear();
        }
        if(!resourceIndexService.storeExists(storeName)){
            resourceIndexService.createStore(storeName);
            resourceIndexService.createLanguageBasedAnalyzer(storeName,currentBundle.getFileMap().keySet());
            resources=fileService.loadRowData(files);
            resourceIndexService.reloadDocuments(storeName,resources);
        }else {
            resourceIndexService.createLanguageBasedAnalyzer(storeName,currentBundle.getFileMap().keySet());
            resources=resourceIndexService.getAllResources(storeName);
        }
        parserTable.setItems(resources);
        parserTable.getItems().add(new Resource(""));
    }
    private void addResourcesToIndex(Bundle bundle,List<File> files) throws IOException, ConfigurationException {
        String storeName=bundle.getName();
        if(!resourceIndexService.storeExists(storeName)){
            resourceIndexService.createStore(storeName);
            resourceIndexService.createLanguageBasedAnalyzer(storeName,bundle.getFileMap().keySet());
            List<Resource> resources=fileService.loadRowData(files);
            resourceIndexService.reloadDocuments(storeName,resources);
        }
    }

    private void loadData(String storeName) throws IOException, ConfigurationException {
        if(resources!=null) {
            resources.clear();
        }
        if(resourceIndexService.storeExists(storeName)){
            resourceIndexService.createLanguageBasedAnalyzer(storeName,currentBundle.getFileMap().keySet());
            resources=resourceIndexService.getAllResources(storeName);
        }
        parserTable.setItems(resources);
        parserTable.getItems().add(new Resource(""));
    }
    private void specialLoadData(String storeName) throws IOException, ConfigurationException {
        if(resources!=null) {
            resources.clear();
        }
        if(resourceIndexService.storeExists(storeName)){
            resourceIndexService.createLanguageBasedAnalyzer(storeName,currentBundle.getFileMap().keySet());
            resources=searchResourcesMap.get(searchOption);
        }
        parserTable.setItems(resources);
        parserTable.getItems().add(new Resource(""));
    }

    private void changeColumnNames(Map<String,String> fileMap) {
        parserTable.getColumns().clear();
        tablePane.getChildren().remove(parserTable);
        parserTable=new TableView<Resource>();
        parserTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setSortPolicy();
        parserTable.setEditable(true);
        tablePane.getChildren().add(parserTable);
        parserTable.prefWidthProperty().bind(tablePane.widthProperty());
        parserTable.prefHeightProperty().bind(tablePane.heightProperty());
        List<String> columnNames= new ArrayList<>(fileMap.keySet());
        sortFields(columnNames);
        List<String> searchOptions=new ArrayList<>();
        searchOptions.add("code");
        searchOptions.addAll(columnNames);
        searchOptionsBox.setItems(FXCollections.observableArrayList(searchOptions));
        searchOptionsBox.getSelectionModel().select(0);
        List<TableColumn> tableColumns=columnNames.stream().map(TableColumn::new).collect(Collectors.toList());
        TableColumn codeColumn=new TableColumn("code");
        codeColumn.setCellValueFactory(new PropertyValueFactory<Resource,String>("code"));
        codeColumn.setCellFactory(param -> new EditCell3());
        codeColumn.setOnEditCommit((Event event) -> {
                    CellEditEvent<Resource, String> cellEditEvent = (CellEditEvent<Resource, String>) event;
                    Resource resource = (cellEditEvent).getTableView().getItems().get(
                            cellEditEvent.getTablePosition().getRow());
                    String oldCode = resource.getCode();
                    boolean isCodeRepeated=resources.stream().map(currentResource -> currentResource.getCode()).anyMatch(code -> code.equals(cellEditEvent.getNewValue()));
                    if (!cellEditEvent.getNewValue().equals("")) {
                        if(!cellEditEvent.getNewValue().equals(oldCode)&&!cellEditEvent.getNewValue().contains(" ")) {
                            if(!isCodeRepeated) {
                                resource.setCode(cellEditEvent.getNewValue());
                                try {
                                    if (!oldCode.equals("")) {
                                        resourceIndexService.updateDocument(currentBundle.getName(), resource);
                                        List<Tuple> tuples = new LinkedList<>();
                                        for (String key : fileMap.keySet()) {
                                            tuples.add(new Tuple(fileMap.get(key), resource.getPropertyValue(key)));
                                        }
                                        fileService.updateKeyInFiles(tuples, oldCode, resource.getCode());
                                        long lastModified=Files.getLastModifiedTime(Path.of(currentBundle.getPath())).toMillis();
                                        currentBundle.setLastModified(lastModified);
                                        bundleService.updateBundle(currentBundle);
                                    } else {
                                        parserTable.getItems().add(new Resource(""));
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (ConfigurationException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                parserTable.refresh();
                            }
                        }else{
                            parserTable.refresh();
                        }
                    }else{
                        parserTable.refresh();
                    }
                }
        );
        for(TableColumn tableColumn : tableColumns){
            tableColumn.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Resource, String>, ObservableValue<String>>) r -> r.getValue().getProperty(tableColumn.getText()));
            tableColumn.setCellFactory(param -> new EditCell3());
            tableColumn.setOnEditCommit((Event event) ->{
                CellEditEvent<Resource,String> cellEditEvent=(CellEditEvent<Resource,String>) event;
                Resource resource=(cellEditEvent).getTableView().getItems().get(
                        cellEditEvent.getTablePosition().getRow());
                if(!resource.getCode().equals("")) {
                    if(!resource.getPropertyValue(tableColumn.getText()).equals(cellEditEvent.getNewValue())) {
                        resource.setProperty(tableColumn.getText(), cellEditEvent.getNewValue());
                        try {
                            resourceIndexService.updateDocument(currentBundle.getName(), resource);
                            fileService.saveOrUpdateProperty(currentBundle.getFileMap().get(tableColumn.getText()), resource.getCode(), resource.getPropertyValue(tableColumn.getText()));
                            long lastModified=Files.getLastModifiedTime(Path.of(currentBundle.getPath())).toMillis();
                            currentBundle.setLastModified(lastModified);
                            bundleService.updateBundle(currentBundle);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ConfigurationException e) {
                            e.printStackTrace();
                        }
                    }else{
                        parserTable.refresh();
                    }
                }else{
                    parserTable.refresh();
                }
            });
        }
        parserTable.getColumns().add(codeColumn);
        parserTable.getColumns().addAll(tableColumns);
        int numberOfCols=parserTable.getColumns().size();
        List<TableColumn> columns=(List<TableColumn>)parserTable.getColumns();
        for(TableColumn column : columns){
            column.prefWidthProperty().bind(parserTable.widthProperty().divide(numberOfCols));
        }
    }

    private void setSortPolicy() {
        parserTable.sortPolicyProperty().set((Callback<TableView<Resource>, Boolean>) param -> {
            Comparator<Resource> comparator = (o1, o2) -> o1.getCode().equals("") ? 1
                    : o2.getCode().equals("") ? -1
                    : param.getComparator() == null ? 0
                    : param.getComparator().compare(o1, o2);
            FXCollections.sort(param.getItems(), comparator);
            return true;
        });
    }

    @FXML private void filterBundles() throws IOException, ParseException {
        if(currentBundle!=null) {
            String queryString = bundleSearchField.getText();
            ObservableList<Bundle> searchedBundles;
            if (queryString.equals("")) {
                queryString = "*:*";
            } else {
                queryString = queryString + "*";
            }
            searchedBundles = bundleService.searchBundles(queryString);
            if (!searchedBundles.isEmpty()) {
                updateBundleBox(searchedBundles);
            }
        }
    }

    @FXML private void openDirectory() throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        final DirectoryChooser directoryChooser=new DirectoryChooser();
        Stage stage= (Stage) anchorId.getScene().getWindow();
        File file=directoryChooser.showDialog(stage);
        processDirectory(file);
        FXCollections.sort(bundles, Comparator.comparing(Bundle::getName));
        if(bundles!=null&&!bundles.isEmpty()) {
            setBundle(bundles.get(0));
        }
    }

    private void processDirectory(File file) throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        if(file!=null) {
            if (containsBundles(file)) {
                processSingleBundleDirectory(file);
            } else {
                File[] subFiles = file.listFiles();
                List<CompletableFuture> completableFutures=new ArrayList<>();
                for (File subFile : subFiles) {
                    CompletableFuture<Void> asyncCompletableFuture=CompletableFuture.supplyAsync(() -> {
                        try {
                            if (containsBundles(subFile)) {
                                processBundleDirectory(subFile);
                            }
                        }catch (IllegalStateException e){

                        }catch (NullPointerException e){

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ConfigurationException e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
                    asyncCompletableFuture.exceptionally(ex ->{
                        return null;
                    });
                    completableFutures.add(asyncCompletableFuture);
                }
                CompletableFuture<Void> completableFuture=CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new));
                completableFuture.exceptionally((ex)->{
                    return null;
                });
                completableFuture.get();
            }
        }
    }

    private boolean containsBundles(File file) throws IOException {
        return Arrays.asList(file.listFiles())
                .stream() //
                .anyMatch(subFile -> subFile.isFile()&& FilenameUtils.getExtension(subFile.getPath()).equals("properties")); //
    }

    private void processSingleBundleDirectory(File file) throws IOException, ConfigurationException {
        if(file!=null) {
            File[] subFiles=file.listFiles();
            if(subFiles!=null) {
                List<Bundle> fileBundles = getBundles(file, subFiles);
                FXCollections.sort(bundles, Comparator.comparing(Bundle::getName));
                bundleBox.getSelectionModel().select(fileBundles.get(0));
            }
        }
    }

    private synchronized List<Bundle> getBundles(File file, File[] subFiles) throws IOException {
        List<Bundle> fileBundles = Arrays.stream(subFiles)
                .filter(subFile -> FilenameUtils.getBaseName(subFile.getPath()).matches(".*_[a-z]{2}_[A-Z]{2}"))
                .filter(Predicate.not(subFile ->  bundlesExist(getBundleName(subFile)))
                ) //
                .map(subFile -> new Bundle(getBundleName(subFile), file.getPath(),file.lastModified())) //
                .filter(distinctByKey(Bundle::getName))//
                .collect(Collectors.toList());
        bundles.addAll(fileBundles);

        bundleBox.setItems(bundles);
        bundleService.addBundles(fileBundles);
        return fileBundles;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
    private String getBundleName(File file){
        String name=FilenameUtils.getBaseName(file.getPath());
        return name.substring(0,name.length()-6);
    }
    private boolean bundlesExist(String name){
        return bundles.stream().anyMatch(bundle -> bundle.getName().equals(name));
    }

    private void processBundleDirectory(File file) throws IOException, ConfigurationException {
        if(file!=null) {
            if(file!=null) {
                File[] subFiles=file.listFiles();
                if(subFiles!=null) {
                    List<Bundle> fileBundles = getBundles(file, subFiles);
                    for(Bundle bundle : fileBundles){
                        processBundleWithoutUI(bundle);
                    }
                }
            }
        }
    }

    @FXML private void resetFilter() throws IOException {
        if(currentBundle!=null) {
            ObservableList<Bundle> searchedBundles = bundleService.loadBundles();
            if (!searchedBundles.isEmpty()) {
                bundleSearchField.clear();
                updateBundleBox(searchedBundles);
            }
        }
    }

    private void updateBundleBox(ObservableList<Bundle> searchedBundles) {
        bundleBox.getItems().clear();
        bundles=searchedBundles;
        FXCollections.sort(bundles, Comparator.comparing(Bundle::getName));
        bundleBox.setItems(searchedBundles);
        bundleBox.getSelectionModel().select(0);
        bundleBox.show();
    }

    @FXML private void getAllResources() throws IOException {
        if(currentBundle!=null) {
            searchBar.clear();
            if (resourceIndexService.storeExists(currentBundle.getName())) {
                ObservableList<Resource> searchedResources = resourceIndexService.getAllResources(currentBundle.getName());
                parserTable.setItems(searchedResources);
            }
            reloadAllColumns();
            parserTable.getItems().add(new Resource(""));
        }
    }

    @FXML private void searchFile() throws IOException, ParseException, ConfigurationException {
        if(currentBundle!=null) {
            String searchString=searchBar.getText();
            String[] fieldsArray = getFieldsArray();
            searchOption = searchOptionsBox.getSelectionModel().getSelectedItem().toString();
            ObservableList<Resource> searchedResources;
            boolean matchFound=false;
            if (resourceIndexService.storeExists(currentBundle.getName())) {
                if(!searchString.matches("( +)")&&!searchString.equals("")) {
                    loadResourcesMap(searchString, fieldsArray, currentBundle);
                    matchFound = searchResourcesMap.values().stream().anyMatch(Predicate.not(list -> list.isEmpty()));
                    if (matchFound) {
                        setSelectedOption(searchString);
                    }
                }else{
                    loadResourcesMap(searchString, fieldsArray, currentBundle);
                    resources=resourceIndexService.getAllResources(currentBundle.getName());
                    parserTable.setItems(resources);
                    parserTable.getItems().add(new Resource(""));
                    reloadAllColumns();
                    matchFound=true;
                }
            }
            if(!matchFound){
                List<Bundle> otherBundles=bundles.stream().filter(Predicate.not(bundle -> bundle.getName().equals(currentBundle.getName()))).collect(Collectors.toList());
                for(Bundle bundle : otherBundles) {
                        String[] bundleFieldsArray=getFields(bundle);
                        if (resourceIndexService.storeExists(bundle.getName())) {
                            loadResourcesMap(searchString,bundleFieldsArray,bundle);
                            matchFound = searchResourcesMap.values().stream().anyMatch(Predicate.not(list -> list.isEmpty()));
                            if(matchFound){
                                softChangeBundle(bundle);
                                setSelectedOption(searchString);
                                break;
                            }
                        }
                    }
            }
        }
    }

    private void reloadAllColumns() {
        List<String> columnNames = new ArrayList<>(currentBundle.getFileMap().keySet());
        sortFields(columnNames);
        List<String> searchOptions = new ArrayList<>();
        searchOptions.add("code");
        searchOptions.addAll(columnNames);
        searchResourcesMap = Collections.emptyMap();
        searchOptionsBox.getItems().clear();
        searchOptionsBox.setItems(FXCollections.observableArrayList(searchOptions));
        searchOptionsBox.getSelectionModel().select(searchOption);
    }

    private void setSelectedOption(String searchString) {
        if (searchResourcesMap.get(searchOption)!=null&&!searchResourcesMap.get(searchOption).isEmpty()) {
            selectOption(searchString, searchOption);
        } else {
            String firstMatch = searchResourcesMap.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).map(entry -> entry.getKey()).findFirst().orElse("");
            if (!firstMatch.equals("")) {
                selectOption(searchString, firstMatch);
            }
        }
    }

    private void selectOption(String queryString, String searchOption) {
        searchOptionsBox.getSelectionModel().select(searchOption);
        if (!queryString.matches("( +)") && !queryString.equals("")) {
            searchOptionsBox.show();
        }
        parserTable.getItems().add(new Resource(""));
    }

    private void loadResourcesMap(String queryString, String[] fieldsArray,Bundle bundle) throws ParseException, IOException {
        searchResourcesMap = resourceIndexService.searchIndex(bundle.getName(), queryString, fieldsArray,"code");
        if(!searchResourcesMap.isEmpty()) {
            Iterator<String> iterator = searchResourcesMap.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (searchResourcesMap.get(key).isEmpty()) {
                    searchResourcesMap.remove(key);
                }
            }
            List<String> options = new ArrayList<>(searchResourcesMap.keySet());
            sortFields(options);
            searchOptionsBox.getItems().clear();
            searchOptionsBox.setItems(FXCollections.observableArrayList(options));
        }
    }

    @FXML private void deleteBundle() throws IOException, ConfigurationException {
        if(currentBundle!=null) {
            Bundle bundle = (Bundle) bundleBox.getSelectionModel().getSelectedItem();
            try {
                resourceIndexService.deleteStore(bundle.getName());
                bundleService.deleteBundle(bundle);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (bundle.getName().equals(currentBundle.getName())) {
                clearParseTable();
            }
            bundles.remove(bundle);
            bundleBox.setItems(bundles);
            if (!bundles.isEmpty()) {
                Bundle initialBundle = bundles.get(0);
                setBundle(initialBundle);
                bundleBox.getSelectionModel().select(bundles.indexOf(initialBundle));
            }
        }
    }
}
