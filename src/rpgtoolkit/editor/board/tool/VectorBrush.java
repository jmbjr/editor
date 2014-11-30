package rpgtoolkit.editor.board.tool;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import rpgtoolkit.editor.board.AbstractBoardView;
import rpgtoolkit.editor.board.BoardLayerView;
import rpgtoolkit.editor.board.types.BoardVector;

/**
 * 
 * 
 * @author Joshua Michael Daly
 */
public class VectorBrush extends AbstractBrush
{
    
    private BoardVector boardVector;
    private boolean stillDrawing;
    
    /*
     * *************************************************************************
     * Public Constructors
     * *************************************************************************
     */
    public VectorBrush()
    {
        this.boardVector = new BoardVector();
        this.stillDrawing = false;
    }
    
    
    /*
     * *************************************************************************
     * Public Getters and Setters
     * *************************************************************************
     */
    @Override
    public Shape getShape()
    {
        return this.getBounds();
    }

    @Override
    public Rectangle getBounds()
    {
        return new Rectangle(0, 0, 1, 1);
    }
    
    public BoardVector getBoardVector()
    {
        return this.boardVector;
    }
    
    public void setBoardVector(BoardVector vector)
    {
        this.boardVector = vector;
    }
    
    public boolean isDrawing()
    {
        return this.stillDrawing;
    }
    
    public void setDrawing(boolean isDrawing)
    {
        this.stillDrawing = isDrawing;
    }

    /*
     * *************************************************************************
     * Public Methods
     * *************************************************************************
     */
    @Override
    public void drawPreview(Graphics2D g2d, AbstractBoardView view)
    {
        if (this.boardVector.getPoints().size() < 1)
        {
            return;
        }
        
        Point cursor = view.getBoardEditor().getCursorLocation();
        Point lastVectorPoint = this.boardVector.getPoints()
                .get(this.boardVector.getPoints().size() - 1);
        
        g2d.drawLine(lastVectorPoint.x, lastVectorPoint.y, cursor.x, cursor.y);
    }

    @Override
    public boolean equals(Brush brush)
    {
        return brush instanceof VectorBrush &&
                ((VectorBrush) brush).boardVector.equals(this.boardVector);
    }
    
    @Override
    public Rectangle doPaint(int x, int y, Rectangle selection) throws Exception
    {
        BoardLayerView boardLayerView = this.affectedContainer.getLayer(
                this.initialLayer);
        
        super.doPaint(x, y, selection);
        
        if (boardLayerView != null)
        {
            if (!this.stillDrawing)
            {
                this.stillDrawing = true;
                this.boardVector = new BoardVector();
                this.boardVector.setLayer(this.initialLayer);

                this.affectedContainer.getLayer(this.initialLayer).
                        getLayer().getVectors().add(this.boardVector);
            }

            this.boardVector.addPoint(x, y);
            boardLayerView.getLayer().getBoard().fireBoardChanged();
        }
        
        return null;
    }
    
    public void finishVector()
    {
        if (this.boardVector.getPointCount() < 2)
        {
            this.affectedContainer.getLayer(initialLayer).getLayer()
                    .getVectors().remove(this.boardVector);
        }

        this.boardVector = new BoardVector();
        this.stillDrawing = false;
    }
    
}