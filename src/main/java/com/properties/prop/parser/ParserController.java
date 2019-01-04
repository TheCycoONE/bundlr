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
    private ObservableList<Bundle> bundles;
    private TableView parserTable;

    @Autowired
    private FileService fileService;

    @Autowired
    private ResourceIndexService resourceIndexService;

    @Autowired
    private BundleService bundleService;

    @FXML
    public void initialize() throws IOException, ConfigurationException {
        parserTable=new TableView<Resource>();
        parserTable.getColumns().add(new TableColumn<>("code"));
        tablePane.getChildren().add(parserTable);
        parserTable.prefWidthProperty().bind(tablePane.widthProperty());
        parserTable.prefHeightProperty().bind(tablePane.heightProperty());
        parserTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
            resourceIndexService.loadStores(bundles.stream().map(Bundle::getName).collect(Collectors.toList()));
            updateIndexes();
        } catch (IOException e) {
            bundles = FXCollections.observableArrayList();
        }
        bundleBox.getItems().addAll(bundles);
        if(!bundles.isEmpty()){
            changeBundle(bundles.get(0));
        }
        bundleBox.getSelectionModel().selectedItemProperty().addListener((ChangeListener<Bundle>) (observable, oldValue, newValue) -> {
            try {
                if(newValue!=null) {
                    bundleSearchField.setText(newValue.getName());
                    bundleSearchField.positionCaret(newValue.getName().length());
                    changeBundle(newValue);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        });
        bundleBox.getSelectionModel().select(0);
        bundleSearchField.setOnKeyPressed(event -> {
            triggerFilterBundles(event);
        });
        searchBar.setOnKeyPressed(event -> {
            triggerSearchFile(event);
        });
        searchResourcesBtn.setOnKeyPressed(event -> {
            triggerSearchFile(event);
        });
        deleteBundleBtn.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER){
                deleteBundle();
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
                }
            }
        });
        bundleSearchBtn.setOnKeyPressed(event -> {
            triggerFilterBundles(event);
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
        parserTable.setOnKeyPressed(event -> {
            if(event.getCode()==KeyCode.DELETE){
                if(currentBundle!=null) {
                    List<Resource> resources = (List<Resource>) parserTable.getSelectionModel().getSelectedItems();
                    List<Resource> safeResources = resources.stream().filter(Predicate.not(resource -> resource.getCode().equals(""))).collect(Collectors.toList());
                    List<String> codes = safeResources.stream().map(Resource::getCode).collect(Collectors.toList());
                    Collection<String> filePaths = currentBundle.getFileMap().values();
                    try {
                        resourceIndexService.deleteDocuments(currentBundle.getName(), safeResources);
                        fileService.removeFileEntries(filePaths, codes);
                        parserTable.getItems().removeAll(safeResources);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ConfigurationException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
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

    private void triggerFilterBundles(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            try {
                filterBundles();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void changeBundle(Bundle bundle) throws IOException, ConfigurationException {
        currentBundle=bundle;
        if(bundle.getFileMap().isEmpty()) {
            File file = new File(bundle.getPath());
            updateStoreUI(file, bundle);
        }else {
            changeColumnNames(bundle.getFileMap());
            loadData(bundle.getName());
        }
        bundleBox.getSelectionModel().select(bundles.indexOf(bundle));
        bundleSearchField.setText(bundle.getName());
        bundleSearchField.positionCaret(bundle.getName().length());
    }
    private void updateIndexes() throws IOException, ConfigurationException {
        for(Bundle bundle : bundles){
            File file=new File(bundle.getPath());
            File[] fileArray=file.listFiles();
            if(fileArray!=null) {
                List<File> files = Arrays.asList(fileArray);
                resources = fileService.loadRowData(files);
                Map<String,String> fileMap=new LinkedHashMap<>();
                for(File currentFile : files){
                    fileMap.put(FilenameUtils.getBaseName(currentFile.getName()),currentFile.getPath());
                }
                bundle.setFileMap(fileMap);
                resourceIndexService.createLanguageBasedAnalyzer(bundle.getName(),fileMap.keySet());
                resourceIndexService.addDocuments(bundle.getName(), resources);
            }
        }
    }

    private void updateStoreUI(File file,Bundle bundle) throws IOException, ConfigurationException {
        Map<String,String> fileMap=bundle.getFileMap();
        if(fileMap.isEmpty()){
            if (file != null) {
                File[] fileArray = file.listFiles();
                if (fileArray != null) {
                    List<File> files = Arrays.asList(fileArray);
                    try (Stream<Path> paths = Files.walk(Paths.get(file.getAbsolutePath()))) {
                        updateFileMap(fileMap, paths);
                        changeColumnNames(fileMap);
                        loadData(bundle.getName(), files);
                    }
                }
            }
        }
    }

    private void updateFileMap(Map<String, String> fileMap, Stream<Path> paths) {
        List<Path> pathList = paths
                .filter(Files::isRegularFile)
                .filter((filePath) -> FilenameUtils.getExtension(filePath.toString()).equals("properties"))
                .collect(Collectors.toList());
        for (Path path : pathList) {
            fileMap.put(FilenameUtils.getBaseName(path.toString()), path.toString());
        }
    }

    private void loadBundleDataInIndex(File file,Bundle bundle) throws IOException, ConfigurationException {
        Map<String,String> fileMap=bundle.getFileMap();
        if(fileMap.isEmpty()){
            if (file != null) {
                File[] fileArray = file.listFiles();
                if (fileArray != null) {
                    List<File> files = Arrays.asList(fileArray);
                    try (Stream<Path> paths = Files.walk(Paths.get(file.getAbsolutePath()))) {
                        updateFileMap(fileMap, paths);
                        addResourcesToStore(bundle.getName(),files);
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
        List<String> fields = new ArrayList<>();
        fields.add("code");
        fields.addAll(currentBundle.getFileMap().keySet());
        return fields.toArray(String[]::new);
    }

    private void loadData(String storeName,List<File> files) throws IOException, ConfigurationException {
        if(resources!=null) {
            resources.clear();
        }
        if(!resourceIndexService.storeExists(storeName)){
            resourceIndexService.createStore(storeName);
            resourceIndexService.createLanguageBasedAnalyzer(storeName,currentBundle.getFileMap().keySet());
            resources=fileService.loadRowData(files);
            resourceIndexService.addDocuments(storeName,resources);
        }else {
            resourceIndexService.createLanguageBasedAnalyzer(storeName,currentBundle.getFileMap().keySet());
            resources=resourceIndexService.getAllResources(storeName);
        }
        parserTable.setItems(resources);
        parserTable.getItems().add(new Resource(""));
    }
    private void addResourcesToStore(String storeName,List<File> files) throws IOException, ConfigurationException {
        if(!resourceIndexService.storeExists(storeName)){
            resourceIndexService.createStore(storeName);
            resourceIndexService.createLanguageBasedAnalyzer(storeName,currentBundle.getFileMap().keySet());
            ObservableList<Resource> resources=fileService.loadRowData(files);
            resourceIndexService.addDocuments(storeName,resources);
        }
    }
    private void loadData(String storeName) throws IOException, ConfigurationException {
        parserTable.getItems().clear();
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

    private void changeColumnNames(Map<String,String> fileMap) {
        parserTable.getColumns().clear();
        parserTable.getItems().clear();
        tablePane.getChildren().remove(parserTable);
        parserTable=new TableView<Resource>();
        parserTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setSortPolicy();
        parserTable.setEditable(true);
        tablePane.getChildren().add(parserTable);
        parserTable.prefWidthProperty().bind(tablePane.widthProperty());
        parserTable.prefHeightProperty().bind(tablePane.heightProperty());
        List<String> columnNames=fileMap.keySet().stream().collect(Collectors.toList());
        Collections.sort(columnNames, (o1, o2) -> {
            String s1=o1.substring(o1.length()-5);
            String s2=o2.substring(o2.length()-5);
            if (s1.equals(s2)) {
                return 0;
            }
            return s1.compareTo(s2);
        });
        List<String> searchOptions=new ArrayList<>();
        searchOptions.add("All columns");
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
                queryString = "*" + queryString + "*";
            }
            searchedBundles = bundleService.searchBundles(queryString);
            if (!searchedBundles.isEmpty()) {
                bundleBox.getItems().clear();
                bundles=searchedBundles;
                bundleBox.setItems(searchedBundles);
                bundleBox.getSelectionModel().select(0);
                bundleBox.show();
            }
        }
    }

    @FXML private void openDirectory() throws IOException, ConfigurationException {
        final DirectoryChooser directoryChooser=new DirectoryChooser();
        Stage stage= (Stage) anchorId.getScene().getWindow();
        File file=directoryChooser.showDialog(stage);
        if(file!=null) {
            if (isBundle(file)) {
                String storeName = FilenameUtils.getBaseName(file.getName());
                boolean bundleExists = bundles.stream().anyMatch(bundle -> bundle.getName().equals(storeName));
                if(!bundleExists) {
                    processSingleBundleDirectory(file,storeName);
                }
            } else {
                File[] subFiles = file.listFiles();
                for (File subFile : subFiles) {
                    if (isBundle(subFile)) {
                        String storeName = FilenameUtils.getBaseName(subFile.getName());
                        boolean bundleExists = bundles.stream().anyMatch(bundle -> bundle.getName().equals(storeName));
                        if(!bundleExists) {
                            processBundleDirectory(subFile,storeName);
                        }
                    }
                }
                changeBundle(bundles.get(0));
            }
        }
    }

    private boolean isBundle(File file) throws IOException {
        return Arrays.asList(file.listFiles())
                .stream() //
                .allMatch(subFile -> subFile.isFile()&& FilenameUtils.getExtension(subFile.getPath()).equals("properties")); //
    }

    private void processSingleBundleDirectory(File file,String storeName) throws IOException, ConfigurationException {
        if(file!=null) {
            setCurrentBundle(file, storeName);
            updateStoreUI(file, currentBundle);
        }
    }

    private void setCurrentBundle(File file, String storeName) throws IOException {
        currentBundle = new Bundle(storeName, file.getAbsolutePath());
        bundles.add(currentBundle);
        bundleBox.setItems(bundles);
        bundleBox.getSelectionModel().select(bundles.indexOf(currentBundle));
        bundleService.addBundle(currentBundle);
    }

    private void processBundleDirectory(File file,String storeName) throws IOException, ConfigurationException {
        if(file!=null) {
            setCurrentBundle(file, storeName);
            loadBundleDataInIndex(file,currentBundle);
        }
    }

    @FXML private void resetFilter() throws IOException {
        if(currentBundle!=null) {
            ObservableList<Bundle> searchedBundles = bundleService.loadBundles();
            if (!searchedBundles.isEmpty()) {
                bundleSearchField.clear();
                bundleBox.getItems().clear();
                bundles=searchedBundles;
                bundleBox.setItems(searchedBundles);
                bundleBox.getSelectionModel().select(0);
                bundleBox.show();
            }
        }
    }

    @FXML private void getAllResources() throws IOException {
        if(currentBundle!=null) {
            searchBar.clear();
            if (resourceIndexService.storeExists(currentBundle.getName())) {
                ObservableList<Resource> searchedResources = resourceIndexService.getAllResources(currentBundle.getName());
                if (!searchedResources.isEmpty()) {
                    parserTable.setItems(searchedResources);
                }
            }
            parserTable.getItems().add(new Resource(""));
        }
    }

    @FXML private void searchFile() throws IOException, ParseException, ConfigurationException {
        if(currentBundle!=null) {
            String searchString=searchBar.getText();
            String queryString = searchString;
            String[] fieldsArray = getFieldsArray();
            String searchOption = searchOptionsBox.getSelectionModel().getSelectedItem().toString();
            ObservableList<Resource> searchedResources;
            boolean matchFound=false;
            if (resourceIndexService.storeExists(currentBundle.getName())) {
                if(!queryString.equals("")) {
                    String wildcardQueryString = "*" + queryString + "*";
                    searchedResources = searchResources(fieldsArray, searchOption, wildcardQueryString);
                    if (!searchedResources.isEmpty()) {
                        parserTable.setItems(searchedResources);
                        matchFound = true;
                    } else {
                        searchedResources = searchResources(fieldsArray,searchOption,queryString);
                        if (!searchedResources.isEmpty()) {
                            parserTable.setItems(searchedResources);
                            matchFound = true;
                        }
                    }
                }else {
                    searchedResources = resourceIndexService.getAllResources(currentBundle.getName());
                    if(!searchedResources.isEmpty()){
                        parserTable.setItems(searchedResources);
                        matchFound = true;
                    }
                }
            }
            if(!matchFound){
                List<Bundle> otherBundles=bundles.stream().filter(Predicate.not(bundle -> bundle.getName().equals(currentBundle.getName()))).collect(Collectors.toList());
                queryString=searchString;
                for(Bundle bundle : otherBundles) {
                        String[] bundleFieldsArray =bundle.getFileMap().keySet().toArray(String[]::new);
                        if (resourceIndexService.storeExists(bundle.getName())) {
                            if(!queryString.equals("")) {
                                String wildcardQueryString = "*" + queryString + "*";
                                searchedResources = searchResources(bundleFieldsArray,searchOption,wildcardQueryString);
                                if (!searchedResources.isEmpty()) {
                                    changeBundle(bundle);
                                    parserTable.setItems(searchedResources);
                                    break;
                                } else {
                                    searchedResources = searchResources(bundleFieldsArray,searchOption,queryString);
                                    if (!searchedResources.isEmpty()) {
                                        changeBundle(bundle);
                                        parserTable.setItems(searchedResources);
                                        break;
                                    }
                                }
                            }else {
                                searchedResources = resourceIndexService.getAllResources(currentBundle.getName());
                                if(!searchedResources.isEmpty()){
                                    parserTable.setItems(searchedResources);
                                    break;
                                }
                            }
                        }
                    }
            }
            parserTable.getItems().add(new Resource(""));
        }
    }

    private ObservableList<Resource> searchResources(String[] fieldsArray, String searchOption, String queryString) throws ParseException, IOException {
        ObservableList<Resource> searchedResources;
        if(searchOption.equals("All columns")) {
            searchedResources = resourceIndexService.searchIndex(currentBundle.getName(), queryString, fieldsArray);
        }else{
            searchedResources = resourceIndexService.searchIndex(currentBundle.getName(), queryString, searchOption);
        }
        return searchedResources;
    }

    @FXML private void deleteBundle(){
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
                try {
                    Bundle initialBundle = bundles.get(0);
                    changeBundle(initialBundle);
                    bundleBox.getSelectionModel().select(bundles.indexOf(initialBundle));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ConfigurationException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
