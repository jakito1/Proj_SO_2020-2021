#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <string.h>
#include <signal.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdbool.h>
#include <limits.h>

int** importData (char* filename, int* size);
void startPath (int size, int* path);
int distance (int size, int* path, int** matrix);
void swap (int size, int* path);
void killWorkers (int ammount, int pids[ammount]);


/*
Main program
Can be run with 3 or 4 arguments
Example of 3 arguments: "./main uk12.txt 10 15" - this will run the test uk12 with 10 instances for 15 seconds, once
Example of 4 arguments: "./main uk12.txt 10 15 20" - this will run the test uk12 with 10 instances for 15 seconds 20 times (each with 10 instances and for 15 seconds)
*/
int main (int argc, char *argv[]){

    //Check if the program received the correct number of arguments
    if (argc != 4 && argc != 5){
        printf("Invalid Arguments!\n");
        exit(EXIT_FAILURE);
    }

    int protection = PROT_READ | PROT_WRITE;
    int visibility = MAP_ANONYMOUS | MAP_SHARED;

    char name[(sizeof("tsp_testes/") + sizeof(argv[1])) / sizeof(char)];
    strcpy(name, "tsp_testes/");
    strcat(name, argv[1]);
    int numberWorkers = atoi(argv[2]);
    int duration = atoi(argv[3]);
    int loopTimes = 1;
    if (argc == 5){
        loopTimes = atoi(argv[4]);
    }

    //Validate the arguments received
    if (numberWorkers < 1 || duration < 1 || loopTimes < 1){
        printf("Invalid Arguments!\n");
        exit(EXIT_FAILURE);
    }

    int** matrix;
    int size;
    matrix = importData(name, &size);

    //Setting up the shared memory for all runs
    int *bestBestDistance = mmap(NULL, sizeof(int), protection, visibility, 0, 0);
    int *bestBestPath = mmap(NULL, sizeof(int) * size, protection, visibility, 0, 0);
    long *bestBestTime = mmap(NULL, sizeof(long), protection, visibility, 0, 0);
    int *bestBestCounter = mmap(NULL, sizeof(int), protection, visibility, 0, 0);
    int *totalBest = mmap(NULL, sizeof(int), protection, visibility, 0, 0);

    bestBestDistance[0] = INT_MAX; 
    long totalBestIterations = 0;
    int totalBestTime = 0;
    totalBest[0] = 0;

    //Loop responsible for repeating the test multiple times
    for (int i = 0; i < loopTimes; i++){

        srand(time(NULL));

        sem_unlink("working");
        sem_t *sem_working = sem_open("working", O_CREAT, 0644, 1);

        int counter = 0;
        int pids[numberWorkers];

        //Setting up the shared memory for a single run
        int *path = mmap(NULL, sizeof(int) * size, protection, visibility, 0, 0);
        int *bestDistance = mmap(NULL, sizeof(int), protection, visibility, 0, 0);
        int *bestPath = mmap(NULL, sizeof(int) * size, protection, visibility, 0, 0);
        long *bestTime = mmap(NULL, sizeof(long), protection, visibility, 0, 0);
        int *bestCounter = mmap(NULL, sizeof(int), protection, visibility, 0, 0);
    
        startPath(size, path);

        bestDistance[0] = INT_MAX;

        long begin = time(NULL);

        /*
        Children Driver
        This is where the bulk of the work is done. Each child will be attempting to find the shortest path
        */
        for (int j = 0; j < numberWorkers; j++){
            pids[j] = fork();
            if (pids[j] == 0){
                while (1){
                    sem_wait(sem_working);
                    counter++;
                    int dist = distance(size, path, matrix);
                    swap(size, path);

                    //Temporarily set this run best                
                    if (dist < bestDistance[0]){
                        bestDistance[0] = dist;

                        for (int i = 0; i < size; i++){
                            bestPath[i] = path[i];
                        }

                        bestPath = path;
                        bestTime[0] = time(NULL) - begin;
                        bestCounter[0] = counter;
                    }

                    //Temporarily set the overall best
                    if (bestDistance[0] < bestBestDistance[0]){
                        totalBest[0] = 0;
                        bestBestDistance[0] = bestDistance[0];

                        for (int i = 0; i < size; i++){
                            bestBestPath[i] = bestPath[i];
                        }

                        bestBestTime[0] = bestTime[0];
                        bestBestCounter[0] = bestCounter[0];
                    }

                    sem_post(sem_working);
                }
            }
        }

        //Parent controls the time the children spent working and kills them after such time ends
        while (1){
            if ((time(NULL) - begin) > duration){
                killWorkers(numberWorkers, pids);
                break;          
            }
        }

        if (bestBestDistance[0] == bestDistance[0]){
            totalBest[0]++;
        }


        totalBestIterations += bestCounter[0];
        totalBestTime += bestTime[0];


        printf("Test Number: %d\n", i + 1);
        printf("Test Name: %s\n", argv[1]);
        printf("Number of cities: %d\n", size);
        printf("Number of processes: %d\n", numberWorkers);
        printf("Best Distance: %d\n", bestDistance[0]);
        printf("Best Path: ");
        
        for (int i = 0; i < size; i++){
            printf("%d ", bestPath[i] + 1);
        }

        printf("\nBest Iteration: %d\n", bestCounter[0]);
        printf("Best Time: %ld seconds\n\n", bestTime[0]);

        //Freeing the shared memory set for this run only
        munmap(path, sizeof(int) * size);
        munmap(bestDistance, sizeof(int));
        munmap(bestPath, sizeof(int) * size);
        munmap(bestTime, sizeof(long));
        munmap(bestCounter, sizeof(int));
    }


    printf("------------------------- STATISTICS ----------------------------\n");
    printf("Total Tests: %d\n", loopTimes);
    printf("Test Name: %s\n", argv[1]);
    printf("Total Execution Time: %d seconds\n", duration * loopTimes);
    printf("Number of processes: %d\n", numberWorkers);
    printf("Best Distance: %d\n", bestBestDistance[0]);
    printf("Best Path: ");
        
    for (int i = 0; i < size; i++){
        printf("%d ", bestBestPath[i] + 1);
    }
    
    printf("\nBest Iteration: %d\n", bestBestCounter[0]);
    printf("Best Time: %ld seconds\n", bestBestTime[0]);
    if (totalBestTime == 0){
        printf("Average Best Time: 0 seconds\n");
    }else{
        printf("Average Best Time: %d seconds\n", totalBestTime / loopTimes);
    }
    if (totalBestIterations == 0){
        printf("Average Best Iteration: 0\n");
    }else{
        printf("Average Best Iteration: %ld\n", totalBestIterations / loopTimes);
    }   
    printf("Total Best Times: %d\n\n", totalBest[0]);

    //Free the globally shared memory
    munmap(bestBestPath, sizeof(int) * size);
    munmap(bestBestDistance, sizeof(int));
    munmap(bestBestCounter, sizeof(int));
    munmap(bestBestTime, sizeof(long));
    munmap(totalBest, sizeof(int));

    //Free the matrix that held the test
    for (int i = 0; i < size; i++){
        free(matrix[i]);
    }
    free(matrix);

    exit (EXIT_SUCCESS);
}

/*
Function to import the required data
Will receive the address of the filename and the address to where it should return the number of cities
Will return the address of the array of addresses where the data is located
*/
int** importData(char* filename, int* size){
    FILE *f;
    f = fopen(filename, "r");

    if (f == NULL) {
		printf("An error ocurred...It was not possible to open the file %s...\n", filename);
		exit(EXIT_FAILURE);
	}

    fscanf(f, "%d", size);
    int** matrix = malloc(sizeof(int*) * *size);

    for (int i = 0; i < *size; i++){
        matrix[i] = malloc(sizeof(int) * *size);
        for (int j = 0; j < *size; j++){         
            fscanf(f, "%d", &matrix[i][j]);
        }
    }

    fclose(f);
    return matrix;
}

/*
Funtion to randomly generate a starting path
Receives the number of cities and the address to where return the path
*/
void startPath (int size, int* path){
    for (int i = 0; i < size; i++){
        path[i] = i;
    }
    for (int i = 0; i < size; i++){
        swap(size, path);
    }
}

/*
Function to calculate the distance of a given path
Receives the number of cites, the address of the path to calculate and the full matrix where all the data is located
Returns the distance
*/
int distance (int size, int* path, int** matrix){
    int distance = 0;

    for (int i = 0; i < size - 1; i++){
        int curr = path[i];
        int next = path[i + 1];
        distance += matrix[curr][next];
    }

    int last = path[size - 1];
    int first = path[0];
    distance += matrix [last][first];
    return distance;
}

/*
Function to swap two positions of the path
Receives the number of cities and the address of the path in which the two positions will be swapped
*/
void swap (int size, int* path){
    int a = rand() % size;
    int b = rand() % size;
    int temp = path[a];
    path[a] = path[b];
    path[b] = temp ;
}

/*
Function to kill all children
Receives the number of children and the array containing the pids of such children
*/
void killWorkers (int ammount, int pids[ammount]){
    for (int i=0; i < ammount; i++) {
        kill(pids[i], SIGKILL);
    }
}
