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
import javafx.scene.input.PickResult;
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
    public int boidAmount = 20;
    public double distanceValue = 30;
    public double maxVelocityValue = 2;
    @FXML
    public VBox buttons;
    @FXML
    public CheckBox showNeighborhoodCheckBox;
    @FXML
    public CheckBox showVectorsCheckBox;
    boolean isStopped = false;
    boolean isIntervalMode = false;
    boolean showNeighborhood = false;
    boolean showVectors = false;
    double neighborAmount = 5;
    double alignmentWeight = 0.5;
    double separationWeight = 0.5;
    double cohesionWeight = 0.5;
    NeighborhoodType neighborhoodType = NeighborhoodType.DISTANCE;
    Random random = new Random();
    @FXML
    ListView<String> logs;
    Translate pivot;
    Rotate yRotate;
    Rotate xRotate;
    private Frame frame;
    private final int WINDOW_WIDTH = 1620;
    private final int WINDOW_HEIGHT = 1050;

    private final int ENV_SIZE = 3000;
    private final int ENVIRONMENT_WIDTH = ENV_SIZE;
    private final int ENVIRONMENT_HEIGHT = ENV_SIZE;
    private final int ENVIRONMENTS_DEPTH = ENV_SIZE;
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
    private Slider Cohesion, Alignment, Separation, Distance, MaxVelocity, NeighborAmount, BoidAmount;
    @FXML
    private Slider rotateX, rotateY, zoom;
    @FXML
    private VBox vboxLogs;
    @FXML
    private Label logsLabel;
    @FXML
    private ListView boidsLogs;
    @FXML
    private ChoiceBox neighborhoodChoiceBox;


    private Group boidGroup;
    private Box boxEnvironment;
    private PerspectiveCamera camera;
    private Translate cameraPoint;

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
        if (showNeighborhood) {
            showNeighborhood = false;
        } else {
            showNeighborhood = true;
        }
    }

    @FXML
    public void handleShowVectors() {
        if (showVectors) {
            showVectors = false;
        } else {
            showVectors = true;
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        boidGroup = new Group();

        buildBorder();

        cameraStuff();

        listenToObstacleAdding();

        initNeighborhoodChoiceBox();

        menu.setExpandedPane(cameraSetup);


        addListenersToSliders();
        buttons.setSpacing(5);

        initBoids(boidAmount);
        initLogs();
        play();
    }

    private void cameraStuff() {
        camera = new PerspectiveCamera();

        pivot = new Translate(boxEnvironment.getTranslateX(), boxEnvironment.getTranslateY(), boxEnvironment.getTranslateZ());
        yRotate = new Rotate(-30, Rotate.Y_AXIS);
        xRotate = new Rotate(-30, Rotate.X_AXIS);
        cameraPoint = new Translate(-1000, -500, -ENVIRONMENTS_DEPTH * 2.5);

        camera.getTransforms().addAll(
                pivot,
                yRotate,
                xRotate,
                cameraPoint
        );
        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(0),
                        new KeyValue(yRotate.angleProperty(), 0)
                ),
                new KeyFrame(
                        Duration.seconds(120),
                        new KeyValue(yRotate.angleProperty(), 360)
                )
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        //timeline.play();


        boidField.setHeight(WINDOW_HEIGHT);
        boidField.setWidth(WINDOW_WIDTH);
        boidField.setCamera(camera);
        boidField.setRoot(boidGroup);
        boidField.setFill(Color.BLACK);
    }

    private void listenToObstacleAdding() {
        boidField.setOnMouseClicked(mouseEvent -> {
            PickResult pr = mouseEvent.getPickResult();
            Obstacle newObstacle = new Obstacle(
                    new Sphere(50),
                    Color.ORANGE,
                    pr.getIntersectedPoint().getX(),
                    pr.getIntersectedPoint().getY(),
                    pr.getIntersectedPoint().getZ()
            );
            boidGroup.getChildren().add(newObstacle.getShape3D());
            obstacleList.add(newObstacle);

        });
    }

    private void initNeighborhoodChoiceBox() {
        neighborhoodChoiceBox.getItems().add(NeighborhoodType.DISTANCE.toString());
        neighborhoodChoiceBox.getItems().add(NeighborhoodType.TOPOLOGY.toString());
        neighborhoodChoiceBox.setValue(neighborhoodChoiceBox.getItems().get(0));
        NeighborAmount.setDisable(true);

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

    }

    private void addListenersToSliders() {
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
        BoidAmount.valueProperty().addListener((ov, old_val, new_val) -> {
            boidAmount = new_val.intValue();

            if (boidAmount > old_val.doubleValue()) {
                initExtraBoids(boidAmount - boidsList.size());
                NeighborAmount.setMax(boidAmount);
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
    }

    private void play() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isStopped) {
                    try {
                        updateBoidsPositions(frame, neighborhoodType);
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

    public void updateBoidsPositions(Frame frame, NeighborhoodType neighborhoodType) throws Exception {

        switch (neighborhoodType) {
            case DISTANCE:
                findNeighborsByDistance(distanceValue);
                break;
            case TOPOLOGY:
                findNeighborsByAmount(neighborAmount);
                break;
        }

        for (boids.Boid boid : boidsList) {
            boid.update(showNeighborhood, showVectors, alignmentWeight, separationWeight, cohesionWeight, distanceValue, maxVelocityValue, neighborhoodType);
        }

        //updateLogs();

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

    private void updateLogs() {
        AtomicInteger counter = new AtomicInteger();
        // TODO addextraLogs
        for (Boid boid : boidsList
        ) {
            logs.getItems().set(counter.get(), boid.toString() + " neighbors: " + boid.neighborhoodList.size());
            counter.getAndIncrement();
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

    private void initLogs() {
        logs = new ListView<>();
        for (boids.Boid boid : boidsList
        ) {
            logs.getItems().add(boid.getName() +
                    "     X:" + boid.getBoidView().getTranslateX() +
                    "     Y:" + boid.getBoidView().getTranslateY());

        }
        boidsLogs.getItems().setAll(logs);
    }

    public void initBoids(int amount) {
        for (int i = 1; i <= amount; i++) {
            int random_x = random.nextInt(ENVIRONMENT_WIDTH);
            int random_y = random.nextInt(ENVIRONMENT_HEIGHT);
            int random_z = random.nextInt(ENVIRONMENTS_DEPTH);

            Boid boid = new Boid(random_x, random_y, random_z, ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT, ENVIRONMENTS_DEPTH);

            boid.setName("Boid no. " + i);
            addBoid(boid);
        }
    }

    public void initExtraBoids(int amount) {
        int n = boidsList.size();
        for (int i = boidsList.size(); i <= amount + n; i++) {
            int random_x = random.nextInt(ENVIRONMENT_WIDTH);
            int random_y = random.nextInt(ENVIRONMENT_HEIGHT);
            int random_z = random.nextInt(ENVIRONMENTS_DEPTH);

            Boid boid = new Boid(random_x, random_y, random_z, ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT, ENVIRONMENTS_DEPTH);

            boid.setName("Boid no. " + i);
            addBoid(boid);
        }
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

    private void buildBorder() {
        boxEnvironment = new Box(ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT, ENVIRONMENTS_DEPTH);
        PhongMaterial phongMaterial = new PhongMaterial();
        phongMaterial.setDiffuseColor(Color.web("#262418"));
        boxEnvironment.setMaterial(phongMaterial);

        boxEnvironment.setTranslateX(ENVIRONMENT_WIDTH / 2);
        boxEnvironment.setTranslateY(ENVIRONMENT_HEIGHT / 2);
        boxEnvironment.setTranslateZ(ENVIRONMENTS_DEPTH / 2);

        boidGroup.getChildren().add(boxEnvironment);
        boidGroup.getChildren().add(buildFrames());
    }

    private Group buildFrames() {
        Group frameLines = new Group();
        PhongMaterial frameColor = new PhongMaterial(Color.BLUE);
        double frameSize = 30;

        Box line1 = new Box(1, ENVIRONMENT_HEIGHT, frameSize);
        line1.setMaterial(frameColor);
        line1.setTranslateX(0);
        line1.setTranslateY(ENVIRONMENT_WIDTH / 2);

        Box line2 = new Box(ENVIRONMENT_WIDTH, 1, frameSize);
        line2.setMaterial(frameColor);
        line2.setTranslateX(ENVIRONMENT_WIDTH / 2);
        line2.setTranslateY(0);


        Box line3 = new Box(1, ENVIRONMENT_HEIGHT, frameSize);
        line3.setMaterial(frameColor);
        line3.setTranslateX(ENVIRONMENT_WIDTH);
        line3.setTranslateY(ENVIRONMENT_HEIGHT / 2);

        Box line4 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line4.setMaterial(frameColor);
        line4.setTranslateX(ENVIRONMENT_WIDTH / 2);
        line4.setTranslateY(ENVIRONMENT_HEIGHT);

        // in z translated
        Box line5 = new Box(1, ENVIRONMENT_HEIGHT, frameSize);
        line5.setMaterial(frameColor);
        line5.setTranslateX(0);
        line5.setTranslateY(ENVIRONMENT_WIDTH / 2);
        line5.setTranslateZ(ENVIRONMENTS_DEPTH);

        Box line6 = new Box(ENVIRONMENT_WIDTH, 1, frameSize);
        line6.setMaterial(frameColor);
        line6.setTranslateX(ENVIRONMENT_WIDTH / 2);
        line6.setTranslateY(0);
        line6.setTranslateZ(ENVIRONMENTS_DEPTH);

        Box line7 = new Box(1, ENVIRONMENT_HEIGHT, frameSize);
        line7.setMaterial(frameColor);
        line7.setTranslateX(ENVIRONMENT_WIDTH);
        line7.setTranslateY(ENVIRONMENT_HEIGHT / 2);
        line7.setTranslateZ(ENVIRONMENTS_DEPTH);

        Box line8 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line8.setMaterial(frameColor);
        line8.setTranslateX(ENVIRONMENT_WIDTH / 2);
        line8.setTranslateY(ENVIRONMENT_HEIGHT);
        line8.setTranslateZ(ENVIRONMENTS_DEPTH);

        // rotated bottom
        Box line9 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line9.setMaterial(frameColor);
        line9.setTranslateX(ENVIRONMENT_WIDTH);
        line9.setTranslateY(ENVIRONMENT_HEIGHT);
        line9.setTranslateZ(ENVIRONMENTS_DEPTH / 2);
        line9.setRotationAxis(Rotate.Y_AXIS);
        line9.setRotate(90);

        Box line10 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line10.setMaterial(frameColor);
        line10.setTranslateX(0);
        line10.setTranslateY(ENVIRONMENT_HEIGHT);
        line10.setTranslateZ(ENVIRONMENTS_DEPTH / 2);
        line10.setRotationAxis(Rotate.Y_AXIS);
        line10.setRotate(90);

        // rotated up
        Box line11 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line11.setMaterial(frameColor);
        line11.setTranslateX(0);
        line11.setTranslateY(0);
        line11.setTranslateZ(ENVIRONMENTS_DEPTH / 2);
        line11.setRotationAxis(Rotate.Y_AXIS);
        line11.setRotate(90);

        Box line12 = new Box(ENVIRONMENT_WIDTH + 1, 1, frameSize);
        line12.setMaterial(frameColor);
        line12.setTranslateX(ENVIRONMENT_WIDTH);
        line12.setTranslateY(0);
        line12.setTranslateZ(ENVIRONMENTS_DEPTH / 2);
        line12.setRotationAxis(Rotate.Y_AXIS);
        line12.setRotate(90);

        frameLines.getChildren().addAll(line1, line2, line3, line4);
        frameLines.getChildren().addAll(line5, line6, line7, line8);
        frameLines.getChildren().addAll(line9, line10);
        frameLines.getChildren().addAll(line11, line12);

        return frameLines;
    }
}
