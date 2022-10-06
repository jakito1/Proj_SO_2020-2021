import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class AJEvolving {
    private final List<Path> paths;
    private final List<List<Integer>> file;
    private final Random random;

    public AJEvolving(List<List<Integer>> file) {
        random = new Random();
        this.paths = new ArrayList<>();
        this.file = file;
    }

    public void startPaths(int numberPaths) {
        IntStream.range(0, numberPaths).forEach(i -> paths.add(new Path(file)));
    }

    private void sortInternal() {
        paths.sort(Comparator.comparingLong(Path::getDistance));
    }

    public Path bestPath() {
        sortInternal();
        return paths.get(0);
    }


    public void pmxCrossover(double probability, long counter, long timeTaken) {
        sortInternal();
        Path parent1 = paths.get(0);
        Path parent2 = paths.get(1);
        int[] offSpring1 = new int[parent1.getPath().size()];
        int[] offSpring2 = new int[parent1.getPath().size()];
        int[] replacement1 = new int[parent1.getPath().size() + 1];
        int[] replacement2 = new int[parent1.getPath().size() + 1];

        int n = parent1.getPath().size();
        int i, n1, m1, n2, m2;
        int swap;

        int cuttingPoint1 = random.nextInt(n);
        int cuttingPoint2 = random.nextInt(n);

        while (cuttingPoint1 == cuttingPoint2) {
            cuttingPoint2 = random.nextInt(n);
        }

        if (cuttingPoint1 > cuttingPoint2) {
            swap = cuttingPoint1;
            cuttingPoint1 = cuttingPoint2;
            cuttingPoint2 = swap;
        }

        for (i = 0; i < n + 1; i++) {
            replacement1[i] = -1;
            replacement2[i] = -1;
        }
        for (i = cuttingPoint1; i <= cuttingPoint2; i++) {
            offSpring1[i] = parent2.getPath().get(i);
            offSpring2[i] = parent1.getPath().get(i);
            replacement1[parent2.getPath().get(i)] = parent1.getPath().get(i);
            replacement2[parent1.getPath().get(i)] = parent2.getPath().get(i);
        }

        for (i = 0; i < n; i++) {
            if ((i < cuttingPoint1) || (i > cuttingPoint2)) {
                n1 = parent1.getPath().get(i);
                m1 = replacement1[n1];
                n2 = parent2.getPath().get(i);
                m2 = replacement2[n2];

                while (m1 != -1) {
                    n1 = m1;
                    m1 = replacement1[m1];
                }

                while (m2 != -1) {
                    n2 = m2;
                    m2 = replacement2[m2];
                }
                offSpring1[i] = n1;
                offSpring2[i] = n2;
            }
        }

        List<Integer> offSpring1List = Arrays.stream(offSpring1).boxed().collect(Collectors.toList());
        List<Integer> offSpring2List = Arrays.stream(offSpring2).boxed().collect(Collectors.toList());

        if (probability * 100 <= random.nextInt(100)) {
            Path.mutate(offSpring1List);
            Path.mutate(offSpring2List);
        }

        paths.add(new Path(file, offSpring1List, counter, timeTaken));
        paths.add(new Path(file, offSpring2List, counter, timeTaken));
        sortInternal();
        paths.remove(paths.size() - 1);
        paths.remove(paths.size() - 2);
    }

}
