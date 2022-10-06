import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AppDriver extends Thread {
    private final List<AppDriver> threads;
    private String fileName;
    private int nThreads;
    private long execTime;
    private int nPaths;
    private double mutationProb;
    private long startTime;
    private Path bestPath;
    private Path bestOfBestPath;

    public AppDriver(String[] args) {
        threads = new ArrayList<>();
        treatArgs(args);
    }

    public void begin(String[] args) {
        int numberFoundBest = 1;
        long avgCounter = 0;
        long avgTime = 0;
        for (int i = 1; i <= 10; i++) {
            for (int j = 0; j < nThreads; j++) {
                threads.add(new AppDriver(args));
            }
            for (AppDriver var : threads) {
                var.start();
            }

            for (AppDriver var : threads) {
                try {
                    var.join();
                    updateBestPath(var.bestPath);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("\nNúmero do teste: " + i);
            System.out.println("Nome do ficheiro de teste: " + fileName);
            System.out.println("Tempo total de execução: " +
                    TimeUnit.NANOSECONDS.toSeconds(execTime) + " segundos");
            System.out.println("Número de threads usado: " + nThreads);
            System.out.println("Tamanho da população: " + nPaths);
            System.out.println("Probabilidade de mutação: " + mutationProb * 100 + "%");
            System.out.println("Melhor caminho encontrado e sua distância: \n" + bestPath);
            System.out.println("Número de iterações necessárias para chegar ao melhor caminho encontrado: "
                    + bestPath.getCounter());
            System.out.println("Tempo que demorou até o programa atingir o melhor caminho encontrado: "
                    + TimeUnit.NANOSECONDS.toMillis(bestPath.getTimeTaken()) + " milésimos de segundo");

            treatArgs(args);
            threads.clear();
            if (bestOfBestPath == null || bestPath.getDistance() < bestOfBestPath.getDistance()) {
                bestOfBestPath = bestPath;
                numberFoundBest = 1;
                avgCounter = bestPath.getCounter();
                avgTime = bestPath.getTimeTaken();
            } else if (bestOfBestPath != null && bestPath.getDistance() == bestOfBestPath.getDistance()) {
                numberFoundBest++;
                avgCounter += bestPath.getCounter();
                avgTime += bestPath.getTimeTaken();
            }
            bestPath = null;
        }

        System.out.println("\n\nEstatísticas Finais\n");
        System.out.println("Nome do ficheiro de teste: " + fileName);
        System.out.println("Tempo total de execução: " +
                TimeUnit.NANOSECONDS.toSeconds(execTime) * 10 + " segundos");
        System.out.println("Número de threads usado: " + nThreads);
        System.out.println("Tamanho da população: " + nPaths);
        System.out.println("Probabilidade de mutação: " + mutationProb * 100 + "%");
        System.out.println("Melhor caminho encontrado e sua distância: \n" + bestOfBestPath);
        System.out.println("Número de iterações necessárias para chegar ao melhor caminho encontrado: "
                + bestOfBestPath.getCounter());
        System.out.println("Tempo que demorou até o programa atingir o melhor caminho encontrado: "
                + TimeUnit.NANOSECONDS.toSeconds(bestOfBestPath.getTimeTaken()));
        System.out.println("\nNúmero de vezes que o melhor caminho foi encontrado: " + numberFoundBest);
        System.out.println("Tempo médio para o melhor caminho: "
                + TimeUnit.NANOSECONDS.toMillis(avgTime / numberFoundBest) + " milésimos de segundo");
        System.out.println("Iterações médias para o melhor caminho: "
                + (long) ((double) avgCounter / numberFoundBest));


    }

    private synchronized void treatArgs(String[] args) {
        if (args.length != 5) {
            return;
        }
        startTime = System.nanoTime();
        fileName = args[0];
        nThreads = Integer.parseInt(args[1]);
        execTime = TimeUnit.SECONDS.toNanos(Long.parseLong(args[2]));
        nPaths = Integer.parseInt(args[3]);
        mutationProb = Double.parseDouble(args[4]);
    }

    private synchronized void updateBestPath(Path tempBestPath) {
        if (bestPath == null) {
            bestPath = tempBestPath;
        } else {
            if (bestPath.getDistance() > tempBestPath.getDistance()) {
                bestPath = tempBestPath;
            }
        }
    }


    @Override
    public void run() {
        AJEvolving ajEvolving = null;
        long counter = 0;
        try {
            ajEvolving = new AJEvolving(InputReader.readFromTxt(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (ajEvolving != null) {
            ajEvolving.startPaths(nPaths);
            while (System.nanoTime() - startTime < execTime) {
                ajEvolving.pmxCrossover(mutationProb, ++counter, System.nanoTime() - startTime);
            }
            updateBestPath(ajEvolving.bestPath());
        }

    }

}
