import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.apache.commons.lang3.tuple.Pair;
import java.util.HashMap;
import java.util.List;
import java.time.Duration;
import java.time.Instant;

public class Engine {
    // create a hashmap to store MCTS node information (in the future maybe build a custom TT)
    private final HashMap<Long, Pair<Integer, Integer>> mctsHistory = new HashMap<>();
    // Evaluation class
    private final Evaluation evaluation = new Evaluation();
    // Helper class
    private final Helper helper = new Helper();

    public Move Search(Board board, long searchTime, boolean debug, boolean verbose){

        int maxItr = 9999999;
        double mostNumberOfVisit = Double.MIN_VALUE;
        int positionValue = 0;
        int numberOfIterations = 1;
        Move bestMove = null;

        // timer settings
        Instant start = Instant.now();

        // searching loop
        while (numberOfIterations <= maxItr ){
            if (debug){System.out.println("Current itr: " + numberOfIterations);}
            searchMCT(board, debug);
            numberOfIterations++;
            Instant end = Instant.now();
            if (Duration.between(start, end).toMillis() >= searchTime){
                break;
            }
        }

        List<Move> actions = board.pseudoLegalMoves();
        for (Move action : actions){
            int visits;
            // getting the board position to search its corresponding value
            if(!board.doMove(action)){
                continue;
            }
            long key = board.getZobristKey();
            board.undoMove();

            if(mctsHistory.containsKey(key)){
                Pair<Integer, Integer> stats = mctsHistory.get(key);
                visits = stats.getLeft();
                positionValue = stats.getRight();

            }
            else{
                visits = 1;
            }

            double numberOfVisit = visits;
            if (numberOfVisit > mostNumberOfVisit) {
                mostNumberOfVisit = numberOfVisit;
                bestMove = action;
            }

            //if(verbose){System.out.println("Move:" + action + " Value: " + numberOfVisit);}
        }
        if(verbose){System.out.println("Move found: " + bestMove + " State Value: " + positionValue/mostNumberOfVisit + " Number of Iteration: " + numberOfIterations);}
        return bestMove;
    }

    // This function does 1 pass of MCTS at a given depth (ply used for verbose)
    private int searchMCT(Board board, boolean debug){
        //positional evaluation will be the value of the position
        int positionValue;
        long boardPosition = board.getZobristKey();
        double bestUcb = -Double.MAX_VALUE;
        Move moveToExplore = null;

        // Initialize MCTS history, if it exists then extract information, this will be our parent node
        int parentNumberOfVisits;
        int parentSumValue;
        if(mctsHistory.containsKey(boardPosition)){
            Pair<Integer, Integer> mctsHistValue = mctsHistory.get(boardPosition);
            parentNumberOfVisits = mctsHistValue.getLeft();
            parentSumValue = mctsHistValue.getRight();
        }
        else{
            // Fill with empty values
            Pair<Integer,Integer> node = Pair.of(0,0);
            mctsHistory.put(boardPosition, node);
            parentNumberOfVisits = 0;
            parentSumValue = 0;
        }

        // Termination conditions
        if (board.isDraw()) {
            if(debug){System.out.println("In draw case");}
            positionValue = 0;
        }
        else if (board.isMated()){
            if(debug) {System.out.println("In mated case");}
            positionValue = -10000;
        }

        else if(parentNumberOfVisits == 0){
            // if the node has never been visited before, update its value and store it in our MCTS history
            positionValue = evaluation.positionalEvaluation(board);
        }
        // main MCTS algorithm
        else{
            List<Move> actions = board.pseudoLegalMoves();
            // calculate the value of each board state by applying UCB
            for (Move action : actions){
                if (!board.doMove(action)){
                    continue;
                }
                // extract information from mcts of the node
                long childPosition = board.getZobristKey();
                int childNumberOfVisits;
                int childSumValue;

                if(mctsHistory.containsKey(childPosition)){
                    Pair<Integer, Integer> mctsHistValue = mctsHistory.get(childPosition);
                    childNumberOfVisits = mctsHistValue.getLeft();
                    childSumValue = mctsHistValue.getRight();
                }
                // if we haven't seen this node before
                else{
                    childNumberOfVisits = 1;
                    /*
                    if we haven't seen this node before assume its value to be similar to its parent, in other words
                    Q(s) child ≈ Q(s) parent, not that this will make it so that UCB values good heuristic and converges on an optimal
                    heuristic value rather than global optimality. I think this is fine because the global optimally in chess is unknown and
                    trying to do a fresh UCB is a waste of limited time.
                     */
                    childSumValue = -parentSumValue/parentNumberOfVisits;
                }
                board.undoMove();
                // calculate UCB
                double averageValue = (double) childSumValue /childNumberOfVisits;
                double explorationTerm = (double) 100 * Math.sqrt(Math.log(parentNumberOfVisits) / childNumberOfVisits);
                double ucb = explorationTerm - averageValue;

                if(debug){System.out.println("UCB value: " + ucb);}

                if (ucb > bestUcb){
                    bestUcb = ucb;
                    moveToExplore = action;
                }

                if(debug){System.out.println("Move to explore: " + moveToExplore);}
            }
            // recursively calculate the position based on the move selected through argmax A of UCB
            board.doMove(moveToExplore);
            positionValue = -searchMCT(board, debug);
            board.undoMove();
        }
        // store the stuff back into MCTS history
        // the evaluation is the parent evaluation + the leaf node evaluation
        // we will use the evaluation to calculate Q(s) which is simply evaluation / # the node has been visited
        mctsHistory.put(boardPosition,Pair.of(parentNumberOfVisits + 1,parentSumValue + positionValue));

        if(debug){System.out.println("MCTS Eval: " + positionValue);}

        return positionValue;
    }
}
