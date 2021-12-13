package Chess;

import Chess.ChessPiece.*;
import java.util.ArrayList;

public class ChessGame {

    // TODO det borde finnas en metod som tar en tuple som argument och returnar en Piece. Detta hade gjort att mycket
    //  kodduplicering hade kunnat undvikas.
    //  Sättet att få en Piece nu: tuple -> tile -> piece
    //  Borde finnas en funktion sådan att: tuple -> piece.

    // TODO borde även finnas en funktion som endast flyttar en Piece (alltså flyttar en piece och gör rutan den stod
    //  på tom). Detta hade också hjälpt undvika kodduplicering och göra koden mer lättläst.

    // TODO borde även finnas en funktion för att revert temporary move
    //  toTile.setPiece(toPiece);
    //  fromTile.setPiece(piece);
    private final ChessBoard board;
    private boolean isFinished;
    private PieceColor currentPlayer; // The current player, uses Enumeration PieceColor, can either be black or white
    // TODO borde ha bättre namngivning

    public ChessGame(){
        board = new ChessBoard(); // The chess board
        currentPlayer = PieceColor.White; // White starts
        isFinished = false;
    }

    /**
     * @return returns true if move was played, false if move was illegal
     */
    public boolean playMove(Tuple from, Tuple to){
        if(isValidMove(from, to, false)) { //Checks if it is a valid move
            Tile fromTile = board.getBoardArray()[from.Y()][from.X()]; // Gets the tile to move from
            ChessPiece pieceToMove = fromTile.getPiece(); // Gets the piece on the tile to move from

            Tile toTile = board.getBoardArray()[to.Y()][to.X()]; // Gets the tile to move to
            toTile.setPiece(pieceToMove); // Moves the piece to the tile

            fromTile.empty(); // Makes the form tile empty
            endTurn(); // End turn
            return true;
        } else {
            return false;
        }
    }
    //TODO bryter mot command quarry separation principle. Returnar true om den lyckades flytta pjäsen. Returnar false
    // om den inte lyckades. Vi vill att metoder antingen ska returna nåt ELLER göra nåt. Den här gör båda.
    // En lösning skulle kunna vara att throwa ett exception om det inte får att flytta pjäsen, och låta client-koden
    // (Program-klassen) ta hand om den. Detta kan göras genom att ha en if-stats som kör isValidMove, och om den
    // returnar false så throwas ett exception

    // Switches current player
    private void endTurn(){
        currentPlayer = (currentPlayer == PieceColor.White)
                ? PieceColor.Black
                : PieceColor.White;
    }

    /**
     * @param from the position from which the player tries to move from
     * @param to the position the player tries to move to
     * @param hypothetical if the move is hypothetical, we disregard if it sets the from player check
     * @return a boolean indicating whether the move is valid or not
     */
    private boolean isValidMove(Tuple from, Tuple to, boolean hypothetical){
        Tile fromTile = board.getTileFromTuple(from);
        Tile toTile = board.getTileFromTuple(to);
        ChessPiece fromPiece = fromTile.getPiece();
        ChessPiece toPiece = toTile.getPiece();

        if (fromPiece == null){ // Om rutan man försöker flytta från är tom returnar den false
            return false;
        } else if (fromPiece.getColor() != currentPlayer) { // Om rutan man försöker flytta från är ockuperad av motståndarens pjäs returnar den false
            return false;
        } else if (toPiece != null && toPiece.getColor() == currentPlayer) { // Om rutan man försöker flytta till är ockuperad av sin egna pjäs returnar den false
            return false;
        } else if (isValidMoveForPiece(from, to)){
            //if hypothetical and valid, return true
            if(hypothetical) return true;

            //temporarily play the move to see if it makes us check
            toTile.setPiece(fromPiece);
            fromTile.empty();
            if (isKingCheck(currentPlayer)){//check that move doesn't put oneself in check
                toTile.setPiece(toPiece);
                fromTile.setPiece(fromPiece);
                return false;
            } //TODO bryt till en egen funktion

            //if mate, finish game
            if (isColorCheckMate(ChessPiece.opponent(currentPlayer)))
                isFinished = true;
            //TODO detta borde inte hända här, utan i playMove. Detta bryter mot command/quarry separation principle

            //revert temporary move
            toTile.setPiece(toPiece);
            fromTile.setPiece(fromPiece);

            return true;
        }
        return false;
    }
    // TODO 1. if-satsen som kollar om spelet är slut borde inte vara här då denna metod endast ska vara ansvarig för
    //  att kolla om det är ett valid move och returna true eller false. Om den även sätter isFinished så bryter den mot
    //  command command/quarry separation principle.
    //  2. Parametern "hypothetical" används endast när man använder canOpponentTakeLocation. Man har gjort det såhär
    //  för att undvika kodduplicering, (dom 3 första if-conditions är de ända man är ute efter, resten behövs inte).
    //  Detta är okej då det inte bryter mot någon princip, men man skulle eventuellt bryta ner funktionen med
    //  funktionell nedbrytning och sen låta canOpponentTakeLocation använda den funktionen som den behöver. Då blir det
    //  enklare att förstå.

    /* -----------------------------------------------------------------------------------------------------------*/

    // Checks whether a given move from one tuple to another is valid.
    private boolean isValidMoveForPiece(Tuple from, Tuple to){
        ChessPiece fromPiece = board.getTileFromTuple(from).getPiece(); //TODO denna bryter mot Law of Demeter
        boolean repeatableMoves = fromPiece.hasRepeatableMoves();

        return repeatableMoves
                ? isValidMoveForPieceRepeatable(from, to)
                : isValidMoveForPieceNonRepeatable(from, to);
    }
    // TODO tror inte det är så bra att repeatbleMoves används här då båda metoderna som kan kallas här har lite kodduplicering.

    // Check whether a given move is valid for a piece without repeatable moves. (pjäser som int har exakt förstämda moves, tex drottning)
    private boolean isValidMoveForPieceRepeatable(Tuple from, Tuple to) {
        ChessPiece fromPiece = board.getTileFromTuple(from).getPiece(); //TODO denna bryter mot Law of Demeter
        Move[] validMoves = fromPiece.getMoves();
        int xMove = to.X() - from.X();
        int yMove = to.Y() - from.Y();

        // 7 används här för det är den längsta sträckan en pjäs kan röra på sig
        for(int i = 1; i <= 7; i++){
            for(Move move : validMoves) {
                //generally check for possible move
                if (move.x * i == xMove && move.y * i == yMove) {
                    //if move is generally valid - check if path is valid up till i
                    for (int j = 1; j <= i; j++){
                        Tile tile = board.getTileFromTuple(new Tuple(from.X() + move.x * j, from.Y() +move.y * j));
                        //if passing through non empty tile return false
                        if (j != i && !tile.isEmpty())
                            return false;
                        //if last move and toTile is empty or holds opponents piece, return true
                        if (j == i && (tile.isEmpty() || tile.getPiece().getColor() != currentPlayer))
                            return true;
                    }
                }
            }
        }
        return false;
    }
    // TODO skulle eventuellt bryta ner denna med funktionell nedbrytning

    // Check whether a given move is valid for a piece with repeatable moves. (pjäser som har exakt förstämda moves, tex häst)
    private boolean isValidMoveForPieceNonRepeatable(Tuple from, Tuple to){
        ChessPiece fromPiece = board.getTileFromTuple(from).getPiece(); //TODO denna bryter mot Law of Demeter
        Move[] validMoves = fromPiece.getMoves();
        Tile toTile = board.getTileFromTuple(to);
        int xMove = to.X() - from.X();
        int yMove = to.Y() - from.Y();

        for (Move move : validMoves) {
            if (move.x == xMove && move.y == yMove) {
                if (move.onTakeOnly){//if move is only legal on take (pawns)
                    if (toTile.isEmpty()) return false;

                    ChessPiece toPiece = toTile.getPiece();
                    return fromPiece.getColor() != toPiece.getColor();//if different color, valid move

                    //handling first move only for pawns - they should not have moved yet
                } else if (move.firstMoveOnly) {
                    return toTile.isEmpty() && isFirstMoveForPawn(from, board);
                } else {
                    return toTile.isEmpty();
                }
            }
        }
        return false;
    }
    // TODO denna och den tidigare metoden är ganska lika, och har lite kodduplicering (tex moveX). Man skulle nog kunna
    //  ha en del i samma metod och sen ha en if-sats för repeatableMoves där de skiljer sig åt.

    // Determine wheter the Pawn at 'from' on 'board' has moved yet.
    public boolean isFirstMoveForPawn(Tuple from, ChessBoard board){
        Tile tile = board.getTileFromTuple(from);
        if (tile.isEmpty() || tile.getPiece().getPieceType() != PieceType.Pawn) { //TODO denna bryter mot Law of Demeter
            return false;
        } else {
            PieceColor color = tile.getPiece().getColor();
            return (color == PieceColor.White)
                    ? from.Y() == 6
                    : from.Y() == 1;
        }
    }
    // TODO ett lättare sätt för att avgöra om bonden har flyttats vore att ha en boolean hasMoved i pawn (eller i alla)
    //  som startar som false och som sen ändras till true. Då slipper man hela den här uträkningen och metoden.

    /* -----------------------------------------------------------------------------------------------------------*/

    //Kollar så man inte sätter sig själv i schack.
    private boolean isKingCheck(PieceColor kingColor){
        Tuple kingLocation = board.getKingLocation(kingColor);
        return canOpponentTakeLocation(kingLocation, kingColor);
    }

    private boolean canOpponentTakeLocation(Tuple location, PieceColor color){
        PieceColor opponentColor = ChessPiece.opponent(color);
        Tuple[] piecesLocation = board.getAllPiecesLocationForColor(opponentColor);

        for(Tuple fromTuple: piecesLocation) {
            if (isValidMove(fromTuple, location, true))
                return true;
        }
        return false;
    }

    /* -----------------------------------------------------------------------------------------------------------*/

    private boolean isColorCheckMate(PieceColor color){
        if(!isKingCheck(color)) return false;//if not check, then we're not mate
        return !isCheckPreventable(color);
    }

    // Function that checks if any piece can prevent check for the given color
    // This includes whether the King can move out of check himself.
    private boolean isCheckPreventable(PieceColor color){
        boolean canPreventCheck = false;
        Tuple[] locations = board.getAllPiecesLocationForColor(color);

        for(Tuple location : locations){
            Tile fromTile = board.getTileFromTuple(location);
            ChessPiece piece = fromTile.getPiece();
            Tuple[] possibleMoves = validMovesForPiece(piece, location);

            for(Tuple newLocation : possibleMoves){
                Tile toTile = board.getTileFromTuple(newLocation);
                ChessPiece toPiece = toTile.getPiece();

                //temporarily play the move to see if it makes us check
                toTile.setPiece(piece);
                fromTile.empty();

                //if we're no longer check
                if (!isKingCheck(color)){
                    canPreventCheck = true;
                }
                //revert temporary move
                toTile.setPiece(toPiece);
                fromTile.setPiece(piece);
                if(canPreventCheck){ // early out
                    System.out.printf("Prevented with from:" + fromTile + ", to: " + toTile);
                    return canPreventCheck;
                }
            }
        }
        return canPreventCheck;
    }
    // TODO funktionell nedbrytning skulle vara bra här.

    // A utility function that gets all the possible moves for a piece, with illegal ones removed.
    // NOTICE: Does not check for counter-check when evaluating legality.
    //         This means it mostly checks if it is a legal move for the piece in terms
    //         of ensuring its not taking one of its own, and within its 'possibleMoves'.
    // Returns the Tuples representing the Tiles to which the given piece
    // can legally move.
    private Tuple[] validMovesForPiece(ChessPiece piece, Tuple currentLocation){
            return piece.hasRepeatableMoves()
                ? validMovesRepeatable(piece, currentLocation)
                : validMovesNonRepeatable(piece, currentLocation);
    }
    // TODO har samma problem som isValidMoveForPiece. validMovesRepeatable och validMovesNonRepeatable har kodduplicering,
    //  det som skiljer dom åt är vad som händer i for(Move move: moves). Så ha en if-sats som avgör om piece har repeatable
    //  moves och välj sedan efter det vilken "implementation" av for-loopen som ska köras (i form av 2 olika metoder).

    // Returns the Tuples representing the Tiles to which the given piece
    // can legally move.
    private Tuple[] validMovesRepeatable(ChessPiece piece, Tuple currentLocation) {
        Move[] moves = piece.getMoves();
        ArrayList<Tuple> possibleMoves = new ArrayList<>();

        for(Move move: moves){
            for(int i = 1; i < 7; i++){
                int newX = currentLocation.X() + move.x * i;
                int newY = currentLocation.Y() + move.y * i;
                if (newX < 0 || newX > 7 || newY < 0 || newY > 7) break;

                Tuple toLocation = new Tuple(newX, newY);
                Tile tile = board.getTileFromTuple(toLocation);
                if (tile.isEmpty()) {
                    possibleMoves.add(toLocation);
                } else {
                    if (tile.getPiece().getColor() != piece.getColor())
                        possibleMoves.add(toLocation);
                    break;
                }
            }
        }
        return possibleMoves.toArray(new Tuple[0]);
    }

    private Tuple[] validMovesNonRepeatable(ChessPiece piece, Tuple currentLocation) {
        Move[] moves = piece.getMoves();
        ArrayList<Tuple> possibleMoves = new ArrayList<>();

        for(Move move: moves){
            int currentX = currentLocation.X();
            int currentY = currentLocation.Y();
            int newX = currentX + move.x;
            int newY = currentY + move.y;
            if (newX < 0 || newX > 7 || newY < 0 || newY > 7) continue;
            Tuple newLocation = new Tuple(newX,newY);
            if (isValidMoveForPiece(currentLocation, newLocation)) possibleMoves.add(newLocation);
        }
        return possibleMoves.toArray(new Tuple[0]);
    }

    /* --------------------------------- GETTERS --------------------------------------------------------------------*/

    /**
     * @return returns the current ChessBoard associated with the game.
     */
    public ChessBoard getBoard(){
        return board;
    }

    /**
     * @return returns whether the given ChessGame is finished.
     */
    public boolean isFinished(){
        return isFinished;
    }
}
