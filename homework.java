
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

/*
 * Name: Eric J. Hachuel
 * USC loginid: hachuelb
 * CSCI 561 - Artificial Intelligence, Fall 2017 (Homework 1 - Search)
 * Due: September 20th, 2017
 */

/*
* The SANode class generates a node and instantiates energy (# of conflicts), nursery (state), and lizard config.
*/
class SANode {
    
    private int energy;
    private byte[][] nurseryState;
    private boolean[][] lizardState;

    /**
     * Constructor creates a Node for SA and stores relevant information 
     * @param energy the depth of the algorithm (i.e: amount of lizards placed)
     * @param state the 2D array showing the current configuration of the board
     * @param lizardState the 2D boolean array showing the current configuration of lizards
     */
    //When creating node will do SANode (CalculateEnergy(State[][]), State[][])
    public SANode (int energy, byte[][] state, boolean[][] lizardState){
        this.energy = energy;
        this.nurseryState = state;
        this.lizardState = lizardState;
    }

    /**
     * getState returns the state (or 2D nursery configuration of the given Node)
     * @return returns the nursery configuration
     */
    public byte[][] getState(){
        return nurseryState;
    }
    
    //Array stores TRUE where lizard present, false elsewhere
    public boolean[][] getLizardState(){
        return lizardState;
    }
    
    //Get the number of conflicts for the specific state
    public int getEnergy(){
        return energy;
    }
}

/*
* The Node class generates a node and instantiates depth, nursery (state), and parent Node
*/
class Node {
    private int depth;
    private byte[][] nurseryState;

    /**
     * Constructor creates a Node and stores relevant information such as the  
     * @param depth the depth of the algorithm (i.e: amount of lizards placed)
     * @param state the 2D array showing the current configuration of the board
     */
    public Node (int depth, byte[][] state){
        this.depth = depth;
        this.nurseryState = state;
    }

    /**
     * getDepth returns the depth of the given Node
     * @return returns the depth
     */
    public int getDepth(){
        return depth;
    }

    /**
     * getState returns the state (or 2D nursery configuration of the given Node)
     * @return returns the nursery configuration
     */
    public byte[][] getState(){
        return nurseryState;
    }
}

public class homework {
    
    private static final String SOLUTION_FOUND = "OK";
    private static final String SOLUTION_NOT_FOUND = "FAIL";
    private static final int EMPTY_CELL = 0;
    private static final int LIZARD_PRESENT = 1;
    private static final int TREE_PRESENT = 2;
    private static final int UNSATISFACTORY_CELL = 3; //will use to store unavailable cells
    private static final double SA_INITIAL_TEMPERATURE = 10;
    private static final boolean LIZARD = true;
    
    /**
     * calculateTemperature calculates the new temperature for the current iteration using multiplicative cooling
     * @param iterationNumber current iteration
     * @param coolingSchedule the mathematical formula for the cooling
     * @return the newly calculated temperature
     */
    public static double calculateTemperature(int iterationNumber, String coolingSchedule){
        //Initialize new temperature
        double newTemperature = 0;
        //Select cooling scheduling
        if(coolingSchedule.contentEquals("MC")){
            //initialize alpha constant to use multiplicative cooling 
            double alpha_constant = 0.95;
            newTemperature = SA_INITIAL_TEMPERATURE * (Math.pow(alpha_constant, iterationNumber));
            //return the newly calculated temperature
            return newTemperature; 
        }
        
        else if(coolingSchedule.contentEquals("LOGC")){
            double alpha_constant = 1.5;
            newTemperature = (SA_INITIAL_TEMPERATURE)/(1 + (alpha_constant * Math.log(1 + iterationNumber)));
            //return the newly calculated temperature
            return newTemperature; 
        }
        
        else if(coolingSchedule.contentEquals("QUADC")){
            double alpha_constant = 1.5;
            newTemperature = (SA_INITIAL_TEMPERATURE)/(1 + (alpha_constant * Math.pow(iterationNumber, 2)));
            //return the newly calculated temperature
            return newTemperature; 
        }
        return newTemperature;
    }

    /**
     * calculateStateEnergy calculates the number of conflicts (i.e. energy) in our state
     * @param state the current state
     * @param lizardState the state/configuration of the lizards
     * @return the number of conflicts, if any.
     */
    public static int calculateStateEnergy(byte[][] state, boolean[][] lizardState){
        
        //Initialize the number of conflicts (energy) to zero
        int numberOfConflicts = 0;

        //Loop through state, get number of conflicts
        for(int currRow = 0; currRow < state.length; currRow++){
            for(int currCol = 0; currCol < state.length; currCol++){
                //if find a 1, calculate its conflicts with all other 1s
                if(state[currRow][currCol] == 1){
                    //loop through boolean array
                    for(int boolRow = 0; boolRow < state.length; boolRow++){
                        for(int boolCol = 0; boolCol < state.length; boolCol++){
                            if(currRow == boolRow && currCol == boolCol){
                                continue;
                            }
                            else{
                                //If there is a lizard at current location, calculate conflicts with current element
                                if(lizardState[boolRow][boolCol]){
                                    //ROWS
                                    //Check if the current empty cell is in the same row as the reference location
                                    sameRowBreak: if(currRow == boolRow){
                                        //Check if reference cell to the right of current cell
                                        if(boolCol - currCol >= 1){
                                            for(int loopCol = currCol; loopCol < boolCol; loopCol++){
                                                if(state[currRow][loopCol] == TREE_PRESENT){
                                                    break sameRowBreak;
                                                }
                                            }
                                            //If for loop is done (no break) and no trees have been found: add a conflict
                                            numberOfConflicts++;
                                        }
                                        //Check if reference cell to the left of current cell
                                        else if(boolCol - currCol <= -1){
                                            for(int loopCol = boolCol; loopCol < currCol; loopCol++){
                                                if(state[currRow][loopCol] == TREE_PRESENT){
                                                    break sameRowBreak;
                                                }
                                            }
                                            //If for loop is done (no break) and no trees have been found: add a conflict
                                            numberOfConflicts++;
                                        } 
                                    }
                                    //COLUMNS
                                    //Check if the current empty cell is in the same column as the reference location
                                    sameColBreak: if(currCol == boolCol){
                                        //Check if the reference cell is below the empty cell
                                        if(boolRow - currRow >= 1){
                                            for(int loopRow = currRow; loopRow < boolRow; loopRow++){
                                                if(state[loopRow][currCol] == TREE_PRESENT){
                                                    break sameColBreak;
                                                }
                                            }
                                            //If for loop is done (no break) and no trees have been found: add a conflict
                                            numberOfConflicts++;
                                        }
                                        //Check if the reference cell is above the empty cell
                                        else if(boolRow - currRow <= -1){
                                            for(int loopRow = boolRow; loopRow < currRow; loopRow++){
                                                if(state[loopRow][currCol] == TREE_PRESENT){
                                                    break sameColBreak;
                                                }
                                            }
                                            //If for loop is done (no break) and no trees have been found: add a conflict
                                            numberOfConflicts++;
                                        }
                                    }
                                    //DIAGONALS
                                    //Check if current empty cell in same minor (upper-left or lower-right) diagonal as reference cell
                                    sameMinorDiag: if(Math.abs(currRow - boolRow) == Math.abs(currCol - boolCol)){
                                        //Check if reference on lower left direction of current cell
                                        if(currRow > boolRow && currCol < boolCol){
                                            //Create diagonal row and column variables for iterating over the diagonal
                                            int diagLoopRow = currRow;
                                            int diagLoopCol = currCol;
                                            //While loop to loop at all values from the reference cell to the empty cell in diagonal
                                            while(diagLoopRow > boolRow){
                                                if(state[diagLoopRow][diagLoopCol] == TREE_PRESENT){
                                                    break sameMinorDiag;
                                                }
                                                diagLoopRow = diagLoopRow - 1;
                                                diagLoopCol = diagLoopCol + 1;
                                            }
                                            //If for loop is done (no break) and no trees have been found: add a conflict
                                            numberOfConflicts++;
                                        }
                                        //Check if reference on upper left direction of current cell
                                        if(currRow < boolRow && currCol < boolCol){
                                            //Create diagonal row and column variables for iterating over the diagonal
                                            int diagLoopRow = currRow;
                                            int diagLoopCol = currCol;
                                            //While loop to loop at all values from the reference cell back to the empty cell
                                            while(diagLoopRow < boolRow){
                                                if(state[diagLoopRow][diagLoopCol] == TREE_PRESENT){
                                                    break sameMinorDiag;
                                                }
                                                diagLoopRow = diagLoopRow + 1;
                                                diagLoopCol = diagLoopCol + 1;
                                            }
                                            //If for loop is done (no break) and no trees have been found: add a conflict
                                            numberOfConflicts++;
                                        }
                                        //Check if reference on upper right direction of current cell
                                        if(currRow < boolRow && currCol > boolCol){
                                            //Create diagonal row and column variables for iterating over the diagonal
                                            int diagLoopRow = currRow;
                                            int diagLoopCol = currCol;
                                            //While loop to loop at all values from the reference cell back to the empty cell
                                            while(diagLoopRow < boolRow){
                                                if(state[diagLoopRow][diagLoopCol] == TREE_PRESENT){
                                                    break sameMinorDiag;
                                                }
                                                diagLoopRow = diagLoopRow + 1;
                                                diagLoopCol = diagLoopCol - 1;
                                            }
                                            //If for loop is done (no break) and no trees have been found: add a conflict
                                            numberOfConflicts++;
                                        }
                                        //Check if reference on lower right direction of current cell
                                        if(currRow > boolRow && currCol > boolCol){
                                            //Create diagonal row and column variables for iterating over the diagonal
                                            int diagLoopRow = currRow;
                                            int diagLoopCol = currCol;
                                            //While loop to loop at all values from the reference cell back to the empty cell
                                            while(diagLoopRow > boolRow){
                                                if(state[diagLoopRow][diagLoopCol] == TREE_PRESENT){
                                                    break sameMinorDiag;
                                                }
                                                diagLoopRow = diagLoopRow - 1;
                                                diagLoopCol = diagLoopCol - 1;
                                            }
                                            //If for loop is done (no break) and no trees have been found: add a conflict
                                            numberOfConflicts++;
                                        }
                                    }
                                }
                            } 
                        }
                    }
                }
            }
        }
        //Return the number of Conflicts or Energy of the given state
        return numberOfConflicts;
    }
    
    /**
     * generateSuccessorNode generates a successor Node for SA by randomly changing the location of a random lizard
     * @param currentState the state of the current node
     * @param currentLizardState the boolean array specifying the location of the lizards for the current node
     * @return Returns the newly generated successor node
     */
    public static SANode generateSuccessorNode(byte[][] currentState, boolean[][] currentLizardState){
    
        int stateSize = currentState.length;
        //Quantity of lizards moved to new location (randomly)
        boolean lizardMoved = false;
        
        //Create new state and copy items from front node state
        byte[][] successorNodeState = new byte[stateSize][stateSize];
        boolean[][] successorNodeBoolean = new boolean[stateSize][stateSize];
        
        //Copy items from given arrays into newly created ones
        for(int i = 0; i < stateSize; i++){
            for(int j = 0; j < stateSize; j++){
                successorNodeState[i][j] = currentState[i][j];
                if(successorNodeState[i][j] == LIZARD_PRESENT){
                    //Increase the amount of available cells
                    successorNodeBoolean[i][j] = LIZARD;
                }
            }
        }
        
        //Randomly modify successorNodeState
        while(lizardMoved == false){
            //generate a random row and column in state
            int randomRowEmpty = ThreadLocalRandom.current().nextInt(0,stateSize);
            int randomColEmpty = ThreadLocalRandom.current().nextInt(0,stateSize);
            //If the randomly generated cell happens to be an empty cell, proceed.
            if(successorNodeState[randomRowEmpty][randomColEmpty] == EMPTY_CELL){
                //generate a random row and column
                int randomRowLizard = ThreadLocalRandom.current().nextInt(0,stateSize);
                int randomColLizard = ThreadLocalRandom.current().nextInt(0,stateSize);
                
                //Generate new row and column values until you fall on a cell containing a lizard
                while(successorNodeState[randomRowLizard][randomColLizard] != LIZARD_PRESENT){
                    //generate a random row and column in state to start looping from
                    randomRowLizard = ThreadLocalRandom.current().nextInt(0,stateSize);
                    randomColLizard = ThreadLocalRandom.current().nextInt(0,stateSize);
                }
                //"swap" the two cells 
                successorNodeState[randomRowEmpty][randomColEmpty] = LIZARD_PRESENT;
                successorNodeBoolean[randomRowEmpty][randomColEmpty] = true;
                successorNodeState[randomRowLizard][randomColLizard] = EMPTY_CELL;
                successorNodeBoolean[randomRowLizard][randomColLizard] = false;     
                //set boolean to true for while loop termination
                lizardMoved = true;
            }
        }
        //Calculate the energy of the new state
        int successorEnergy = calculateStateEnergy(successorNodeState, successorNodeBoolean);
        //Initialize a new node with the new energy, state, and boolean arrays
        SANode successorNode = new SANode(successorEnergy, successorNodeState, successorNodeBoolean);
        //Return the new successorNode
        return successorNode;
    }
    
    /**
     * InitializeRandomNode initializes the first node with the given input file
     * @param inputNursery
     * @param lizards
     * @return returns the first node
     */
    public static SANode InitializeRandomNode(byte[][] inputNursery, int lizards){
        
        int lizardsPlaced = 0;
        int totalAvailable = 0;
        int nurserySize = inputNursery.length;
        
        //input nursery has energy of 0 since no lizards
        //Create new state and copy items from front node state
        byte[][] initialNodeState = new byte[nurserySize][nurserySize];
        boolean[][] initialNodeBoolean = new boolean[nurserySize][nurserySize];
        
        //Initialize a new node
        SANode initialNode = new SANode(0, null, null);
        
        //Copy items from front node state into newly created array
        for(int i = 0; i < nurserySize; i++){
            for(int j = 0; j < nurserySize; j++){
                initialNodeState[i][j] = inputNursery[i][j];
                //Count number of zeros for adding random lizards
                if(initialNodeState[i][j] == EMPTY_CELL){
                    //Increase the amount of available cells
                    totalAvailable++;
                }
            }
        }
        //If there are enough empty cells, proceed
        if(totalAvailable >= lizards){
            while(lizardsPlaced < lizards){
                //generate a random row and column in state
                int randomRow = ThreadLocalRandom.current().nextInt(0,nurserySize);
                int randomCol = ThreadLocalRandom.current().nextInt(0,nurserySize);

                if(initialNodeState[randomRow][randomCol] == EMPTY_CELL){
                    //Add a lizard to the state
                    initialNodeState[randomRow][randomCol] = LIZARD_PRESENT;
                    //Add a lizard to the boolean array
                    initialNodeBoolean[randomRow][randomCol] = LIZARD;
                    //Increment number of lizards placed
                    lizardsPlaced++;
                }
            }
            //Calculate number of conflicts in the given state
            int energyLevel = calculateStateEnergy(initialNodeState, initialNodeBoolean);
            //Update the new node with the new states
            initialNode = new SANode(energyLevel, initialNodeState, initialNodeBoolean); 
        }
        //If there aren't enough empty cells, return null and declare failure
        else{
            return null; //not enough available cells
        }   
        return initialNode;
    }
    
    /**
     * printOutputNursery prints the output nursery to an 'output.txt' file on the current directory
     * @param solutionResult the solution type: OK or FAIL
     * @param outputNursery the output 2D array solution, if any
     * @throws java.io.FileNotFoundException
     */
    public static void printOutputNursery(String solutionResult, byte[][] outputNursery) throws FileNotFoundException {
        PrintStream outputFileStream = new PrintStream( new FileOutputStream("output.txt"));
        if(outputNursery == null){
            outputFileStream.println("FAIL");
        }
        else{
            outputFileStream.println("OK");
            for(int i=0; i< outputNursery.length; i++){
                for(int j=0; j< outputNursery.length; j++){
                    if(outputNursery[i][j] == UNSATISFACTORY_CELL){
                        outputFileStream.print(0);
                    }
                    else{
                        outputFileStream.print(outputNursery[i][j]);
                    } 
                }
            outputFileStream.println(); //skip line after every row
            }
        } 
    }
    
    /**
     * addConstraints adds unavailable cells to the newly generated node
     * @param newNode the newly created node you want to add constraints to
     * @param refRow the row of the newly added lizard (for reference)
     * @param refCol the column of the newly added lizard (for reference)
     * @return returns the Node with its changed state (with added constraints)
     */
    public static Node addConstraints(Node newNode, int refRow, int refCol){
        //Store State
        byte[][] newNodeState = newNode.getState();
        //Loop through input node's state 2D array, check for constraint satisfaction
        for(int currRow = 0; currRow < newNodeState.length; currRow++){
            for(int currCol = 0; currCol < newNodeState.length; currCol++){
                //Check if current location is an empty location
                if(newNodeState[currRow][currCol] == EMPTY_CELL){
                    //ROWS
                    //Check if the current empty cell is in the same row as the reference location
                    sameRowBreak: if(currRow == refRow){
                        //Check if reference cell to the right of current cell
                        if(refCol - currCol >= 1){
                            for(int loopCol = currCol; loopCol < refCol; loopCol++){
                                if(newNodeState[currRow][loopCol] == TREE_PRESENT){
                                    break sameRowBreak;
                                }
                            }
                            //If for loop is done and no trees have been found: add a 3 [unsatisfactory cell].
                            newNodeState[currRow][currCol] = UNSATISFACTORY_CELL;
                        }
                        //Check if reference cell to the left of current cell
                        else if(refCol - currCol <= -1){
                            for(int loopCol = refCol; loopCol < currCol; loopCol++){
                                if(newNodeState[currRow][loopCol] == TREE_PRESENT){
                                    break sameRowBreak;
                                }
                            }
                            //If for loop is done (no break) and no trees have been found: add a 3 [unsatisfactory cell].
                            newNodeState[currRow][currCol] = UNSATISFACTORY_CELL;
                        } 
                    }
                    //COLUMNS
                    //Check if the current empty cell is in the same column as the reference location
                    sameColBreak: if(currCol == refCol){
                        //Check if the reference cell is below the empty cell
                        if(refRow - currRow >= 1){
                            for(int loopRow = currRow; loopRow < refRow; loopRow++){
                                if(newNodeState[loopRow][currCol] == TREE_PRESENT){
                                    break sameColBreak;
                                }
                            }
                            //If for loop is done (no break) and no trees have been found: add a 3 [unsatisfactory cell].
                            newNodeState[currRow][currCol] = UNSATISFACTORY_CELL;
                        }
                        //Check if the reference cell is above the empty cell
                        else if(refRow - currRow <= -1){
                            for(int loopRow = refRow; loopRow < currRow; loopRow++){
                                if(newNodeState[loopRow][currCol] == TREE_PRESENT){
                                    break sameColBreak;
                                }
                            }
                            //If for loop is done (no break) and no trees have been found: add a 3 [unsatisfactory cell].
                            newNodeState[currRow][currCol] = UNSATISFACTORY_CELL;
                        }
                    }
                    //DIAGONALS
                    //Check if current empty cell in same minor (upper-left or lower-right) diagonal as reference cell
                    sameMinorDiag: if(Math.abs(currRow - refRow) == Math.abs(currCol - refCol)){
                        //Check if reference on lower left direction of current cell
                        if(currRow > refRow && currCol < refCol){
                            //Create diagonal row and column variables for iterating over the diagonal
                            int diagLoopRow = currRow;
                            int diagLoopCol = currCol;
                            //While loop to loop at all values from the reference cell to the empty cell in diagonal
                            while(diagLoopRow > refRow){
                                if(newNodeState[diagLoopRow][diagLoopCol] == TREE_PRESENT){
                                    break sameMinorDiag;
                                }
                                diagLoopRow = diagLoopRow - 1;
                                diagLoopCol = diagLoopCol + 1;
                            }
                            //If for loop is done (no break) and no trees have been found: add a 3 [unsatisfactory cell].
                            newNodeState[currRow][currCol] = UNSATISFACTORY_CELL;
                        }
                        //Check if reference on upper left direction of current cell
                        if(currRow < refRow && currCol < refCol){
                            //Create diagonal row and column variables for iterating over the diagonal
                            int diagLoopRow = currRow;
                            int diagLoopCol = currCol;
                            //While loop to loop at all values from the reference cell back to the empty cell
                            while(diagLoopRow < refRow){
                                if(newNodeState[diagLoopRow][diagLoopCol] == TREE_PRESENT){
                                    break sameMinorDiag;
                                }
                                diagLoopRow = diagLoopRow + 1;
                                diagLoopCol = diagLoopCol + 1;
                            }
                            //If for loop is done (no break) and no trees have been found: add a 3 [unsatisfactory cell].
                            newNodeState[currRow][currCol] = UNSATISFACTORY_CELL;
                        }
                        //Check if reference on upper right direction of current cell
                        if(currRow < refRow && currCol > refCol){
                            //Create diagonal row and column variables for iterating over the diagonal
                            int diagLoopRow = currRow;
                            int diagLoopCol = currCol;
                            //While loop to loop at all values from the reference cell back to the empty cell
                            while(diagLoopRow < refRow){
                                if(newNodeState[diagLoopRow][diagLoopCol] == TREE_PRESENT){
                                    break sameMinorDiag;
                                }
                                diagLoopRow = diagLoopRow + 1;
                                diagLoopCol = diagLoopCol - 1;
                            }
                            //If for loop is done (no break) and no trees have been found: add a 3 [unsatisfactory cell].
                            newNodeState[currRow][currCol] = UNSATISFACTORY_CELL;
                        }
                        //Check if reference on lower right direction of current cell
                        if(currRow > refRow && currCol > refCol){
                            //Create diagonal row and column variables for iterating over the diagonal
                            int diagLoopRow = currRow;
                            int diagLoopCol = currCol;
                            //While loop to loop at all values from the reference cell back to the empty cell
                            while(diagLoopRow > refRow){
                                if(newNodeState[diagLoopRow][diagLoopCol] == TREE_PRESENT){
                                    break sameMinorDiag;
                                }
                                diagLoopRow = diagLoopRow - 1;
                                diagLoopCol = diagLoopCol - 1;
                            }
                            //If for loop is done (no break) and no trees have been found: add a 3 [unsatisfactory cell].
                            newNodeState[currRow][currCol] = UNSATISFACTORY_CELL;
                        }
                    } 
                } 
            }
        }
        //return the updated node to add it to the Linked List
        return newNode;
    }
    
    /**
     * Takes input.txt parsed items as input and solves the specified search algorithm. Returns a solution or failure.
     * @param algorithm the type of algorithm to use
     * @param lizardQty the quantity of lizards that must be placed
     * @param inputNursery the initial nursery configuration (2D matrix)
     * @return returns the 2D matrix solution if any, or reports failure if encountered
     */
    public static byte[][] solveSearch(String algorithm, int lizardQty, byte[][] inputNursery){
        
        if(algorithm.contentEquals("BFS") || algorithm.contentEquals("DFS")){
            
            //Create initial Node (parent is null)
            Node firstNode = new Node(0, inputNursery);
            //Initialize the queue of nodes
            LinkedList<Node> nodeQueue = new LinkedList<>();
            //Add first node to the Queue
            nodeQueue.add(firstNode);
            
            do{
                //remove Node from front of queue and evaluate goal test
                Node frontNode = nodeQueue.remove();
                //store state (nursery configuration) of front node in memory
                byte[][] frontState = frontNode.getState();
                //Store state size for subsequent loops
                int stateSize = frontState.length;
                //Store front node depth
                int frontDepth = frontNode.getDepth();
                //Check if you have placed the required amount of lizards, if so - return the state (GOAL TEST)
                if(frontDepth == lizardQty){
                    return frontState;
                }
               
                else{
                    //Create a new Node, increase its depth, and add the lizard to the newly discovered cell (if cell available)
                    Node newNode = new Node(frontDepth + 1, frontState);
                    //integers to store row and col location of the added lizard
                    int refRow = 0;
                    int refCol = 0;
                    //Loop through front node state (nursery configuration) until you find the next empty cell
                    for(int i = 0; i < stateSize; i++){
                        for(int j = 0; j < stateSize; j++){
                            //If you find an empty cell, add a lizard
                            if(frontState[i][j] == EMPTY_CELL){
                                //Create new state and copy items from front node state
                                byte[][] newFrontState = new byte[frontState.length][frontState.length];
                                //Copy items from front node state into newly created array
                                for(int k = 0; k < frontNode.getState().length; k++){
                                    for(int l = 0; l < frontNode.getState().length; l++){
                                        newFrontState[k][l] = frontState[k][l];
                                    }
                                }
                                //Create a new Node, increase its depth, and add the lizard to the newly discovered cell (if cell available)
                                newNode = new Node(frontDepth + 1, newFrontState);
                                //Add lizard to newly created node's state
                                newNode.getState()[i][j] = LIZARD_PRESENT;
                                //Store the (i,j) coordinates of the newly added lizard
                                refRow = i;
                                refCol = j;
                                //Once an empty cell has been filled with a lizard, we run the constraints function
                                //This will add a 3 to every location that does not meet the requirements for subsequent placements - returns the updated Node
                                newNode = addConstraints(newNode, refRow, refCol);
                                //Add new node to the nodeQueue and iterate
                                //If the algorithm is BFS, add node to the BACK of the queue
                                if(algorithm.contentEquals("BFS")){
                                    nodeQueue.addLast(newNode);
                                }
                                //If the algorithm is DFS, add node to the FRONT of the queue
                                else if(algorithm.contentEquals("DFS")){
                                    nodeQueue.addFirst(newNode);
                                }   
                            }
                        }
                    }           
                }                
            } while (!nodeQueue.isEmpty()); //end while loop if node nodeQueue is empty and return null
            //Return null if end of queue reached (no elements in queue left to look at)
            return null;
        }
        
        else if(algorithm.contentEquals("SA")){
            //Track Start Time for the program termination
            long programStartTime = System.currentTimeMillis();
            //Initialize Temperature
            double currentTemperature = SA_INITIAL_TEMPERATURE;
            //Initialize iteration number (for while loop)
            int iterationNumber = 0;
            //Cooling Schedule used
            String cooling = "LOGC";
            //Energy Delta (difference in amount of conflicts)
            int energyDelta;
            //Create and initialize the first Node with the initial Nursery setup as input
            SANode currentNode = new SANode(0, null, null);
            currentNode = InitializeRandomNode(inputNursery, lizardQty);
            //Check if the node is null, if so, return null to output "FAIL".
            if(currentNode == null){
                return null;
            }
            //Run Simulated Annealing Algorithm
            //If there are no conflicts in the initial board, return the state
            else if(currentNode.getEnergy() == 0){
                return currentNode.getState();
            }
            //If the initial Node is not null and there are conflicts, run the algorithm
            else{
                //Initialize current time and elapsed time variables
                long programCurrentTime;
                long elaspedTime = 0;
                
                //'Infinite Loop' for simulated annealing
                //Loop will terminate after 4.5 minutes or when temperature reaches 0
                while(elaspedTime < 270000 && currentTemperature > 0){
                    //Create next: A node with a randomly selected successor of current (moves random Lizard)
                    SANode successorNode = new SANode(0, null, null);
                    successorNode = generateSuccessorNode(currentNode.getState(), currentNode.getLizardState());
                    //If there are no conflicts in the new board, return its state
                    if(successorNode.getEnergy() == 0){
                        return successorNode.getState();
                    }
                    //If there are conflicts, calculate the difference between the new node and the current node
                    else{
                        //Calculate the difference in energy (amount of conflicts) between the current and successor nodes
                        energyDelta = successorNode.getEnergy() - currentNode.getEnergy();
                        //If the sucessor has less conflicts than the current node, set current to successor
                        if(energyDelta < 0){
                            //currentNode now points to the sucessorNode
                            currentNode = successorNode;
                        }
                        //If the successor node has more conflicts than the current node, accept the successor node with a certain probability
                        else{
                            double randomLimit = Math.random();
                            double acceptProbability = Math.exp(-energyDelta/currentTemperature);
                            //if the acceptance probability is larger than the random limit, current node points to successor node
                            if(acceptProbability > randomLimit){
                                currentNode = successorNode;
                            }
                        }
                        //Elapsed Time calculation
                        programCurrentTime = System.currentTimeMillis();
                        elaspedTime = programCurrentTime - programStartTime;
                        //Increase the iteration number
                        iterationNumber++;
                        //Calculate temperature for current interation number with specified cooling schedule function
                        currentTemperature =  calculateTemperature(iterationNumber, cooling);
                    }
                }
            }
        }
        return null; //returns null if neither of the three algorithms is found
    }
    
    //CLASS TO TEST PRINTING ARRAYS
    public static void printArrays(byte[][] inputArray, boolean[][] inputBoolean){
        //OUTPUT TESTS:
        for(int i = 0; i < inputArray.length; i++){
            for(int j = 0; j < inputArray.length; j++){
                System.out.print(inputArray[i][j]);
            }
            System.out.println();
        }
        System.out.println();
        
        if(inputBoolean != null){
            for(int i = 0; i < inputBoolean.length; i++){
                for(int j = 0; j < inputBoolean.length; j++){
                    System.out.print(inputBoolean[i][j]);
                }
                System.out.println();
            }
        }
    }
    
    public static void main(String[] args) throws FileNotFoundException {
        //Read input file from current directory                   
        //Store file and pass to new Scanner object for reading
        File inputFile = new File("input.txt");
        Scanner in = new Scanner(inputFile);
        //Read file's first line for Search Algorithm type (i.e. BFS, DFS, SA)
        String searchAlgorithm = in.next();
        //Read file's second line for the width and height of the square nursery
        int nurserySize = in.nextInt();
        //Read file's third line for the number of baby lizards
        int lizardQty = in.nextInt();
        //Read file's next n lines to store tree locations in 2D array inputBoard
        byte[][] inputBoard = new byte[nurserySize][nurserySize];
        //Consume line to start looping through array
        in.nextLine();
        //Fill in newly created 2D matrix (parse Strings into numeric values)
        for(int i = 0; i < nurserySize; i++){
           //Read line by line and store data in array
           String nextRow = in.nextLine();
           //Read each column value, convert character to int
           for(int j = 0; j < nurserySize; j++){
               inputBoard[i][j] = (byte) Character.getNumericValue(nextRow.charAt(j));
           }
        }
        //Solve the lizard puzzle with the given algorithm
        byte[][] outputNursery = solveSearch(searchAlgorithm, lizardQty, inputBoard);
        //If the algorithm does not find a solution, print "FAIL"
        if(outputNursery == null){
            printOutputNursery(SOLUTION_NOT_FOUND, null);
        }
        //If a solution is found, print "OK" and the solution 2D array
        else{
            printOutputNursery(SOLUTION_FOUND, outputNursery);
        }
    }
}
