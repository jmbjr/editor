/**
 * Copyright (c) 2015, rpgtoolkit.net <help@rpgtoolkit.net>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.rpgtoolkit.editor.ui;

import net.rpgtoolkit.editor.editors.board.panels.AbstractModelPanel;
import net.rpgtoolkit.editor.editors.board.panels.BoardProgramPanel;
import net.rpgtoolkit.editor.editors.board.panels.BoardPanel;
import net.rpgtoolkit.editor.editors.board.panels.BoardVectorPanel;
import net.rpgtoolkit.editor.editors.board.panels.BoardSpritePanel;
import net.rpgtoolkit.common.assets.Board;
import net.rpgtoolkit.common.assets.BoardLight;
import net.rpgtoolkit.common.assets.BoardProgram;
import net.rpgtoolkit.common.assets.BoardSprite;
import net.rpgtoolkit.common.assets.BoardVector;

/**
 *
 *
 * @author Joshua Michael Daly
 */
public final class ModelPanelFactory {
  
  private ModelPanelFactory() {

  }

  public static AbstractModelPanel getModelPanel(Object model) {
    if (model instanceof Board) {
      return new BoardPanel((Board) model);
    } else if (model instanceof BoardVector) {
      return new BoardVectorPanel((BoardVector) model);
    } else if (model instanceof BoardProgram) {
      return new BoardProgramPanel((BoardProgram) model);
    } else if (model instanceof BoardSprite) {
      return new BoardSpritePanel((BoardSprite) model);
    } else if (model instanceof BoardLight) {

    }

    return null;
  }
}
