/**
 * Copyright (c) 2015, rpgtoolkit.net <help@rpgtoolkit.net>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.rpgtoolkit.editor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import net.rpgtoolkit.editor.ui.actions.ExitAction;
import net.rpgtoolkit.editor.ui.actions.NewProjectAction;
import net.rpgtoolkit.editor.ui.actions.OpenFileAction;
import net.rpgtoolkit.editor.ui.actions.OpenProjectAction;
import net.rpgtoolkit.editor.ui.actions.SaveAction;
import net.rpgtoolkit.editor.ui.resources.Icons;

/**
 *
 * @author Joshua Michael Daly
 */
public final class FileMenu extends JMenu implements ActionListener {

  private JMenu newMenu;
  private JMenuItem newProjectMenuItem;
  private JMenuItem newBoardMenuItem;

  private JMenu openMenu;
  private JMenuItem openProjectMenuItem;
  private JMenuItem openFileMenuItem;
  private JMenuItem saveMenuItem;
  private JMenuItem saveAsMenuItem;
  private JMenuItem saveAllMenuItem;
  private JMenuItem exitMenuItem;

  /**
   *
   */
  public FileMenu() {
    super("File");

    setMnemonic(KeyEvent.VK_F);

    configureFileMenu();

    add(newMenu);
    add(openMenu);
    add(new JSeparator());
    add(saveMenuItem);
    add(saveAsMenuItem);
    add(saveAllMenuItem);
    add(new JSeparator());
    add(exitMenuItem);
  }

  public JMenu getNewMenu() {
    return newMenu;
  }

  public JMenuItem getNewProjectMenuItem() {
    return newProjectMenuItem;
  }

  public JMenu getOpenMenu() {
    return openMenu;
  }

  public JMenuItem getOpenProjectMenuItem() {
    return openProjectMenuItem;
  }

  public JMenuItem getOpenFileMenuItem() {
    return openFileMenuItem;
  }

  public JMenuItem getSaveMenuItem() {
    return saveMenuItem;
  }

  public JMenuItem getSaveAsMenuItem() {
    return saveAsMenuItem;
  }

  public JMenuItem getSaveAllMenuItem() {
    return saveAllMenuItem;
  }

  public JMenuItem getExitMenuItem() {
    return exitMenuItem;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == newProjectMenuItem) {

    } else if (e.getSource() == newBoardMenuItem) {
      MainWindow.getInstance().createNewBoard();
    }
  }

  /**
   * Enable all the menu items after a project has been opened.
   */
  public void doEnableItems() {
    newBoardMenuItem.setEnabled(true);
    openFileMenuItem.setEnabled(true);
  }

  /**
   *
   */
  private void configureFileMenu() {
    configureNewSubMenu();
    configureOpenSubMenu();
    configureSaveMenuItem();
    configureSaveAsMenuItem();
    configureSaveAllMenuItem();
    configureExitMenuItem();
  }

  /**
   *
   */
  private void configureNewSubMenu() {
    configureNewProjectMenuItem();
    configureNewBoardMenuItem();

    newMenu = new JMenu("New");
    newMenu.setEnabled(true);
    newMenu.add(newProjectMenuItem);
    newMenu.add(newBoardMenuItem);
  }

  private void configureOpenSubMenu() {
    configureOpenProjectMenuItem();
    configureOpenFileMenuItem();

    openMenu = new JMenu("Open");
    openMenu.add(openProjectMenuItem);
    openMenu.add(openFileMenuItem);
  }

  private void configureNewProjectMenuItem() {
    newProjectMenuItem = new JMenuItem("New Project");
    newProjectMenuItem.setAction(new NewProjectAction());
    newProjectMenuItem.setText("New Project");
    newProjectMenuItem.setIcon(Icons.getSmallIcon("new-project"));
    newProjectMenuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
    newProjectMenuItem.setMnemonic(KeyEvent.VK_N);
    newProjectMenuItem.setEnabled(true);
  }

  private void configureNewBoardMenuItem() {
    newBoardMenuItem = new JMenuItem("New Board");
    newBoardMenuItem.setEnabled(false);
    newBoardMenuItem.addActionListener(this);
  }

  private void configureOpenProjectMenuItem() {
    openProjectMenuItem = new JMenuItem("Open Project");
    openProjectMenuItem.setAction(new OpenProjectAction());
    openProjectMenuItem.setText("Open Project");
    openProjectMenuItem.setIcon(Icons.getSmallIcon("project"));
    openProjectMenuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK
                    + ActionEvent.SHIFT_MASK));
    openProjectMenuItem.setMnemonic(KeyEvent.VK_T);
  }

  private void configureOpenFileMenuItem() {
    openFileMenuItem = new JMenuItem();
    openFileMenuItem.setAction(new OpenFileAction());
    openFileMenuItem.setText("Open File");
    openFileMenuItem.setEnabled(false);
    openFileMenuItem.setIcon(Icons.getSmallIcon("open"));
    openFileMenuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
    openFileMenuItem.setMnemonic(KeyEvent.VK_O);
  }

  private void configureSaveMenuItem() {
    saveMenuItem = new JMenuItem();
    saveMenuItem.setAction(new SaveAction());
    saveMenuItem.setText("Save");
    saveMenuItem.setIcon(Icons.getSmallIcon("save"));
    saveMenuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
    saveMenuItem.setMnemonic(KeyEvent.VK_N);
    saveMenuItem.setEnabled(false);
  }

  private void configureSaveAsMenuItem() {
    saveAsMenuItem = new JMenuItem("Save As");
    saveAsMenuItem.setMnemonic(KeyEvent.VK_A);
    saveAsMenuItem.setEnabled(false);
  }

  private void configureSaveAllMenuItem() {
    saveAllMenuItem = new JMenuItem("Save All");
    saveAllMenuItem.setIcon(Icons.getSmallIcon("save-all"));
    saveAllMenuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK
                    + ActionEvent.SHIFT_MASK));
    saveAllMenuItem.setEnabled(false);
  }

  private void configureExitMenuItem() {
    exitMenuItem = new JMenuItem();
    exitMenuItem.setAction(new ExitAction());
    exitMenuItem.setText("Exit");
    exitMenuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
  }

}
