import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.apache.commons.lang3.tuple.Pair;
import java.util.HashMap;
import java.util.List;
import java.time.Duration;
import java.time.Instant;

public class Engine {
    // create a hashmap to store MCTS node information (in the future maybe build a custom TT)
    private final HashMap<Pair<Long, Boolean>, Pair<Integer, Integer>> mctsHistory = new HashMap<>();
    // Evaluation class
    private final Evaluation evaluation = new Evaluation();
    // Helper class
    private final Helper helper = new Helper();
    //stats
    private int ROLLOUT_COUNT = 0;

    public Move Search(Board board, long searchTime, boolean debug, boolean verbose){
        int mostNumberOfVisit = Integer.MIN_VALUE;
        int positionValue = 0;
        int numberOfIterations = 1;
        Move bestMove = null;

        // timer settings
        Instant start = Instant.now();

        // searching loop
        while (true){
            if (debug){System.out.println("Current itr: " + numberOfIterations);}
            searchMCT(board, 0, debug);
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
            boolean repetition = board.isRepetition();
            board.undoMove();

            if(mctsHistory.containsKey(Pair.of(key,repetition))){
                Pair<Integer, Integer> stats = mctsHistory.get(Pair.of(key,repetition));
                visits = stats.getLeft();
                positionValue = stats.getRight();

            }
            else{
                visits = 0;
            }

            int numberOfVisit = visits;
            if (numberOfVisit > mostNumberOfVisit) {
                mostNumberOfVisit = numberOfVisit;
                bestMove = action;
            }

            if(verbose){System.out.println("|Move:" + action + " |Number of Visits: " + numberOfVisit + "|");}
        }

        if (verbose) {
            System.out.printf(
                    "Move Found: %-10s | Value: %-6d | Iterations: %-6d | Rollouts: %-6d%n",
                    bestMove, positionValue, numberOfIterations, ROLLOUT_COUNT
            );
        }

        ROLLOUT_COUNT = 0;
        return bestMove;
    }

    // This function does 1 pass of MCTS
    private int searchMCT(Board board, int ply, boolean debug){
        //positional evaluation will be the value of the position
        int positionValue;
        long boardPosition = board.getZobristKey();
        double bestUcb = -Double.MAX_VALUE;
        Move moveToExplore = null;

        // Initialize MCTS history, if it exists then extract information, this will be our parent node
        int parentNumberOfVisits;
        int parentSumValue;
        if(mctsHistory.containsKey(Pair.of(boardPosition,board.isRepetition()))){
            Pair<Integer, Integer> mctsHistValue = mctsHistory.get(Pair.of(boardPosition,board.isRepetition()));
            parentNumberOfVisits = mctsHistValue.getLeft();
            parentSumValue = mctsHistValue.getRight();
        }
        else{
            // Fill with empty values
            Pair<Integer,Integer> node = Pair.of(0,0);
            mctsHistory.put(Pair.of(boardPosition,board.isRepetition()), node);
            parentNumberOfVisits = 0;
            parentSumValue = 0;
        }

        // Termination conditions
        if (board.isRepetition() || board.isInsufficientMaterial()) {
            positionValue = 0;
            ROLLOUT_COUNT++;
        }
        else if (board.isMated()){
            positionValue = -10000 + ply;
            ROLLOUT_COUNT++;
        }
        // Bootstrap node values
        else if(parentNumberOfVisits == 0){
            // If the node has never been visited before, update its value and store it in our MCTS history
            positionValue = evaluation.positionalEvaluation(board);
        }
        // main MCTS algorithm
        else{
            // This is selection
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

                if(mctsHistory.containsKey(Pair.of(childPosition,board.isRepetition()))){
                    Pair<Integer, Integer> mctsHistValue = mctsHistory.get(Pair.of(childPosition,board.isRepetition()));
                    childNumberOfVisits = mctsHistValue.getLeft();
                    childSumValue = mctsHistValue.getRight();
                }
                // if we haven't seen this node before
                else{
                    childNumberOfVisits = 0;
                    childSumValue = 0;
                }
                board.undoMove();

                double confidenceValue;
                // force exploration of unvisited nodes, this is safer in terms of mate finding
                if (childNumberOfVisits == 0) {
                    confidenceValue = Double.MAX_VALUE;
                }
                else {
                    confidenceValue = helper.UCB(childSumValue, childNumberOfVisits, parentNumberOfVisits, 400);
                }

                if(debug){System.out.println("UCB value: " + confidenceValue);}

                if (confidenceValue > bestUcb){
                    bestUcb = confidenceValue;
                    moveToExplore = action;
                }

                if(debug){System.out.println("Move to explore: " + moveToExplore);}
            }
            // recursively calculate the position based on the move selected through argmax A of UCB
            // this is expansion
            board.doMove(moveToExplore);
            positionValue = -searchMCT(board, ply + 1, debug); // This is a bit confusing but positionValue = child value
            board.undoMove();
        }
        // store the stuff back into MCTS history
        // the evaluation is the parent evaluation + the leaf node evaluation
        // we will use the evaluation to calculate Q(s) which is simply evaluation / # the node has been visited
        mctsHistory.put(Pair.of(boardPosition,board.isRepetition()),Pair.of(parentNumberOfVisits + 1,parentSumValue + positionValue));

        return positionValue;
    }
}
