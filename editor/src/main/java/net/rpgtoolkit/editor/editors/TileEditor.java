/**
 * Copyright (c) 2015, rpgtoolkit.net <help@rpgtoolkit.net>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.rpgtoolkit.editor.editors;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.rpgtoolkit.common.assets.Tile;
import net.rpgtoolkit.common.assets.TileSet;

public class TileEditor extends JInternalFrame
{
    private TileSet tileSet;
    private TileCanvas canvas;
    private JSpinner tileSelector;
    private JFileChooser openBox;
    private final SpinnerListModel slm;

    /*
     * *************************************************************************
     * Public Constructors
     * *************************************************************************
     */
    public TileEditor(File fileName)
    {
        super("Tile Editor", true, true, true, true);

        this.setResizable(true);

        this.tileSet = new TileSet(fileName);
        this.slm = new SpinnerListModel(this.tileSet.getTiles());

        this.tileSelector = new JSpinner(this.slm);
        this.tileSelector.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                if (canvas.hasChanged())
                {
                    switch (JOptionPane.showInternalConfirmDialog(
                            canvas, 
                            "This Tile has changed, would you like to save the changes?", 
                            "Save Changes?", 
                            JOptionPane.YES_NO_CANCEL_OPTION))
                    {
                        case JOptionPane.NO_OPTION:
                            canvas.changeTile((Tile) tileSelector.getValue());
                            break;
                        case JOptionPane.CANCEL_OPTION:
                            break;
                        case JOptionPane.YES_OPTION:
                            tileSet.save();
                            canvas.changeTile((Tile) tileSelector.getValue());
                            break;
                    }
                }
                else
                {
                    canvas.changeTile((Tile) tileSelector.getValue());
                }
            }
        });

        this.canvas = new TileCanvas((Tile) this.tileSelector.getValue());

        this.setSize(700, 700);
        this.setLayout(new BorderLayout());
        this.add(this.canvas, BorderLayout.CENTER);
        this.add(this.tileSelector, BorderLayout.SOUTH);

        this.setVisible(true);
    }

}
