/**
 * Copyright (c) 2015, rpgtoolkit.net <help@rpgtoolkit.net>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.rpgtoolkit.editor.editors.board;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.rpgtoolkit.common.assets.Board;
import net.rpgtoolkit.common.assets.TilePixelOutOfRangeException;
import net.rpgtoolkit.editor.editors.BoardEditor;
import net.rpgtoolkit.editor.ui.MainWindow;

/**
 * A concrete class for drawing 2D RPG-Toolkit Boards, this is the view component. It defines the
 * actual code behind its abstract super classes drawing routines. It handles the drawing of
 * individual layers, vectors, tile based coordinates, and the grid.
 *
 * TBD: Create a grid drawing class and pull it out of here, a generic grid drawer can then be used
 * for this and the tile set viewer.
 *
 * @author Geoff Wilson
 * @author Joshua Michael Daly
 * @version 0.1
 */
public final class BoardView2D extends AbstractBoardView {

  /**
   * Default constructor.
   */
  public BoardView2D() {

  }

  /**
   * This constructor is used when creating a new board.
   *
   * @param boardEditor The parent BoardEditor for this view.
   */
  public BoardView2D(BoardEditor boardEditor) {
    super(boardEditor);
  }

  /**
   * This constructor is used when opening an existing board.
   *
   * @param board The Toolkit board that this view represents.
   * @param boardEditor The parent BoardEditor for this view.
   */
  public BoardView2D(BoardEditor boardEditor, Board board) {
    super(boardEditor, board);
  }

  /**
   * Overrides the default paintComponent method by first making a call to its super class
   * paintComponent method and then performs its own custom drawing routines.
   *
   * @param g The graphics context to draw to.
   */
  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics2D g2d = (Graphics2D) g;
    g2d.transform(affineTransform);

    try {
      paintBoard();
    } catch (TilePixelOutOfRangeException e) {

    }

    if (bufferedImage != null) {
      g.drawImage(bufferedImage, 0, 0, null);
    }

    g.dispose();
    g2d.dispose();
  }

  /**
   * Paints the board to the screen using a BufferedImage, it calls multiple sub methods which each
   * draw part of the board (if they are set to).
   *
   * @throws TilePixelOutOfRangeException Thrown if a tiles pixel value is out of the allowed range.
   */
  @Override
  protected void paintBoard() throws TilePixelOutOfRangeException {
    Graphics2D g = bufferedImage.createGraphics();

    // Draw background colour first.
    g.setColor(getDefaultBackgroudColor());
    g.fillRect(0, 0, (board.getWidth() * 32), (board.getHeight() * 32));

    paintLayers(g);
    paintSprites(g);
    paintStartPostion(g);

    // Reset an opcaity changes in the layers.
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,
            1.0f));

    if (MainWindow.getInstance().isShowGrid()) {
      paintGrid(g);
    }

    if (MainWindow.getInstance().isShowCoordinates()) {
      paintCoordinates(g);
    }

    if (boardEditor.getSelection() != null) {
      paintSelection(g);
    }

    paintCursor(g);

    if (MainWindow.getInstance().isShowVectors()) {
      paintVectors(g);
    }

    if (MainWindow.getInstance().isShowPrograms()) {
      paintPrograms(g);
    }

    paintBrushPreview(g);
  }

  /**
   * Handles the drawing of individual layers to the graphics context. It cycles through each layer
   * and calls that layers drawTiles(g) method.
   *
   * @param g The graphics context to draw on.
   */
  @Override
  protected void paintLayers(Graphics2D g) {
    ArrayList<BoardLayerView> layers = getLayerArrayList();

    for (BoardLayerView layer : layers) {
      if (layer.isVisible()) {
        try {
          layer.drawTiles(g);
        } catch (TilePixelOutOfRangeException ex) {
          Logger.getLogger(BoardView2D.class.getName()).
                  log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  /**
   * Handles the drawing of the grid on the graphics context. It draws a grid based on the boards
   * width and height in tiles.
   *
   * IMPROVEMENT: Move this functionality to an external class named "GridDrawer" this would save
   * repeating the code on the TileSet viewer and potentially elsewhere.
   *
   * @param g The graphics context to draw too.
   */
  @Override
  protected void paintGrid(Graphics2D g) {
    // Determine tile size
    Dimension tileSize = new Dimension(32, 32);

    if (tileSize.width <= 0 || tileSize.height <= 0) {
      return;
    }

    // Determine lines to draw from clipping rectangle
    Rectangle clipRectangle = new Rectangle(bufferedImage.getWidth(),
            bufferedImage.getHeight());
    int startX = (clipRectangle.x / tileSize.width * tileSize.width);
    int startY = (clipRectangle.y / tileSize.height * tileSize.height);
    int endX = (clipRectangle.x + clipRectangle.width);
    int endY = (clipRectangle.y + clipRectangle.height);

    g.setColor(getGridColor());

    for (int x = startX; x < endX; x += tileSize.width) {
      g.drawLine(x, clipRectangle.y, x, clipRectangle.y
              + clipRectangle.height - 1);
    }

    for (int y = startY; y < endY; y += tileSize.height) {
      g.drawLine(clipRectangle.x, y, clipRectangle.x
              + clipRectangle.width - 1, y);
    }
  }

  /**
   * Handles the drawing of each layers set of vectors and draws them to the graphics context. It
   * cycles through each layer and calls that layers drawVectors(g) method.
   *
   * @param g The graphics context to draw on.
   */
  @Override
  protected void paintVectors(Graphics2D g) {
    ArrayList<BoardLayerView> layers = getLayerArrayList();

    for (BoardLayerView layer : layers) {
      if (layer.isVisible()) {
        layer.drawVectors(g);
      }
    }
  }

  /**
   * Handles the drawing of each layers set of programs and draws them to the graphics context. It
   * cycles through each layer and calls that layers drawPrograms(g) method.
   *
   * @param g The graphics context to draw on.
   */
  @Override
  protected void paintPrograms(Graphics2D g) {
    ArrayList<BoardLayerView> layers = getLayerArrayList();

    for (BoardLayerView layer : layers) {
      if (layer.isVisible()) {
        layer.drawPrograms(g);
      }
    }
  }

  /**
   *
   * @param g
   */
  @Override
  protected void paintSprites(Graphics2D g) {
    ArrayList<BoardLayerView> layers = getLayerArrayList();

    for (BoardLayerView layer : layers) {
      if (layer.isIsVisible()) {
        layer.drawSprites(g);
      }
    }
  }
  
  /**
   * 
   * 
   * @param g 
   */
  @Override
  protected void paintStartPostion(Graphics2D g) {
      int x = board.getStartingPositionX();
      int y = board.getStartingPositionY();
      int length = 15;
      
      g.setColor(getDefaultStartPositionColor());
      g.drawLine(x - length, y, x + length, y);
      g.drawLine(x, y - length, x, y + length);
  }

  /**
   * Handles the drawing of the coordinates on the graphics context. It draws a coordinates based on
   * the boards width and height in tiles.
   *
   * BUG: The coordinates are being effected by the boards scaling factors, correct
   *
   * IMPROVEMENT: Consider splitting this method up into smaller pieces to make IT more
   * understandable.
   *
   * @param g The graphics context to draw on.
   */
  @Override
  protected void paintCoordinates(Graphics2D g) {
    Dimension tileSize = new Dimension(32, 32);

    if (tileSize.width <= 0 || tileSize.height <= 0) {
      return;
    }

    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Determine tile size and offset
    Font font = new Font("SansSerif", Font.PLAIN, tileSize.height / 4);
    g.setFont(font);
    FontRenderContext fontRenderContext = g.getFontRenderContext();

    g.setColor(Color.WHITE);

    // Determine area to draw from clipping rectangle
    Rectangle clipRectangle = new Rectangle(bufferedImage.getWidth(),
            bufferedImage.getHeight());
    int startX = clipRectangle.x / tileSize.width;
    int startY = clipRectangle.y / tileSize.height;
    int endX = (clipRectangle.x + clipRectangle.width) / tileSize.width
            + 1;
    int endY = (clipRectangle.y + clipRectangle.height) / tileSize.height
            + 1;

    // Draw the coordinates
    int gy = startY * tileSize.height;

    for (int y = startY; y < endY; y++) {
      int gx = startX * tileSize.width;

      for (int x = startX; x < endX; x++) {
        String coordinates = "(" + x + "," + y + ")";
        Rectangle2D textSize
                = font.getStringBounds(coordinates, fontRenderContext);

        int fx = gx + (int) ((tileSize.width - textSize.getWidth())
                / 2);
        int fy = gy + (int) ((tileSize.height + textSize.getHeight())
                / 2);

        g.drawString(coordinates, fx, fy);
        gx += tileSize.width;
      }

      gy += tileSize.height;
    }
  }

  /**
   *
   *
   * @param g
   */
  @Override
  protected void paintSelection(Graphics2D g) {
    int tileWidth = MainWindow.TILE_SIZE;
    int tileHeight = tileWidth;

    Rectangle selection = boardEditor.getSelection();

    g.setColor(new Color(100, 100, 255));
    g.drawRect(
            selection.x * tileWidth,
            selection.y * tileHeight,
            (selection.width + 1) * tileWidth,
            (selection.height + 1) * tileHeight);
    g.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_ATOP, 0.2f));
    g.fillRect(
            selection.x * tileWidth + 1,
            selection.y * tileHeight + 1,
            (selection.width + 1) * tileWidth - 1,
            (selection.height + 1) * tileHeight - 1);
  }

  /**
   *
   *
   * @param g
   */
  @Override
  protected void paintCursor(Graphics2D g) {
    Rectangle cursor = MainWindow.getInstance().getCurrentBrush().getBounds();
    Point selection = boardEditor.getCursorTileLocation();

    int tileWidth = MainWindow.TILE_SIZE;
    int tileHeight = MainWindow.TILE_SIZE;

    int centerX = (selection.x * tileWidth)
            - (((int) cursor.getWidth() / 2) * tileWidth);
    int centerY = (selection.y * tileHeight)
            - (((int) cursor.getHeight() / 2) * tileHeight);

    g.setColor(new Color(100, 100, 255));
    g.drawRect(
            centerX,
            centerY,
            ((int) cursor.getWidth()) * tileWidth,
            ((int) cursor.getHeight()) * tileHeight);
    g.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_ATOP, 0.2f));
    g.fillRect(
            centerX,
            centerY,
            ((int) cursor.getWidth()) * tileWidth,
            ((int) cursor.getHeight()) * tileHeight);
  }

  @Override
  protected void paintBrushPreview(Graphics2D g) {
    MainWindow.getInstance().getCurrentBrush().drawPreview(g, this);
  }
}
