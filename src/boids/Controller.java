package boids;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;


public class Controller implements Initializable {
    private final List<Boid> boidsList = new ArrayList<>();
    private final List<Obstacle> obstacleList = new ArrayList<>();
    private final int WINDOW_WIDTH = 1620;
    private final int WINDOW_HEIGHT = 1080;
    @FXML
    public VBox buttons;
    @FXML
    public CheckBox showNeighborhoodCheckBox, isLeaderInEnv, isPredatorInEnv;
    @FXML
    public CheckBox showVectorsCheckBox;
    public int boidAmount = 20;
    public double distanceValue = 50;
    public double maxVelocityValue = 15;
    public double minVelocityValue = 5;
    public int obstaclesAmount = 0;
    boolean isStopped = false;
    boolean isIntervalMode = false;
    boolean showNeighborhood = false;
    boolean showVectors = false;
    double neighborAmount = 5;
    double alignmentWeight = 0.5;
    double separationWeight = 0.5;
    double cohesionWeight = 0.5;
    Integer prevSelectedIndex = null;
    NeighborhoodType neighborhoodType = NeighborhoodType.DISTANCE;
    Random random = new Random();
    @FXML
    ListView<String> logs;
    Translate pivot;
    Rotate yRotate;
    Rotate xRotate;
    private Leader leader = null;
    private Predator predator = null;
    private int ENV_SIZE = 3000;
    private int ENVIRONMENT_WIDTH = ENV_SIZE;
    private int ENVIRONMENT_HEIGHT = ENV_SIZE;
    private int ENVIRONMENTS_DEPTH = ENV_SIZE;
    @FXML
    private TextField boidAmountTextField;
    @FXML
    private Button continueAnimationButton;
    @FXML
    private SubScene boidField;
    @FXML
    private Accordion menu;
    @FXML
    private TitledPane positionLogs, moveControl, flockRules, cameraSetup;
    @FXML
    private Slider Cohesion, Alignment, Separation;
    @FXML
    private Slider Distance, MaxVelocity, minVelocity, NeighborAmount, BoidAmount;
    @FXML
    private Slider rotateX, rotateY, zoom;
    @FXML
    private Slider numberOfObstacle, envSize;
    @FXML
    private VBox vboxLogs;
    @FXML
    private Label logsLabel;
    @FXML
    private ListView boidsLogs;
    @FXML
    private ChoiceBox neighborhoodChoiceBox;
    @FXML
    private CheckBox isStaticCamera, isOrbitalCamera;


    private Group boidGroup;
    private Group frameLines;
    private Box boxEnvironment;
    private Translate cameraPoint;
    private Timeline timeline;

    @FXML
    private void handleStartClick() {
        isStopped = false;
    }

    @FXML
    private void handleStopClick() {
        isStopped = true;
    }

    @FXML
    private void handleContinueClick() {
        if (isIntervalMode) {
            isIntervalMode = false;
            continueAnimationButton.setText("1s internal off");
        } else {
            isIntervalMode = true;
            continueAnimationButton.setText("1s internal on");
        }
    }

    @FXML
    private void handleZoomIn() {
        cameraPoint.setZ(cameraPoint.getZ() + 100);
    }

    @FXML
    private void handleZoomOut() {
        cameraPoint.setZ(cameraPoint.getZ() - 100);
    }


    @FXML
    public void handleShowNeighborhood() {
        showNeighborhood = !showNeighborhood;
    }

    @FXML
    public void handleShowVectors() {
        showVectors = !showVectors;
    }

    @FXML
    public void handleStaticCamera() {
        timeline.stop();
        isOrbitalCamera.setSelected(false);
        rotateY.setDisable(false);
    }

    @FXML
    public void handleOrbitalCamera() {
        timeline.play();
        isStaticCamera.setSelected(false);
        rotateY.setDisable(true);
    }

    @FXML
    public void handleLeaderCheckBox() {
        if (isLeaderInEnv.isSelected()) {
            addLeaderToEnv();
        } else {
            removeLeaderFromEnv();
        }
    }

    @FXML
    public void handlePredatorCheckBox() {
        if (isPredatorInEnv.isSelected()) {
            addPredatorToEnv();
        } else {
            removePredatorFromEnv();
        }
    }

    private void removePredatorFromEnv() {
        boidGroup.getChildren().remove(predator);
        predator.getPredatorSphere().setOpacity(0);
        predator = null;
    }

    private void addPredatorToEnv() {
        predator = new Predator(ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT, ENVIRONMENTS_DEPTH);
        boidGroup.getChildren().add(predator.getPredatorSphere());
    }


    private void addLeaderToEnv() {
        leader = new Leader(ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT, ENVIRONMENTS_DEPTH);
        boidGroup.getChildren().add(leader.getLeaderSphere());
    }

    private void removeLeaderFromEnv() {
        boidGroup.getChildren().remove(leader);
        leader.getLeaderSphere().setOpacity(0);
        leader = null;

    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Distance.setMax(ENV_SIZE / 10d);
        boidField.setHeight(WINDOW_HEIGHT);
        boidField.setWidth(WINDOW_WIDTH);

        boidGroup = new Group();

        buildEnv();
        cameraStuff();
        initNeighborhoodChoiceBox();
        menu.setExpandedPane(flockRules);

        boidField.setRoot(boidGroup);

        buttons.setSpacing(5);

        initBoids(boidAmount);
        initLogs();
        addListenersToUIElements();
        play();
    }

    private void cameraStuff() {
        PerspectiveCamera camera = new PerspectiveCamera();

        pivot = new Translate(boxEnvironment.getTranslateX(), boxEnvironment.getTranslateY(), boxEnvironment.getTranslateZ());
        yRotate = new Rotate(-15, Rotate.Y_AXIS);
        xRotate = new Rotate(-15, Rotate.X_AXIS);
        cameraPoint = new Translate(-1000, -500, -ENVIRONMENTS_DEPTH * 2.5);

        camera.getTransforms().addAll(
                pivot,
                yRotate,
                xRotate,
                cameraPoint
        );
        timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(0),
                        new KeyValue(yRotate.angleProperty(), 0)
                ),
                new KeyFrame(
                        Duration.seconds(240),
                        new KeyValue(yRotate.angleProperty(), 360)
                )
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        boidField.setCamera(camera);
    }


    private void initNeighborhoodChoiceBox() {
        neighborhoodChoiceBox.getItems().add(NeighborhoodType.DISTANCE.toString());
        neighborhoodChoiceBox.getItems().add(NeighborhoodType.TOPOLOGY.toString());
        neighborhoodChoiceBox.setValue(neighborhoodChoiceBox.getItems().get(0));
        NeighborAmount.setDisable(true);

    }

    private void addListenersToUIElements() {
        neighborhoodChoiceBox.valueProperty().addListener((ov, old_val, new_val) -> {
            neighborhoodType = NeighborhoodType.valueOf((String) new_val);
            switch (neighborhoodType) {
                case DISTANCE:
                    NeighborAmount.setDisable(true);
                    Distance.setDisable(false);
                    break;
                case TOPOLOGY:
                    NeighborAmount.setDisable(false);
                    Distance.setDisable(true);
                    break;
            }

        });
        Cohesion.valueProperty().addListener((ov, old_val, new_val) -> {
            cohesionWeight = (double) new_val;
        });
        Separation.valueProperty().addListener((ov, old_val, new_val) -> {
            separationWeight = (double) new_val;
        });
        Alignment.valueProperty().addListener((ov, old_val, new_val) -> {
            alignmentWeight = (double) new_val;
        });
        Distance.valueProperty().addListener((ov, old_val, new_val) -> {
            distanceValue = (double) new_val;
        });
        MaxVelocity.valueProperty().addListener((ov, old_val, new_val) -> {
            maxVelocityValue = (double) new_val;
        });
        minVelocity.valueProperty().addListener((ov, old_val, new_val) -> {
            minVelocityValue = (double) new_val;
        });
        BoidAmount.valueProperty().addListener((ov, old_val, new_val) -> {
            boidAmount = new_val.intValue();

            if (boidAmount > old_val.doubleValue()) {
                initExtraBoids(boidAmount - boidsList.size());
            } else {
                deleteBoidsTo(boidAmount);
            }
            NeighborAmount.setMax(boidAmount);
        });
        NeighborAmount.valueProperty().addListener((ov, old_val, new_val) -> {
            neighborAmount = (double) new_val;
            neighborAmount = new_val.intValue() >= boidAmount ? boidAmount - 1 : new_val.intValue();
        });
        rotateX.valueProperty().addListener((ov, old_val, new_val) -> {
            double rotationX = (double) new_val;
            xRotate.setAngle(rotationX);
        });
        rotateY.valueProperty().addListener((ov, old_val, new_val) -> {
            double rotationY = (double) new_val;
            yRotate.setAngle(rotationY);
        });
        zoom.valueProperty().addListener((ov, old_val, new_val) -> {
            double zoom = (double) new_val - (double) old_val;
            cameraPoint.setZ(cameraPoint.getZ() + zoom);
        });
        numberOfObstacle.valueProperty().addListener((ov, old_val, new_val) -> {
            obstaclesAmount = new_val.intValue();
            if (obstaclesAmount > old_val.intValue()) {
                addObstacles(obstaclesAmount - obstacleList.size());
            } else {
                deleteObstaclesTo(obstaclesAmount);
            }
        });
        envSize.valueProperty().addListener((ov, old_val, new_val) -> {
            ENV_SIZE = (int) (double) new_val;
            ENVIRONMENT_WIDTH = ENV_SIZE;
            ENVIRONMENT_HEIGHT = ENV_SIZE;
            ENVIRONMENTS_DEPTH = ENV_SIZE;
            boidGroup.getChildren().remove(boxEnvironment);
            boidGroup.getChildren().remove(frameLines);
            buildEnv();
            cameraStuff();
            deleteBoidsTo(0);
            deleteObstaclesTo(0);
            initExtraBoids(boidAmount);
            addObstacles(obstaclesAmount);
            if (isLeaderInEnv.isSelected())
                removeLeaderFromEnv();
                addLeaderToEnv();

            if (isPredatorInEnv.isSelected())
                removePredatorFromEnv();
                addPredatorToEnv();


        });
        logs.setOnMouseClicked(mouseEvent -> {

            int selectedIndex = logs.getSelectionModel().getSelectedIndex();
            Sphere sphere = (Sphere) boidsList.get(selectedIndex).getBoidView();
            sphere.setMaterial(new PhongMaterial(Color.GREEN));

            if (prevSelectedIndex == null)
                prevSelectedIndex = selectedIndex;

            if (selectedIndex != prevSelectedIndex) {
                Sphere spherePrev = (Sphere) boidsList.get(prevSelectedIndex).getBoidView();
                spherePrev.setMaterial(new PhongMaterial(Color.RED));
            }

            prevSelectedIndex = selectedIndex;
        });
    }

    private void deleteObstaclesTo(double obstaclesAmount) {
        while (obstacleList.size() != obstaclesAmount) {
            obstacleList.get(obstacleList.size() - 1).getShape3D().setOpacity(0);
            obstacleList.remove(obstacleList.size() - 1);
        }
    }

    private void addObstacles(double amount) {
        for (int i = 0; i < amount; i++) {
            Obstacle newObstacle = new Obstacle(
                    new Sphere(ENV_SIZE / 50),
                    Color.ORANGE,
                    random.nextInt(ENV_SIZE - 100),
                    random.nextInt(ENV_SIZE - 100),
                    random.nextInt(ENV_SIZE - 100)
            );
            boidGroup.getChildren().add(newObstacle.getShape3D());
            obstacleList.add(newObstacle);
        }
    }

    private void play() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isStopped) {
                    try {
                        updateEnv(neighborhoodType);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        timer.start();

    }

    public void updateEnv(NeighborhoodType neighborhoodType) throws Exception {

        switch (neighborhoodType) {
            case DISTANCE:
                findNeighborsByDistance(distanceValue);
                break;
            case TOPOLOGY:
                findNeighborsByAmount(neighborAmount);
                break;
        }

        for (boids.Boid boid : boidsList) {
            boid.update(showNeighborhood, showVectors, alignmentWeight, separationWeight, cohesionWeight,
                    distanceValue, maxVelocityValue, minVelocityValue, leader, predator, obstacleList);
        }

        if (leader != null)
            leader.update();

        if (predator != null)
            predator.update(boidsList);

        updateLogs();

        for (Boid boid : boidsList) {
            boid.clearNeighborhoodList();
        }

        if (isIntervalMode)
            sleep(1000);
    }

    private void findNeighborsByAmount(double amountOfNeighbor) throws Exception {
        TreeMap<Double, Boid> boidsWithDistance = new TreeMap<>();
        for (int i = 0; i < boidsList.size(); i++) {
            for (int j = 0; j < boidsList.size(); j++) {
                if (i != j) {
                    double distanceFrom = boidsList.get(i).getDistanceFrom(boidsList.get(j));
                    boidsWithDistance.put(distanceFrom, boidsList.get(i));
                }
            }
            for (int k = 0; k < amountOfNeighbor; k++) {
                try {
                    boidsList.get(i).addNeighbor(boidsWithDistance.get(boidsWithDistance.firstKey()));
                    boidsWithDistance.remove(boidsWithDistance.firstKey());
                } catch (Exception e) {
                }
            }
        }
    }

    private void findNeighborsByDistance(double distance) {

        for (int i = 0; i < boidsList.size(); i++)
            for (int j = i + 1; j < boidsList.size(); j++) {
                double distanceFrom = boidsList.get(i).getDistanceFrom(boidsList.get(j));
                if (distanceFrom <= distance) {
                    boidsList.get(i).addNeighbor(boidsList.get(j));
                    boidsList.get(j).addNeighbor(boidsList.get(i));
                }
            }
    }

    private void updateLogs() {
        AtomicInteger counter = new AtomicInteger();
        for (Boid boid : boidsList
        ) {
            logs.getItems().set(counter.get(), boid.toString() + " neighbors: " + boid.neighborhoodList.size());
            counter.getAndIncrement();
        }

    }

    private void initLogs() {
        logs = new ListView<>();
        for (boids.Boid boid : boidsList
        ) {
            logs.getItems().add(boid.getName() +
                    "     X:" + boid.getBoidView().getTranslateX() +
                    "     Y:" + boid.getBoidView().getTranslateY());

        }
        logs.prefHeightProperty().set(950);
        boidsLogs.getItems().setAll(logs);
    }

    public void initBoids(int amount) {
        for (int i = 1; i <= amount; i++) {
            addRandomBoid(i);
        }
    }

    public void initExtraBoids(int amount) {
        int n = boidsList.size();
        for (int i = boidsList.size(); i <= amount + n; i++) {
            addRandomBoid(i);
        }
        initLogs();
    }

    private void addRandomBoid(int boidIndex) {
        int random_x = random.nextInt(ENVIRONMENT_WIDTH);
        int random_y = random.nextInt(ENVIRONMENT_HEIGHT);
        int random_z = random.nextInt(ENVIRONMENTS_DEPTH);

        Boid boid = new Boid(random_x, random_y, random_z, ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT, ENVIRONMENTS_DEPTH);

        boid.setName("Boid no. " + boidIndex);
        addBoid(boid);
    }

    private void deleteBoidsTo(int boidAmount) {
        while (boidsList.size() != boidAmount) {
            boidsList.get(boidsList.size() - 1).boidSphere.setOpacity(0);
            boidsList.get(boidsList.size() - 1).neighborhoodSphere.setOpacity(0);
            boidsList.get(boidsList.size() - 1).getVelocityLine().setOpacity(0);
            boidsList.remove(boidsList.size() - 1);
        }
    }

    public void addBoid(Boid boid) {
        boidsList.add(boid);

        boidGroup.getChildren().add(boid.getNeighborhoodView());
        boidGroup.getChildren().add(boid.getBoidView());
        boidGroup.getChildren().add(boid.getVelocityLine());
    }

    private void buildEnv() {
        boxEnvironment = new Box(ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT, ENVIRONMENTS_DEPTH);
        PhongMaterial phongMaterial = new PhongMaterial();
        phongMaterial.setDiffuseColor(Color.GRAY.brighter());
        boxEnvironment.setMaterial(phongMaterial);

        boxEnvironment.setTranslateX(ENVIRONMENT_WIDTH / 2d);
        boxEnvironment.setTranslateY(ENVIRONMENT_HEIGHT / 2d);
        boxEnvironment.setTranslateZ(ENVIRONMENTS_DEPTH / 2d);

        boidGroup.getChildren().add(boxEnvironment);
        boidGroup.getChildren().add(buildFrames());
    }

    private Group buildFrames() {
        frameLines = new Group();
        PhongMaterial frameColor = new PhongMaterial(Color.BLACK.darker());
        double frameSize = ENV_SIZE / 100d;

        Box line1 = new Box(1, ENVIRONMENT_HEIGHT, frameSize);
        line1.setMaterial(frameColor);
        line1.setTranslateX(0);
        line1.setTranslateY(ENVIRONMENT_WIDTH / 2d);

        Box line2 = new Box(ENVIRONMENT_WIDTH, 1, frameSize);
        line2.setMaterial(frameColor);
        line2.setTranslateX(ENVIRONMENT_WIDTH / 2d);
        line2.setTranslateY(0);


        Box line3 = new Box(1, ENVIRONMENT_HEIGHT, frameSize);
        line3.setMaterial(frameColor);
        line3.setTranslateX(ENVIRONMENT_WIDTH);
        line3.setTranslateY(ENVIRONMENT_HEIGHT / 2d);

        Box line4 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line4.setMaterial(frameColor);
        line4.setTranslateX(ENVIRONMENT_WIDTH / 2d);
        line4.setTranslateY(ENVIRONMENT_HEIGHT);

        // in z translated
        Box line5 = new Box(1, ENVIRONMENT_HEIGHT, frameSize);
        line5.setMaterial(frameColor);
        line5.setTranslateX(0);
        line5.setTranslateY(ENVIRONMENT_WIDTH / 2d);
        line5.setTranslateZ(ENVIRONMENTS_DEPTH);

        Box line6 = new Box(ENVIRONMENT_WIDTH, 1, frameSize);
        line6.setMaterial(frameColor);
        line6.setTranslateX(ENVIRONMENT_WIDTH / 2d);
        line6.setTranslateY(0);
        line6.setTranslateZ(ENVIRONMENTS_DEPTH);

        Box line7 = new Box(1, ENVIRONMENT_HEIGHT, frameSize);
        line7.setMaterial(frameColor);
        line7.setTranslateX(ENVIRONMENT_WIDTH);
        line7.setTranslateY(ENVIRONMENT_HEIGHT / 2d);
        line7.setTranslateZ(ENVIRONMENTS_DEPTH);

        Box line8 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line8.setMaterial(frameColor);
        line8.setTranslateX(ENVIRONMENT_WIDTH / 2d);
        line8.setTranslateY(ENVIRONMENT_HEIGHT);
        line8.setTranslateZ(ENVIRONMENTS_DEPTH);

        // rotated bottom
        Box line9 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line9.setMaterial(frameColor);
        line9.setTranslateX(ENVIRONMENT_WIDTH);
        line9.setTranslateY(ENVIRONMENT_HEIGHT);
        line9.setTranslateZ(ENVIRONMENTS_DEPTH / 2d);
        line9.setRotationAxis(Rotate.Y_AXIS);
        line9.setRotate(90);

        Box line10 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line10.setMaterial(frameColor);
        line10.setTranslateX(0);
        line10.setTranslateY(ENVIRONMENT_HEIGHT);
        line10.setTranslateZ(ENVIRONMENTS_DEPTH / 2d);
        line10.setRotationAxis(Rotate.Y_AXIS);
        line10.setRotate(90);

        // rotated up
        Box line11 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line11.setMaterial(frameColor);
        line11.setTranslateX(0);
        line11.setTranslateY(0);
        line11.setTranslateZ(ENVIRONMENTS_DEPTH / 2d);
        line11.setRotationAxis(Rotate.Y_AXIS);
        line11.setRotate(90);

        Box line12 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line12.setMaterial(frameColor);
        line12.setTranslateX(ENVIRONMENT_WIDTH);
        line12.setTranslateY(0);
        line12.setTranslateZ(ENVIRONMENTS_DEPTH / 2d);
        line12.setRotationAxis(Rotate.Y_AXIS);
        line12.setRotate(90);

        frameLines.getChildren().addAll(line1, line2, line3, line4);
        frameLines.getChildren().addAll(line5, line6, line7, line8);
        frameLines.getChildren().addAll(line9, line10);
        frameLines.getChildren().addAll(line11, line12);

        return frameLines;
    }
}
