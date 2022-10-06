import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class Path {
    private static final Random rand = new Random();
    private final List<List<Integer>> file;
    private final List<Integer> path;
    private final long counter;
    private final long timeTaken;
    private long distance;

    public Path(List<List<Integer>> file) {
        this.counter = 0;
        this.timeTaken = 0;
        this.path = new ArrayList<>();
        this.file = file;
        startPath();
        randomizePath();
        distance = calculateDistance();
    }

    public Path(List<List<Integer>> file, List<Integer> path, long counter, long timeTaken) {
        this.counter = counter;
        this.timeTaken = timeTaken;
        this.path = path;
        this.file = file;
        distance = calculateDistance();
    }

    public static void mutate(List<Integer> list) {
        Collections
                .swap(list, rand.nextInt(list.size()), rand.nextInt(list.size()));
    }

    public long calculateDistance() {
        distance = 0;

        for (int i = 0; i < file.size() - 1; i++) {
            int curr = path.get(i);
            int next = path.get(i + 1);
            distance += file.get(curr).get(next);
        }

        int last = path.get(file.size() - 1);
        int first = path.get(0);
        distance += file.get(last).get(first);
        return distance;
    }

    private void startPath() {
        IntStream.range(0, file.size()).forEach(path::add);
    }

    private void randomizePath() {
        IntStream.range(0, file.size())
                .forEach(i -> Collections
                        .swap(path, rand.nextInt(file.size()), rand.nextInt(file.size())));
    }

    public long getDistance() {
        return distance;
    }

    public List<Integer> getPath() {
        return path;
    }

    public long getCounter() {
        return counter;
    }

    public long getTimeTaken() {
        return timeTaken;
    }


    @Override
    public String toString() {
        StringBuilder returnString = new StringBuilder("Caminho: ");
        for (Integer var : path) {
            returnString.append(var + 1).append(" ");
        }
        returnString.append("\nDistância: ");
        returnString.append(distance);

        return returnString.toString();
    }
}