import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class InputReader {

    public synchronized static List<List<Integer>> readFromTxt(String path) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File("Input Files\\" + path));
        List<List<Integer>> data = new ArrayList<>();
        int size = scanner.nextInt();
        for (int i = 0; i < size; i++) {
            data.add(new ArrayList<>());
        }
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                data.get(i).add(scanner.nextInt());
            }
        }
        return data;
    }

}
