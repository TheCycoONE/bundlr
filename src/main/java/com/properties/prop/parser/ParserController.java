package com.properties.prop.parser;

import com.properties.prop.parser.model.Bundle;
import com.properties.prop.parser.model.Resource;
import com.properties.prop.parser.model.Tuple;
import com.properties.prop.parser.service.BundleService;
import com.properties.prop.parser.service.FileService;
import com.properties.prop.parser.service.ResourceIndexService;
import com.properties.prop.widget.EditCell;
import javafx.application.Platform;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
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
    private ExecutorService bundleWatchersExecutor;
    private ExecutorService folderWatcherExecutor;
    private Bundle currentBundle;
    private ObservableList<Bundle> bundles=FXCollections.synchronizedObservableList(FXCollections.observableArrayList(new CopyOnWriteArrayList<>()));
    private TableView parserTable;
    private Map<String,ObservableList<Resource>> searchResourcesMap;
    private List<File> bundleDirectories;
    private TreeSet<Bundle> sortedBundles;
    private String searchOption;
    private ChangeListener<Bundle> bundleChangeListener;
    private volatile boolean internalChange = false;
    private boolean matchFound;
    private ObservableList<Resource> unsortedResources;

    @Autowired
    private FileService fileService;

    @Autowired
    private ResourceIndexService resourceIndexService;

    @Autowired
    private BundleService bundleService;

    @FXML
    private void initialize() throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        parserTable=new TableView<Resource>();
        parserTable.getColumns().add(new TableColumn<>("code"));
        tablePane.getChildren().add(parserTable);
        parserTable.prefWidthProperty().bind(tablePane.widthProperty());
        parserTable.prefHeightProperty().bind(tablePane.heightProperty());
        parserTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        searchResourcesMap=Collections.emptyMap();
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
                            setTooltip(null);
                        } else {
                            Tooltip tooltip=new Tooltip(item.getPath());
                            setText(item.getName());
                            setTooltip(tooltip);
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
        } catch (IOException ex) {
            bundles = FXCollections.observableArrayList();
        }

        bundleChangeListener=(observable, oldValue, newValue) -> {
            if(newValue!=null){
                try {
                    changeBundle(newValue);
                } catch (IOException | ExecutionException | ConfigurationException e) {
                    e.printStackTrace();
                } catch (InterruptedException ignored){

                }
            }
        };
        bundleBox.getSelectionModel().selectedItemProperty().addListener(bundleChangeListener);
        searchOptionsBox.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            if(newValue!=null){
                searchOption=newValue;
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
            bundleDirectories = bundles.stream().map(bundle -> Path.of(bundle.getPath()).getParent().toFile()).distinct().collect(Collectors.toList());
            for (File file : bundleDirectories) {
                    try {
                        processDirectory(file,false);
                    } catch (IOException | ConfigurationException | ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException ignored) {

                    }
            }
            updateIndexes();
            FXCollections.sort(bundles, Comparator.comparing(Bundle::getName));
            bundleBox.setItems(bundles);
            setBundle(bundles.get(0));
            loadBundleWatchers();
            loadFolderWatchers();
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
                } catch (IOException | ExecutionException | ConfigurationException e) {
                    e.printStackTrace();
                }catch (InterruptedException ignored){

                }
            }
        });
        openFolderBtn.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER){
                try {
                    openDirectory();
                } catch (IOException | ConfigurationException | InterruptedException | ExecutionException e) {
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
            } catch (IOException | ConfigurationException | ParseException e) {
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
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void changeBundle(Bundle bundle) throws IOException, ConfigurationException, ExecutionException, InterruptedException {
            currentBundle = bundle;
            searchResourcesMap=Collections.emptyMap();
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
        searchOption = searchResourcesMap.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).map(entry -> entry.getKey()).findFirst().orElse("");
        specialLoadData(bundle.getName());
        bundleBox.getSelectionModel().selectedItemProperty().removeListener(bundleChangeListener);
        bundleBox.getSelectionModel().select(bundle);
        bundleBox.getSelectionModel().selectedItemProperty().addListener(bundleChangeListener);
    }
    private void loadFolderWatchers() {
        bundleDirectories = bundles.stream().map(bundle -> Path.of(bundle.getPath()).getParent().toFile()).distinct().collect(Collectors.toList());
        if (folderWatcherExecutor != null) {
            folderWatcherExecutor.shutdownNow();
        }
        folderWatcherExecutor = Executors.newFixedThreadPool(bundleDirectories.size());
        for (File file : bundleDirectories) {
            Runnable runnable = () -> {
                try {
                    WatchService watchService = FileSystems.getDefault().newWatchService();
                    Path filePath = file.toPath();
                    filePath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,StandardWatchEventKinds.ENTRY_DELETE,StandardWatchEventKinds.ENTRY_CREATE);
                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (!internalChange) {
                                synchronized (this) {
                                    Path path = filePath.resolve(event.context().toString());
                                    File potentialFile = path.toFile();
                                    String pathString = path.toString();
                                    if (potentialFile.exists()) {
                                        String extension = FilenameUtils.getExtension(pathString);
                                        boolean isPropertiesFile = extension.equals("properties");
                                        boolean isFullDirectory = potentialFile.isDirectory() && Files.list(path).findAny().isPresent();
                                        boolean isExistingBundle = bundles.stream().anyMatch(bundle -> bundle.getPath().equals(pathString));
                                        if (!isExistingBundle && isFullDirectory) {
                                            File directory = path.toFile();
                                            Platform.runLater(() -> {
                                                try {
                                                    if (processDirectory(directory, false)) {
                                                        loadBundleWatchers();
                                                        loadFolderWatchers();
                                                    }
                                                    FXCollections.sort(bundles, Comparator.comparing(Bundle::getName));
                                                    changeBundle(currentBundle);
                                                } catch (IOException | ConfigurationException | ExecutionException e) {
                                                    e.printStackTrace();
                                                } catch (InterruptedException ignored) {

                                                }
                                            });
                                        } else if (isPropertiesFile) {
                                            String fileName = FilenameUtils.getBaseName(pathString);
                                            Bundle bundle = bundles.stream().filter(currentBundle -> fileName.startsWith(currentBundle.getName())).findFirst().orElse(null);
                                            if (bundle != null) {
                                                File bundleFile = new File(bundle.getPath());
                                                if (bundleFile.exists()) {
                                                    handleBundle(bundle, bundleFile);
                                                } else {
                                                    removeBundle(bundle);
                                                }
                                            } else {
                                                File directoryFile = potentialFile.getParentFile();
                                                if (directoryFile.exists()) {
                                                    handleSinglePropertiesFile(potentialFile);
                                                }
                                            }
                                        }
                                    } else {
                                        String fileName = FilenameUtils.getBaseName(pathString);
                                        Bundle bundle = bundles.stream().filter(currentBundle -> fileName.startsWith(currentBundle.getName())).findFirst().orElse(null);
                                        workWithBundle(potentialFile, bundle);
                                    }
                                }
                            }
                        }
                        key.reset();
                        Platform.runLater(() ->{
                            releaseFileWatcher();
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException ignored) {

                }
            };
            folderWatcherExecutor.execute(runnable);
        }
    }

    private void workWithBundle(File potentialFile, Bundle bundle) throws IOException {
        File directoryFile = potentialFile.getParentFile();
        if (directoryFile.exists()) {
            if(bundle!=null){
                File bundleFile = new File(bundle.getPath());
                handleBundle(bundle,bundleFile);
            }else {
                handleSinglePropertiesFile(potentialFile);
            }
        }else {
            if (bundle != null) {
                removeBundle(bundle);
            }
        }
    }

    private synchronized void handleSinglePropertiesFile(File potentialFile) {
        Platform.runLater(() -> {
            try {
                processSingleFileBundle(potentialFile);
            } catch (IOException | ExecutionException | ConfigurationException e) {
                e.printStackTrace();
            } catch (InterruptedException ignored) {
            }
        });
    }

    private synchronized void handleBundle(Bundle bundle, File file) throws IOException {
        File[] fileArray = file.listFiles();
        if (fileArray != null) {
            List<File> files = Arrays.stream(fileArray) //
                    .filter(subFile -> FilenameUtils.getBaseName(subFile.getName()).startsWith(bundle.getName())) //
                    .collect(Collectors.toList());
            Map<String, String> fileMap = new LinkedHashMap<>();
            for (File currentFile : files) {
                fileMap.put(FilenameUtils.getBaseName(currentFile.getName()), currentFile.getPath());
            }
            bundle.setFileMap(fileMap);
            Platform.runLater(() -> {
                parserTable.setEditable(false);
            });
            if(!fileMap.isEmpty()) {
                Platform.runLater(()->{
                    try {
                        updateBundleIndex(bundle, files, fileMap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                if (currentBundle.getName().equals(bundle.getName())) {
                    Platform.runLater(() -> {
                        try {
                                changeBundle(bundle);
                                parserTable.setEditable(true);
                                synchronized (this) {
                                    updateLastModifiedTime(bundle.getPath());
                                }
                        } catch (IOException | ExecutionException | ConfigurationException e) {
                            e.printStackTrace();
                        } catch (InterruptedException ignored) {

                        }
                    });
                }
            }else{
                removeBundle(bundle);
            }
        }
    }

    private void updateIndexes() throws IOException, ExecutionException, InterruptedException {
        Iterator<Bundle> bundleIterator = bundles.listIterator();
        while (bundleIterator.hasNext()) {
            Bundle bundle = bundleIterator.next();
            Path path = Paths.get(bundle.getPath());
            if (Files.notExists(path)) {
                resourceIndexService.deleteStore(bundle.getName());
                bundleService.deleteBundle(bundle);
                bundleIterator.remove();
            }else{
                try (Stream<Path> paths = Files.walk(Path.of(bundle.getPath()))){
                    updateFileMap(bundle,paths);
                }
                if(bundle.getFileMap()==null||bundle.getFileMap().isEmpty()){
                    resourceIndexService.deleteStore(bundle.getName());
                    bundleService.deleteBundle(bundle);
                    bundleIterator.remove();
                }
            }
        }
        List<CompletableFuture<Void>> bundleFutures=new ArrayList<>();
        for(Bundle bundle : bundles){
            CompletableFuture<Void> bundleFuture=CompletableFuture.supplyAsync(() ->{
                try {
                    FileTime bundleModifiedTime=Files.getLastModifiedTime(Path.of(bundle.getPath()));
                    Map<String, String> fileMap = bundle.getFileMap();
                    boolean wasModified=false;
                    if(bundle.getLastModified()!=bundleModifiedTime.toMillis()){
                        wasModified=true;
                    }else {
                        if (!fileMap.isEmpty()) {
                            Collection<String> filePathStrings = fileMap.values();
                            for (String filePathString : filePathStrings) {
                                Path path = Path.of(filePathString);
                                FileTime fileTime = Files.getLastModifiedTime(path);
                                if (fileTime != null) {
                                    if (bundle.getLastModified() != fileTime.toMillis()) {
                                        bundleModifiedTime = fileTime;
                                        wasModified = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if(wasModified){
                        File file = new File(bundle.getPath());
                        if (file.exists()) {
                            File[] fileArray = file.listFiles();
                            if (fileArray != null) {
                                List<File> files = Arrays.stream(fileArray) //
                                        .filter(subFile -> FilenameUtils.getBaseName(subFile.getName()).startsWith(bundle.getName())) //
                                        .collect(Collectors.toList());
                                updateBundleIndex(bundle, files, fileMap);
                                long bundleModifiedLong=bundleModifiedTime.toMillis();
                                bundle.setLastModified(bundleModifiedLong);
                                bundleService.updateBundle(bundle);
                                Path folderPath = Path.of(bundle.getPath());
                                Files.setLastModifiedTime(folderPath, bundleModifiedTime);
                                Collection<String> filePathStrings = fileMap.values();
                                for (String filePathString : filePathStrings) {
                                    Path path = Path.of(filePathString);
                                    Files.setLastModifiedTime(path, bundleModifiedTime);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            });
            bundleFutures.add(bundleFuture);
        }
        CompletableFuture<Void> mainBundleFuture=CompletableFuture.allOf(bundleFutures.toArray(CompletableFuture[]::new));
        mainBundleFuture.get();
    }
    private void loadBundleWatchers() {
        if (bundleWatchersExecutor != null) {
            bundleWatchersExecutor.shutdownNow();
        }
        bundleWatchersExecutor = Executors.newFixedThreadPool(bundles.size());
        List<Path> paths = bundles.stream().map(Bundle::getPath).distinct().map(pathString -> Path.of(pathString)).collect(Collectors.toList());
        for (Path filePath : paths) {
            Runnable runnable = () -> {
                try {
                    WatchService watchService = FileSystems.getDefault().newWatchService();
                    filePath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                            for (WatchEvent<?> event : key.pollEvents()) {
                                if (!internalChange) {
                                    synchronized (this) {
                                        Path path = filePath.resolve(event.context().toString());
                                        String pathString = path.toString();
                                        File potentialFile = path.toFile();
                                        String fileName = FilenameUtils.getBaseName(pathString);
                                        String extension = FilenameUtils.getExtension(pathString);
                                        boolean isPropertiesFile = extension.equals("properties");
                                        boolean isFullDirectory = potentialFile.isDirectory() && Files.list(path).findAny().isPresent();
                                        Bundle bundle = bundles.stream().filter(currentBundle -> fileName.startsWith(currentBundle.getName())).findFirst().orElse(null);
                                        if (!isPropertiesFile && isFullDirectory) {
                                            Platform.runLater(() -> {
                                                try {
                                                    if (processDirectory(potentialFile, false)) {
                                                        loadBundleWatchers();
                                                        loadFolderWatchers();
                                                    }
                                                } catch (IOException | ConfigurationException | ExecutionException e) {
                                                    e.printStackTrace();
                                                } catch (InterruptedException ignored) {

                                                }
                                                setBundle(currentBundle);
                                            });
                                        } else if (isPropertiesFile) {
                                            workWithBundle(potentialFile, bundle);
                                        }
                                    }
                                }
                            }
                        key.reset();
                        if(internalChange) {
                            Platform.runLater(() -> {
                                releaseFileWatcher();
                            });
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException ignored) {

                }
            };
            bundleWatchersExecutor.execute(runnable);
        }
    }

    private synchronized void removeBundle(Bundle bundle) throws IOException {
        bundleService.deleteBundle(bundle);
        resourceIndexService.deleteStore(bundle.getName());
        Platform.runLater(() -> {
            synchronized (this) {
                bundles.remove(bundle);
            }
        });
        if (currentBundle.getName().equals(bundle.getName())) {
            Platform.runLater(() -> {
                    if (bundles != null && !bundles.isEmpty()) {
                        try {
                            changeBundle(bundles.get(0));
                        } catch (IOException | ExecutionException | ConfigurationException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {

                        }
                    }
            });
        }
    }

    private void lockFileWatcher() {
        internalChange = true;
    }

    private synchronized void updateBundleIndex(Bundle bundle, List<File> files, Map<String, String> fileMap) throws IOException {
        resourceIndexService.createLanguageBasedAnalyzer(bundle.getName(), fileMap.keySet());
        List<Resource> resources = null;
        try {
            resources = fileService.loadRowData(files);
        } catch (ConfigurationException | ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException ignored) {
            ignored.printStackTrace();
        }
        if(!resourceIndexService.storeExists(bundle.getName())){
            resourceIndexService.createStore(bundle.getName());
        }
        try {
            resourceIndexService.reloadDocuments(bundle.getName(), resources);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdownWatchers(){
        if(bundleWatchersExecutor !=null){
            bundleWatchersExecutor.shutdownNow();
        }
        if(folderWatcherExecutor != null){
            folderWatcherExecutor.shutdownNow();
        }
    }

    private void updateStoreUI(Bundle bundle) throws IOException, ConfigurationException, ExecutionException, InterruptedException {
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
                        updateFileMap(bundle,paths);
                        updateLastModifiedTime(bundle.getPath());
                        changeColumnNames(fileMap);
                        loadData(bundle.getName(), files);
                    }
                }
            }
        }
    }

    private void updateFileMap(Bundle bundle,Stream<Path> paths) throws IOException {
        Map<String,String> fileMap=bundle.getFileMap();
        List<Path> pathList = paths
                .filter(Files::isRegularFile)
                .filter(filePath -> FilenameUtils.getBaseName(filePath.toString()).startsWith(bundle.getName()))
                .filter(filePath -> FilenameUtils.getExtension(filePath.toString()).equals("properties"))
                .collect(Collectors.toList());
        for (Path path : pathList) {
            fileMap.put(FilenameUtils.getBaseName(path.toString()), path.toString());
        }
    }

    private void processBundleWithoutUI(Bundle bundle) throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        Map<String,String> fileMap=bundle.getFileMap();
        File file=new File(bundle.getPath());
        String bundleName=bundle.getName();
        if(fileMap.isEmpty()){
            File[] fileArray = file.listFiles();
            if (fileArray != null) {
                List<File> files = Arrays.stream(fileArray) //
                        .filter(subFile -> FilenameUtils.getBaseName(subFile.getName()).startsWith(bundleName)) //
                        .collect(Collectors.toList());
                processFilesForBundle(bundle, file, files);
            }
        }
    }
    private void processBundleNoUi(Bundle bundle) throws InterruptedException, ExecutionException, ConfigurationException, IOException {
        File file=new File(bundle.getPath());
        String bundleName=bundle.getName();
        File[] fileArray = file.listFiles();
        if (fileArray != null) {
            List<File> files = Arrays.stream(fileArray) //
                        .filter(subFile -> FilenameUtils.getBaseName(subFile.getName()).startsWith(bundleName)) //
                        .collect(Collectors.toList());
            if(files.size()==bundle.getFileMap().size()) {
                processFilesForBundle(bundle, file, files);
            }
        }
    }

    private void processFilesForBundle(Bundle bundle, File file, List<File> files) throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        try (Stream<Path> paths = Files.walk(Paths.get(file.getAbsolutePath()))) {
            updateFileMap(bundle, paths);
            updateLastModifiedTime(bundle.getPath());
            addResourcesToIndex(bundle, files);
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

    private void loadData(String storeName,List<File> files) throws IOException, ConfigurationException, ExecutionException, InterruptedException {
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
    private void addResourcesToIndex(Bundle bundle,List<File> files) throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        String storeName=bundle.getName();
        if(!resourceIndexService.storeExists(storeName)){
            resourceIndexService.createStore(storeName);
            resourceIndexService.createLanguageBasedAnalyzer(storeName,bundle.getFileMap().keySet());
        }
        List<Resource> resources=fileService.loadRowData(files);
        resourceIndexService.reloadDocuments(storeName,resources);
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
    private void specialLoadData(String storeName) {
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
        parserTable.getSelectionModel().setCellSelectionEnabled(true);
        List<String> columnNames= new ArrayList<>(fileMap.keySet());
        sortFields(columnNames);
        if(searchResourcesMap.isEmpty()) {
            List<String> searchOptions = new ArrayList<>();
            searchOptions.add("code");
            searchOption = "code";
            searchOptions.addAll(columnNames);
            searchOptionsBox.setItems(FXCollections.observableArrayList(searchOptions));
            searchOptionsBox.getSelectionModel().select(0);
        }
        List<TableColumn> tableColumns=columnNames.stream().map(TableColumn::new).collect(Collectors.toList());
        TableColumn codeColumn=new TableColumn("code");
        codeColumn.setCellValueFactory(new PropertyValueFactory<Resource,String>("code"));

        codeColumn.setCellFactory(EditCell.forTableColumn());
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
                                        Map<String,String> bundleFileMap=currentBundle.getFileMap();
                                        for (String key : bundleFileMap.keySet()) {
                                            tuples.add(new Tuple(bundleFileMap.get(key), resource.getPropertyValue(key)));
                                        }
                                        lockFileWatcher();
                                        fileService.updateKeyInFiles(tuples, oldCode, resource.getCode());
                                        updateLastModifiedTime(currentBundle.getPath());
                                    } else {
                                        parserTable.getItems().add(new Resource(""));
                                    }
                                } catch (IOException | ConfigurationException e) {
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
            tableColumn.setCellFactory(EditCell.forTableColumn());
            tableColumn.setOnEditCommit((Event event) ->{
                CellEditEvent<Resource,String> cellEditEvent=(CellEditEvent<Resource,String>) event;
                Resource resource=(cellEditEvent).getTableView().getItems().get(
                        cellEditEvent.getTablePosition().getRow());
                if(!resource.getCode().equals("")) {
                    if(!resource.getPropertyValue(tableColumn.getText()).equals(cellEditEvent.getNewValue())) {
                        resource.setProperty(tableColumn.getText(), cellEditEvent.getNewValue());
                        try {
                            resourceIndexService.updateDocument(currentBundle.getName(), resource);
                            Map<String,String> bundleFileMap=currentBundle.getFileMap();
                            lockFileWatcher();
                            fileService.saveOrUpdateProperty(bundleFileMap.get(tableColumn.getText()), resource.getCode(), resource.getPropertyValue(tableColumn.getText()));
                            updateLastModifiedTime(currentBundle.getPath());
                        } catch (IOException | ConfigurationException e) {
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
        parserTable.comparatorProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue==null&&!matchFound) {
                parserTable.setItems(unsortedResources);
            }else if(newValue==null&&matchFound){
                unsortedResources=FXCollections.observableArrayList(parserTable.getItems());
                matchFound=false;
            }else if(newValue!=null&&oldValue==null){
                unsortedResources=FXCollections.observableArrayList(parserTable.getItems());
                matchFound=false;
            }
        });
    }

    private void releaseFileWatcher() {
        internalChange = false;
    }

    private synchronized void updateLastModifiedTime(String pathString) throws IOException {
            long lastModified = Instant.now().toEpochMilli();
            List<Bundle> affectedBundles=bundles.stream().filter(bundle -> bundle.getPath().equals(pathString)).collect(Collectors.toList());
            for(Bundle bundle : affectedBundles) {
                bundle.setLastModified(lastModified);
                bundleService.updateBundle(bundle);
                FileTime bundleFileTime = FileTime.fromMillis(bundle.getLastModified());
                lockFileWatcher();
                Path filePath = Path.of(bundle.getPath());
                Files.setLastModifiedTime(filePath, bundleFileTime);
                lockFileWatcher();
                Map<String, String> fileMap = bundle.getFileMap();
                if (fileMap != null && !fileMap.isEmpty()) {
                    Collection<String> filePathStrings = bundle.getFileMap().values();
                    for (String filePathString : filePathStrings) {
                        Path path = Path.of(filePathString);
                        Files.setLastModifiedTime(path, bundleFileTime);
                    }
                }
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
        if(processDirectory(file,false)) {
            releaseFileWatcher();
            loadBundleWatchers();
            loadFolderWatchers();
        }
        FXCollections.sort(bundles, Comparator.comparing(Bundle::getName));
    }

    private boolean processDirectory(File file,boolean fromOpenDirectory) throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        boolean foundBundles=false;
        if(file!=null) {
            if (containsBundles(file)) {
                processSingleBundleDirectory(file,fromOpenDirectory);
                foundBundles=true;
            } else {
                File[] subFiles = file.listFiles();
                if(subFiles!=null) {
                    foundBundles=Arrays.stream(subFiles).anyMatch(subFile -> {
                        try {
                            return containsBundles(subFile);
                        } catch (IOException e) {
                            return false;
                        }
                    });
                    sortedBundles=new TreeSet<>(Comparator.comparing(Bundle::getName));
                    for (File subFile : subFiles) {
                            try {
                                if (containsBundles(subFile)) {
                                    processBundleDirectory(subFile);
                                }
                            } catch (IllegalStateException | ExecutionException | IOException | ConfigurationException e) {
                                e.printStackTrace();
                            } catch (InterruptedException ignored) {

                            }
                    }
                }
                if (bundles != null && !bundles.isEmpty()&&foundBundles) {
                    FXCollections.sort(bundles, Comparator.comparing(Bundle::getName));
                    if(sortedBundles!=null&&!sortedBundles.isEmpty()) {
                        setBundle(sortedBundles.first());
                    }
                }
            }
        }
        return foundBundles;
    }

    private boolean containsBundles(File file) throws IOException {
        File[] fileArray=file.listFiles();
        if(fileArray!=null) {
            return Arrays.stream(fileArray) //
                    .anyMatch(subFile -> subFile.isFile() && FilenameUtils.getExtension(subFile.getPath()).equals("properties")); //
        }else {
            return false;
        }
    }

    private void processSingleBundleDirectory(File file,boolean fromOpenDirectory) throws IOException, ConfigurationException {
        if(file!=null) {
            File[] subFiles=file.listFiles();
            if(subFiles!=null) {
                List<Bundle> fileBundles = getBundles(file, subFiles);
                autoSelectCurrentBundle(fromOpenDirectory, fileBundles);
            }
        }
    }

    private List<Bundle> getBundles(File file, File[] subFiles) throws IOException {
        Path filePath=file.toPath();
        FileTime lastModifiedFileTime=Files.getLastModifiedTime(filePath);
        long lastModified=lastModifiedFileTime.toMillis();
        List<Bundle> fileBundles = Arrays.stream(subFiles).parallel()
                .filter(subFile -> FilenameUtils.getBaseName(subFile.getPath()).matches(".*_[a-z]{2}_[A-Z]{2}"))
                .filter(Predicate.not(subFile ->  bundlesExist(getBundleName(subFile)))
                ) //
                .map(subFile -> new Bundle(getBundleName(subFile), file.getPath(),lastModified)) //
                .filter(distinctByKey(Bundle::getName))//
                .collect(Collectors.toList());
        bundles.addAll(fileBundles);
        bundleBox.setItems(bundles);
        bundleService.addBundles(fileBundles);
        return fileBundles;
    }
    private synchronized Bundle getBundle(File file) throws IOException {
        if(FilenameUtils.getBaseName(file.getPath()).matches(".*_[a-z]{2}_[A-Z]{2}")){
            Path filePath=file.toPath();
            Path directoryPath=filePath.getParent();
            File directoryFile=directoryPath.toFile();
            FileTime lastModifiedFileTime=Files.getLastModifiedTime(directoryPath);
            long lastModified=lastModifiedFileTime.toMillis();
            String fileName = getBundleName(file);
            Bundle bundle = bundles.stream().filter(currentBundle -> fileName.startsWith(currentBundle.getName())).findFirst().orElse(null);
            if(bundle==null) {
                    bundle = new Bundle(fileName, directoryFile.getPath(), lastModified);
                    bundles.add(bundle);
                    bundleBox.setItems(bundles);
                    bundleService.addBundle(bundle);
            }
            return bundle;
        }
        return null;
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
        return bundles.stream().parallel().anyMatch(bundle -> bundle.getName().equals(name));
    }

    private void processBundleDirectory(File file) throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        if(file!=null) {
            File[] subFiles=file.listFiles();
            if(subFiles!=null) {
                List<Bundle> fileBundles = getBundles(file, subFiles);
                for (Bundle bundle : fileBundles) {
                    processBundleWithoutUI(bundle);
                }
                sortedBundles.addAll(fileBundles);
            }
        }
    }
    private synchronized void processSingleFileBundle(File file) throws IOException, InterruptedException, ExecutionException, ConfigurationException {
        Bundle bundle=getBundle(file);
        if(bundle!=null){
            processBundleNoUi(bundle);
            if (bundles != null && !bundles.isEmpty()) {
                FXCollections.sort(bundles, Comparator.comparing(Bundle::getName));
                    setBundle(bundle);
            }
        }
    }

    private void autoSelectCurrentBundle(boolean fromOpenDirectory, List<Bundle> fileBundles) {
        if (bundles != null && !bundles.isEmpty()) {
            FXCollections.sort(bundles, Comparator.comparing(Bundle::getName));
            if (fileBundles != null && !fileBundles.isEmpty() && fromOpenDirectory) {
                setBundle(fileBundles.get(0));
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
                matchFound=true;
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
            matchFound=false;
            boolean foundResources=false;
            if (resourceIndexService.storeExists(currentBundle.getName())) {
                if(!searchString.matches("( +)")&&!searchString.equals("")) {
                    loadResourcesMap(searchString, fieldsArray, currentBundle);
                    foundResources = searchResourcesMap.values().stream().anyMatch(Predicate.not(list -> list.isEmpty()));
                    if (foundResources) {
                        matchFound=true;
                        setSelectedOption(searchString);
                    }
                }else{
                    matchFound=true;
                    foundResources=true;
                    loadResourcesMap(searchString, fieldsArray, currentBundle);
                    resources=resourceIndexService.getAllResources(currentBundle.getName());
                    parserTable.setItems(resources);
                    parserTable.getItems().add(new Resource(""));
                    reloadAllColumns();
                }
            }
            if(!foundResources){
                List<Bundle> otherBundles=bundles.stream().filter(Predicate.not(bundle -> bundle.getName().equals(currentBundle.getName()))).collect(Collectors.toList());
                for(Bundle bundle : otherBundles) {
                        String[] bundleFieldsArray=getFields(bundle);
                        if (resourceIndexService.storeExists(bundle.getName())) {
                            loadResourcesMap(searchString,bundleFieldsArray,bundle);
                            foundResources = searchResourcesMap.values().stream().anyMatch(Predicate.not(list -> list.isEmpty()));
                            if(foundResources){
                                matchFound=true;
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
        if (!queryString.matches("( +)") && !queryString.equals("")&&searchOptionsBox.getItems().size()>1) {
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

    @FXML private void deleteBundle() throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        if(currentBundle!=null) {
            Bundle bundle = (Bundle) bundleBox.getSelectionModel().getSelectedItem();
            if(bundle!=null) {
                try {
                    resourceIndexService.deleteStore(bundle.getName());
                    bundleService.deleteBundle(bundle);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (bundle.getName().equals(currentBundle.getName())) {
                    clearParseTable();
                }
                if (bundles != null) {
                    bundles.remove(bundle);
                    lockFileWatcher();
                    FileUtils.deleteDirectory(new File(bundle.getPath()));
                    loadBundleWatchers();
                    loadFolderWatchers();
                    bundleBox.setItems(bundles);
                }
                if (bundles != null && !bundles.isEmpty()) {
                    Bundle initialBundle = bundles.get(0);
                    setBundle(initialBundle);
                    bundleBox.getSelectionModel().select(bundles.indexOf(initialBundle));
                }
            }
        }
    }
}
