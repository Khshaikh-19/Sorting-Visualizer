import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Random;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public class SortingVisualizer extends Application {

    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 600;

    private Pane canvas;
    private int[] array;
    private Rectangle[] bars;

    private ComboBox<String> algorithmSelect;
    private Slider sizeSlider;
    private Slider speedSlider;
    private Button generateBtn, startBtn, pauseBtn, resetBtn;

    private volatile Thread sortingThread;
    private AtomicBoolean paused = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(false);


    private final Color BAR_COLOR = Color.web("#4a90e2");
    private final Color COMPARE_COLOR = Color.web("#f5a623");
    private final Color SWAP_COLOR = Color.web("#d0021b");
    private final Color SORTED_COLOR = Color.web("#7ed321");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Sorting Algorithm Visualizer - JavaFX");

        BorderPane root = new BorderPane();

        canvas = new Pane();
        canvas.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT - 120);
        canvas.setStyle("-fx-background-color: #1e1e1e;");
        root.setCenter(canvas);

        HBox controls = buildControls();
        root.setBottom(controls);
        BorderPane.setMargin(controls, new Insets(10));

        // Initialize default array
        array = new int[50];
        generateRandomArray(array.length);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();

        drawArray();
    }

    private HBox buildControls() {
        HBox h = new HBox(10);
        h.setPadding(new Insets(12));
        h.setStyle("-fx-background-color: #f3f3f3;");

        algorithmSelect = new ComboBox<>();
        algorithmSelect.getItems().addAll("Bubble Sort", "Selection Sort", "Insertion Sort", "Merge Sort", "Quick Sort", "Heap Sort");
        algorithmSelect.getSelectionModel().select(0);

        sizeSlider = new Slider(10, 200, 50);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setMajorTickUnit(50);
        sizeSlider.setMinorTickCount(5);
        sizeSlider.setPrefWidth(220);

        speedSlider = new Slider(1, 200, 100); // higher = faster (we will invert it)
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setPrefWidth(180);

        generateBtn = new Button("Generate");
        startBtn = new Button("Start");
        pauseBtn = new Button("Pause");
        resetBtn = new Button("Reset");

        generateBtn.setOnAction(e -> {
            if (isSorting()) return;
            generateRandomArray((int) sizeSlider.getValue());
            drawArray();
        });

        startBtn.setOnAction(e -> {
            if (isSorting()) return;
            stopped.set(false);
            paused.set(false);
            startSort();
        });

        pauseBtn.setOnAction(e -> {
            if (!isSorting()) return;
            if (paused.get()) {
                paused.set(false);
                pauseBtn.setText("Pause");
            } else {
                paused.set(true);
                pauseBtn.setText("Resume");
            }
        });

        resetBtn.setOnAction(e -> {
            stopSortingThread();
            generateRandomArray((int) sizeSlider.getValue());
            drawArray();
            paused.set(false);
            pauseBtn.setText("Pause");
        });

        Label sizeLabel = new Label("Size:");
        Label speedLabel = new Label("Speed:");

        h.getChildren().addAll(new Label("Algorithm:"), algorithmSelect,
                sizeLabel, sizeSlider,
                speedLabel, speedSlider,
                generateBtn, startBtn, pauseBtn, resetBtn);

        return h;
    }

    private boolean isSorting() {
        return sortingThread != null && sortingThread.isAlive();
    }

    private void startSort() {
        sortingThread = new Thread(() -> {
            try {
                String alg = algorithmSelect.getValue();
                int delay = computeDelayFromSpeed((int) speedSlider.getValue());

                switch (alg) {
                    case "Bubble Sort":
                        bubbleSort(array, delay);
                        break;
                    case "Selection Sort":
                        selectionSort(array, delay);
                        break;
                    case "Insertion Sort":
                        insertionSort(array, delay);
                        break;
                    case "Merge Sort":
                        mergeSort(array, 0, array.length - 1, delay);
                        markAllSorted();
                        break;
                    case "Quick Sort":
                        quickSort(array, 0, array.length - 1, delay);
                        markAllSorted();
                        break;
                    case "Heap Sort":
                        heapSort(array, delay);
                        break;
                }
            } catch (InterruptedException ex) {

            }
        });
        sortingThread.setDaemon(true);
        sortingThread.start();
    }

    private void stopSortingThread() {
        stopped.set(true);
        if (sortingThread != null && sortingThread.isAlive()) {
            sortingThread.interrupt();
            try { sortingThread.join(50); } catch (InterruptedException ignored) {}
        }
    }

    private int computeDelayFromSpeed(int speedValue) {

        double fraction = (200.0 - speedValue) / 199.0;
        int delay = (int) Math.round(1 + fraction * 499);
        return delay;
    }

    private void generateRandomArray(int size) {
        Random r = new Random();
        array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = r.nextInt(500) + 5;
        }
    }

    private void drawArray() {
        Platform.runLater(() -> {
            canvas.getChildren().clear();
            int n = array.length;
            bars = new Rectangle[n];
            double width = canvas.getWidth();
            if (width == 0) width = WINDOW_WIDTH;
            double barWidth = Math.max(2, width / n - 2);
            double maxVal = 0;
            for (int v : array) if (v > maxVal) maxVal = v;

            for (int i = 0; i < n; i++) {
                double height = (array[i] / maxVal) * (canvas.getHeight() - 20);
                Rectangle r = new Rectangle(barWidth, height);
                r.setFill(BAR_COLOR);
                r.setX(i * (barWidth + 2));
                r.setY(canvas.getHeight() - height);
                bars[i] = r;
                canvas.getChildren().add(r);
            }
        });
    }

    private void visualizeCompare(int i, int j, int delay) throws InterruptedException {
        highlight(i, j, COMPARE_COLOR);
        waitWhilePaused(delay);
        unhighlight(i, j);
    }

    private void visualizeSwap(int i, int j, int delay) throws InterruptedException {
        highlight(i, j, SWAP_COLOR);
        waitWhilePaused(delay);
        swapValues(i, j);
        updateBarHeights(i, j);
        unhighlight(i, j);
    }

    private void markSorted(int idx) {
        Platform.runLater(() -> {
            if (idx >= 0 && idx < bars.length) bars[idx].setFill(SORTED_COLOR);
        });
    }

    private void markAllSorted() {
        for (int i = 0; i < array.length; i++) {
            final int idx = i;
            Platform.runLater(() -> {
                if (idx >= 0 && idx < bars.length) bars[idx].setFill(SORTED_COLOR);
            });
            try {
                Thread.sleep(4);
            } catch (InterruptedException ignored) {}
        }
    }

    private void highlight(int i, int j, Color color) {
        Platform.runLater(() -> {
            if (i >= 0 && i < bars.length) bars[i].setFill(color);
            if (j >= 0 && j < bars.length) bars[j].setFill(color);
        });
    }

    private void unhighlight(int i, int j) {
        Platform.runLater(() -> {
            if (i >= 0 && i < bars.length) bars[i].setFill(BAR_COLOR);
            if (j >= 0 && j < bars.length) bars[j].setFill(BAR_COLOR);
        });
    }

    private void setBarColor(int idx, Color color) {
        Platform.runLater(() -> {
            if (idx >= 0 && idx < bars.length) bars[idx].setFill(color);
        });
    }

    private void updateBarHeights(int i, int j) {
        Platform.runLater(() -> {
            if (bars == null) return;
            double maxVal = 0;
            for (int v : array) if (v > maxVal) maxVal = v;
            double canvasH = canvas.getHeight();
            double width = canvas.getWidth();
            double barWidth = Math.max(2, width / array.length - 2);

            for (int k = Math.min(i, j); k <= Math.max(i, j); k++) {
                double height = (array[k] / maxVal) * (canvasH - 20);
                Rectangle r = bars[k];
                r.setHeight(height);
                r.setY(canvasH - height);
                r.setWidth(barWidth);
                r.setX(k * (barWidth + 2));
            }
        });
    }

    private void swapValues(int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    private void waitWhilePaused(int delay) throws InterruptedException {

        if (stopped.get()) throw new InterruptedException();

        int waited = 0;
        while (paused.get()) {
            Thread.sleep(50);
            if (stopped.get()) throw new InterruptedException();
        }
        Thread.sleep(delay);
    }



    private void bubbleSort(int[] arr, int delay) throws InterruptedException {
        int n = arr.length;
        boolean swapped;
        for (int i = 0; i < n - 1; i++) {
            swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                visualizeCompare(j, j + 1, delay);
                if (arr[j] > arr[j + 1]) {
                    visualizeSwap(j, j + 1, delay);
                    swapped = true;
                }
                if (stopped.get()) throw new InterruptedException();
            }
            markSorted(n - 1 - i);
            if (!swapped) break;
        }
        markAllSorted();
    }

    private void selectionSort(int[] arr, int delay) throws InterruptedException {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                visualizeCompare(minIdx, j, delay);
                if (arr[j] < arr[minIdx]) minIdx = j;
                if (stopped.get()) throw new InterruptedException();
            }
            if (minIdx != i) {
                visualizeSwap(i, minIdx, delay);
            }
            markSorted(i);
        }
        markAllSorted();
    }

    private void insertionSort(int[] arr, int delay) throws InterruptedException {
        for (int i = 1; i < arr.length; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= 0) {
                visualizeCompare(j, j + 1, delay);
                if (arr[j] > key) {
                    // shift arr[j] to arr[j+1]
                    arr[j + 1] = arr[j];
                    updateBarHeights(j, j + 1);
                    setBarColor(j + 1, SWAP_COLOR);
                    waitWhilePaused(delay);
                    setBarColor(j + 1, BAR_COLOR);
                    j--;
                } else break;
                if (stopped.get()) throw new InterruptedException();
            }
            arr[j + 1] = key;
            updateBarHeights(j + 1, i); // update impacted area
            if (stopped.get()) throw new InterruptedException();
            markSorted(i);
        }
        markAllSorted();
    }

    private void mergeSort(int[] arr, int left, int right, int delay) throws InterruptedException {
        if (left >= right) return;
        int mid = left + (right - left) / 2;
        mergeSort(arr, left, mid, delay);
        mergeSort(arr, mid + 1, right, delay);
        merge(arr, left, mid, right, delay);
    }

    private void merge(int[] arr, int l, int m, int r, int delay) throws InterruptedException {
        int n1 = m - l + 1;
        int n2 = r - m;
        int[] L = new int[n1];
        int[] R = new int[n2];
        System.arraycopy(arr, l, L, 0, n1);
        System.arraycopy(arr, m + 1, R, 0, n2);
        int i = 0, j = 0, k = l;
        while (i < n1 && j < n2) {
            visualizeCompare(l + i, m + 1 + j, delay);
            if (L[i] <= R[j]) {
                arr[k] = L[i++];
            } else {
                arr[k] = R[j++];
            }
            final int idx = k;
            Platform.runLater(() -> updateSingleBar(idx));
            waitWhilePaused(delay);
            k++;
            if (stopped.get()) throw new InterruptedException();
        }
        while (i < n1) {
            arr[k] = L[i++];
            final int idx = k;
            Platform.runLater(() -> updateSingleBar(idx));
            waitWhilePaused(delay);
            k++;
        }
        while (j < n2) {
            arr[k] = R[j++];
            final int idx = k;
            Platform.runLater(() -> updateSingleBar(idx));
            waitWhilePaused(delay);
            k++;
        }
    }

    private void updateSingleBar(int idx) {
        if (bars == null || idx < 0 || idx >= bars.length) return;
        double maxVal = 0;
        for (int v : array) if (v > maxVal) maxVal = v;
        double canvasH = canvas.getHeight();
        double height = (array[idx] / maxVal) * (canvasH - 20);
        Rectangle r = bars[idx];
        r.setHeight(height);
        r.setY(canvasH - height);
    }

    private void quickSort(int[] arr, int low, int high, int delay) throws InterruptedException {
        if (low < high) {
            int p = partition(arr, low, high, delay);
            quickSort(arr, low, p - 1, delay);
            quickSort(arr, p + 1, high, delay);
        }
    }

    private int partition(int[] arr, int low, int high, int delay) throws InterruptedException {
        int pivot = arr[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            visualizeCompare(j, high, delay);
            if (arr[j] < pivot) {
                i++;
                visualizeSwap(i, j, delay);
            }
            if (stopped.get()) throw new InterruptedException();
        }
        visualizeSwap(i + 1, high, delay);
        return i + 1;
    }

    private void heapSort(int[] arr, int delay) throws InterruptedException {
        int n = arr.length;

        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i, delay);
            if (stopped.get()) throw new InterruptedException();
        }

        for (int i = n - 1; i >= 0; i--) {
            visualizeSwap(0, i, delay);
            heapify(arr, i, 0, delay);
            markSorted(i);
            if (stopped.get()) throw new InterruptedException();
        }
        markAllSorted();
    }

    private void heapify(int[] arr, int n, int i, int delay) throws InterruptedException {
        int largest = i;
        int l = 2 * i + 1;
        int r = 2 * i + 2;
        if (l < n) {
            visualizeCompare(l, largest, delay);
            if (arr[l] > arr[largest]) largest = l;
        }
        if (r < n) {
            visualizeCompare(r, largest, delay);
            if (arr[r] > arr[largest]) largest = r;
        }
        if (largest != i) {
            visualizeSwap(i, largest, delay);
            heapify(arr, n, largest, delay);
        }
    }


    @Override
    public void stop() {
        stopped.set(true);
        if (sortingThread != null && sortingThread.isAlive()) {
            sortingThread.interrupt();
        }
    }
}
