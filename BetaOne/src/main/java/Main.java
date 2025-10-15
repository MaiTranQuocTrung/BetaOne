import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

public class Main {
    public static void main(String[] args){
        Engine engine = new Engine();
        Board board = new Board();
        //board.loadFromFen("r2qkbnr/ppp2ppp/2np4/4N3/2B1P3/2N5/PPPP1PPP/R1BbK2R w KQkq - 0 1");
        while(!board.isMated()){
            Move move = engine.Search(board, 5000,false, true);
            System.out.println(move);
            board.doMove(move);
            System.out.println(board);
        }
    }
}
