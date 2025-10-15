import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import java.util.ArrayList;
import java.util.List;

public class Helper {
    Evaluation evaluation = new Evaluation();

    // Is the move a capture move
    public boolean isCapture(Board board, Move move){
        Square origin = move.getFrom();
        // What is the square the piece is moving to
        Square destination = move.getTo();
        // get piece at destination square
        Piece destinationPiece = board.getPiece(destination);
        // get piece at origin
        Piece originPiece = board.getPiece(origin);
        // If there is nothing at that destination square
        return originPiece != Piece.NONE && destinationPiece != Piece.NONE && destinationPiece.getPieceSide() != originPiece.getPieceSide();
    }

    public List<Move> captureMoveList(Board board, List<Move> moveList){
        List<Move> captureMove = new ArrayList<>();
        for (Move action : moveList){
            if(isCapture(board, action)){
                captureMove.add(action);
            }
        }
        return captureMove;
    }
}
