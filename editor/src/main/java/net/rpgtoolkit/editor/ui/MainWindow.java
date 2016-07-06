/**
 * Copyright (c) 2015, rpgtoolkit.net <help@rpgtoolkit.net>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.rpgtoolkit.editor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.rpgtoolkit.common.assets.AbstractAsset;
import net.rpgtoolkit.common.assets.Animation;
import net.rpgtoolkit.common.assets.AssetDescriptor;
import net.rpgtoolkit.common.assets.AssetException;
import net.rpgtoolkit.common.assets.AssetHandle;
import net.rpgtoolkit.common.assets.AssetManager;
import net.rpgtoolkit.common.assets.BasicType;
import net.rpgtoolkit.common.assets.Board;
import net.rpgtoolkit.common.assets.Enemy;
import net.rpgtoolkit.common.assets.Player;
import net.rpgtoolkit.common.assets.Project;
import net.rpgtoolkit.common.assets.SpecialMove;
import net.rpgtoolkit.common.assets.Tile;
import net.rpgtoolkit.common.assets.TileSet;
import net.rpgtoolkit.common.assets.files.FileAssetHandleResolver;
import net.rpgtoolkit.common.assets.serialization.JsonBoardSerializer;
import net.rpgtoolkit.common.assets.serialization.JsonProjectSerializer;
import net.rpgtoolkit.common.assets.serialization.JsonSpecialMoveSerializer;
import net.rpgtoolkit.common.assets.serialization.legacy.LegacyAnimatedTileSerializer;
import net.rpgtoolkit.common.assets.serialization.legacy.LegacyItemSerializer;
import net.rpgtoolkit.editor.editors.AnimationEditor;
import net.rpgtoolkit.editor.editors.BoardEditor;
import net.rpgtoolkit.editor.editors.CharacterEditor;
import net.rpgtoolkit.editor.editors.board.AbstractBrush;
import net.rpgtoolkit.editor.editors.board.BucketBrush;
import net.rpgtoolkit.editor.editors.board.CustomBrush;
import net.rpgtoolkit.editor.editors.board.ShapeBrush;
import net.rpgtoolkit.editor.editors.board.VectorBrush;
import net.rpgtoolkit.editor.editors.EnemyEditor;
import net.rpgtoolkit.editor.editors.ProjectEditor;
import net.rpgtoolkit.editor.editors.SpecialMoveEditor;
import net.rpgtoolkit.editor.editors.TileEditor;
import net.rpgtoolkit.editor.editors.TileSelectionEvent;
import net.rpgtoolkit.editor.ui.listeners.TileSelectionListener;
import net.rpgtoolkit.editor.editors.TileRegionSelectionEvent;
import net.rpgtoolkit.editor.editors.board.NewBoardDialog;
import net.rpgtoolkit.editor.ui.resources.Icons;
import net.rpgtoolkit.editor.editors.board.ProgramBrush;
import net.rpgtoolkit.common.utilities.PropertiesSingleton;
import net.rpgtoolkit.editor.utilities.FileTools;

/**
 * Currently opening TileSets, tiles, programs, boards, animations, characters etc.
 *
 * @author Geoff Wilson
 * @author Joshua Michael Daly
 */
public class MainWindow extends JFrame implements InternalFrameListener {

  // Singleton.
  private static final MainWindow instance = new MainWindow();

  public static final int TILE_SIZE = 32;

  private final JDesktopPane desktopPane;

  private final MainMenuBar menuBar;
  private final MainToolBar toolBar;

  private final JPanel toolboxPanel;
  private final JTabbedPane upperTabbedPane;
  private final JTabbedPane lowerTabbedPane;
  private final ProjectPanel projectPanel;
  private final TileSetTabbedPane tileSetPanel;
  private final PropertiesPanel propertiesPanel;
  private final LayerPanel layerPanel;

  private JFileChooser fileChooser;
  private final String workingDir = PropertiesSingleton.getProjectsDirectory();

  private final JScrollPane debugScrollPane;
  private final JTextArea debugLog;

  // Project Related.
  private Project activeProject;

  // Board Related.
  private boolean showGrid;
  private boolean showVectors;
  private boolean showPrograms;
  private boolean showCoordinates;
  private boolean snapToGrid;
  private AbstractBrush currentBrush;
  private Tile lastSelectedTile;

  // Listeners.
  private final TileSetSelectionListener tileSetSelectionListener;

  private MainWindow() {
    super("RPG Toolkit 4.0");

    this.desktopPane = new JDesktopPane();
    this.desktopPane.setBackground(Color.LIGHT_GRAY);
    this.desktopPane.setDesktopManager(new ToolkitDesktopManager(this));

    this.projectPanel = new ProjectPanel();
    this.tileSetPanel = new TileSetTabbedPane();
    this.upperTabbedPane = new JTabbedPane();
    this.upperTabbedPane.addTab("Project", this.projectPanel);
    this.upperTabbedPane.addTab("Tileset", this.tileSetPanel);

    this.propertiesPanel = new PropertiesPanel();
    this.layerPanel = new LayerPanel();
    this.lowerTabbedPane = new JTabbedPane();
    this.lowerTabbedPane.addTab("Properties", this.propertiesPanel);
    this.lowerTabbedPane.addTab("Layers", this.layerPanel);

    this.toolboxPanel = new JPanel(new GridLayout(2, 1));
    this.toolboxPanel.setPreferredSize(new Dimension(352, 0));
    this.toolboxPanel.add(this.upperTabbedPane);
    this.toolboxPanel.add(this.lowerTabbedPane);

    // Application icon.
    this.setIconImage(Icons.getLargeIcon("application").getImage());

    this.debugLog = new JTextArea("Debug Messages:\n\n");
    this.debugLog.setEditable(false);
    this.debugLog.setRows(6);
    this.debugLog.setBackground(Color.WHITE);

    this.debugScrollPane = new JScrollPane(this.debugLog);
    this.debugScrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    this.registerResolvers();
    this.registerSerializers();

    this.fileChooser = new JFileChooser();
    this.fileChooser.setCurrentDirectory(new File(this.workingDir));

    this.menuBar = new MainMenuBar(this);
    this.toolBar = new MainToolBar();

    this.currentBrush = new ShapeBrush();
    ((ShapeBrush) this.currentBrush).makeRectangleBrush(new Rectangle(0, 0, 1, 1));

    this.lastSelectedTile = new Tile();

    this.tileSetSelectionListener = new TileSetSelectionListener();

    JPanel parent = new JPanel(new BorderLayout());
    parent.add(this.desktopPane, BorderLayout.CENTER);
    parent.add(this.debugScrollPane, BorderLayout.SOUTH);

    this.setLayout(new BorderLayout());
    this.add(this.toolBar, BorderLayout.NORTH);
    this.add(parent, BorderLayout.CENTER);
    this.add(this.toolboxPanel, BorderLayout.EAST);

    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    this.setSize(new Dimension(1024, 768));
    this.setLocationByPlatform(true);
    this.setJMenuBar(this.menuBar);
  }

  public static MainWindow getInstance() {
    return instance;
  }

  public JDesktopPane getDesktopPane() {
    return this.desktopPane;
  }

  public boolean isShowGrid() {
    return showGrid;
  }

  public void setShowGrid(boolean isShowGrid) {
    this.showGrid = isShowGrid;
  }

  public boolean isShowVectors() {
    return showVectors;
  }

  public void setShowVectors(boolean showVectors) {
    this.showVectors = showVectors;
  }

  public boolean isShowPrograms() {
    return showPrograms;
  }

  public void setShowPrograms(boolean showPrograms) {
    this.showPrograms = showPrograms;
  }

  public boolean isShowCoordinates() {
    return showCoordinates;
  }

  public void setShowCoordinates(boolean isShowCoordinates) {
    this.showCoordinates = isShowCoordinates;
  }

  public boolean isSnapToGrid() {
    return snapToGrid;
  }

  public void setSnapToGrid(boolean snapToGrid) {
    this.snapToGrid = snapToGrid;
  }

  public AbstractBrush getCurrentBrush() {
    return this.currentBrush;
  }

  public void setCurrentBrush(AbstractBrush brush) {
    this.currentBrush = brush;
  }

  public Tile getLastSelectedTile() {
    return this.lastSelectedTile;
  }

  public MainMenuBar getMainMenuBar() {
    return this.menuBar;
  }

  public MainToolBar getMainToolBar() {
    return this.toolBar;
  }

  public PropertiesPanel getPropertiesPanel() {
    return this.propertiesPanel;
  }

  public BoardEditor getCurrentBoardEditor() {
    if (this.desktopPane.getSelectedFrame() instanceof BoardEditor) {
      return (BoardEditor) this.desktopPane.getSelectedFrame();
    }

    return null;
  }

  public JFileChooser getFileChooser() {
    return this.fileChooser;
  }

  public Project getActiveProject() {
    return activeProject;
  }

  public TileSelectionListener getTileSetSelectionListener() {
    return tileSetSelectionListener;
  }

  @Override
  public void internalFrameOpened(InternalFrameEvent e) {
    if (e.getInternalFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) e.getInternalFrame();

      upperTabbedPane.setSelectedComponent(tileSetPanel);
      lowerTabbedPane.setSelectedComponent(layerPanel);
      propertiesPanel.setModel(editor.getBoard());
    }
  }

  @Override
  public void internalFrameClosing(InternalFrameEvent e) {

  }

  @Override
  public void internalFrameClosed(InternalFrameEvent e) {

  }

  @Override
  public void internalFrameIconified(InternalFrameEvent e) {

  }

  @Override
  public void internalFrameDeiconified(InternalFrameEvent e) {

  }

  @Override
  public void internalFrameActivated(InternalFrameEvent e) {
    if (e.getInternalFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) e.getInternalFrame();
      this.layerPanel.setBoardView(editor.getBoardView());

      if (editor.getSelectedObject() != null) {
        this.propertiesPanel.setModel(editor.getSelectedObject());
      } else {
        this.propertiesPanel.setModel(editor.getBoard());
      }
    }
  }

  @Override
  public void internalFrameDeactivated(InternalFrameEvent e) {
    if (e.getInternalFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) e.getInternalFrame();

      if (this.layerPanel.getBoardView().equals(editor.getBoardView())) {
        this.layerPanel.clearTable();
      }

      if (this.propertiesPanel.getModel() == editor.getSelectedObject()
              || propertiesPanel.getModel() == editor.getBoard()) {
        this.propertiesPanel.setModel(null);
      }

      // So we do not end up drawing the vector or program on the other 
      // board after it has been deactivated.
      if (this.currentBrush instanceof VectorBrush
              || this.currentBrush instanceof ProgramBrush) {
        VectorBrush brush = (VectorBrush) this.currentBrush;

        if (brush.isDrawing() && brush.getBoardVector() != null) {
          brush.finish();
        }
      }
    }
  }

  private void registerResolvers() {
    AssetManager.getInstance().registerResolver(new FileAssetHandleResolver());
  }

  private void registerSerializers() {
    AssetManager assetManager = AssetManager.getInstance();
    assetManager.registerSerializer(new LegacyAnimatedTileSerializer());
    assetManager.registerSerializer(new LegacyItemSerializer());
    assetManager.registerSerializer(new JsonBoardSerializer());
    assetManager.registerSerializer(new JsonProjectSerializer());
    assetManager.registerSerializer(new JsonSpecialMoveSerializer());
  }

  public void openProject() {
    this.fileChooser.resetChoosableFileFilters();
    FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Toolkit Project", new String[]{"gam", "json"});
    this.fileChooser.setFileFilter(filter);

    File mainFolder = new File(this.workingDir + "/"
            + PropertiesSingleton.getProperty("toolkit.directory.main"));

    if (mainFolder.exists()) {
      this.fileChooser.setCurrentDirectory(mainFolder);
    }

    if (this.fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      String fileName = this.fileChooser.getSelectedFile().getName().
              substring(0, this.fileChooser.getSelectedFile().
                      getName().indexOf('.'));
      System.setProperty("project.path",
              this.fileChooser.getCurrentDirectory().getParent()
              + File.separator
              + PropertiesSingleton.getProperty("toolkit.directory.game")
              + File.separator
              + fileName + File.separator);

      if (fileChooser.getSelectedFile().getName().endsWith(".gam")) {
        try {
        AssetHandle handle = AssetManager.getInstance().deserialize(
                new AssetDescriptor(fileChooser.getSelectedFile().toURI()));
        activeProject = (Project) handle.getAsset();
        } catch (IOException | AssetException ex) {
          Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      
      setupProject();
    }
  }

  public void createNewProject() {
    String projectName = JOptionPane.showInputDialog(this,
            "Project Name:",
            "Create Project",
            JOptionPane.QUESTION_MESSAGE);

    if (projectName != null) {
      boolean result = FileTools.createDirectoryStructure(
              PropertiesSingleton.getProjectsDirectory(), projectName);

      if (result) {
        Project project = new Project(null,PropertiesSingleton.getProjectsDirectory() + File.separator
                + PropertiesSingleton.getProperty("toolkit.directory.main"), projectName);

        try {
          // Write out new project file.
          AssetManager.getInstance().serialize(AssetManager.getInstance().getHandle(project));
          System.setProperty("project.path",
                  System.getProperty("user.home")
                  + File.separator
                  + PropertiesSingleton.getProperty("toolkit.directory.projects")
                  + File.separator
                  + PropertiesSingleton.getProperty("toolkit.directory.game")
                  + File.separator
                  + projectName + File.separator);

          activeProject = project;
          setupProject();
        } catch (IOException | AssetException ex) {
          Logger.getLogger(Board.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  public void primeFileChooser() {
    if (this.activeProject != null) {
      File projectPath = new File(System.getProperty("project.path"));

      this.fileChooser = new JFileChooser(
              new SingleRootFileSystemView(projectPath));

      FileNameExtensionFilter filter = new FileNameExtensionFilter(
              "Toolkit Files", this.getTKFileExtensions());
      this.fileChooser.setFileFilter(filter);

      if (projectPath.exists()) {
        this.fileChooser.setCurrentDirectory(projectPath);
      }
    }
  }

  public void openFile() {
    this.primeFileChooser();

    if (this.fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      this.checkFileExtension(this.fileChooser.getSelectedFile());
    }
  }

  private String[] getTKFileExtensions() {
    return new String[]{
      "brd", "ene", "tem", "itm", "anm", "prg", "tst", "spc", "json"
    };
  }

  public void checkFileExtension(File file) {
    String fileName = file.getName().toLowerCase();

    if (fileName.endsWith(".anm")) {
      this.openAnimation();
    } else if (fileName.endsWith(".brd") || fileName.endsWith(".brd.json")) {
      this.openBoard();
    } else if (fileName.endsWith(".ene")) {
      this.openEnemy();
    } else if (fileName.endsWith(".tem")) {
      this.openCharacter();
    } else if (fileName.endsWith(".prg")) {

    } else if (fileName.endsWith(".tst")) {
      this.openTileset();
    } else if (fileName.endsWith(".spc") || fileName.endsWith(".spc.json")) {
      this.openSpecialMove();
    }
  }

  /**
   * Creates an animation editor window for modifying the specified animation file.
   */
  public void openAnimation() {
    Animation animation = new Animation(fileChooser.getSelectedFile());
    AnimationEditor animationEditor = new AnimationEditor(animation);
    desktopPane.add(animationEditor);

    this.selectToolkitWindow(animationEditor);
  }

  public void createNewBoard() {
    NewBoardDialog dialog = new NewBoardDialog();
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);

    if (dialog.getValue() != null) {
      BoardEditor boardEditor = new BoardEditor("Untitled", dialog.getValue()[0], dialog.getValue()[1]);
      boardEditor.addInternalFrameListener(this);
      boardEditor.setVisible(true);
      boardEditor.toFront();

      this.desktopPane.add(boardEditor);
      this.selectToolkitWindow(boardEditor);
    }
  }

  public void openBoard() {
    try {
      BoardEditor boardEditor;
      File selectedFile = fileChooser.getSelectedFile();

      if (selectedFile.canRead()) {
        Board board;

        AssetHandle handle = AssetManager.getInstance().deserialize(
                new AssetDescriptor(fileChooser.getSelectedFile().toURI()));
        board = (Board) handle.getAsset();

        boardEditor = new BoardEditor(board);
      } else {
        boardEditor = new BoardEditor();
      }

      boardEditor.addInternalFrameListener(this);
      boardEditor.setVisible(true);
      boardEditor.toFront();

      desktopPane.add(boardEditor);
      selectToolkitWindow(boardEditor);
    } catch (IOException | AssetException ex) {
      Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Creates an animation editor window for modifying the specified animation file.
   */
  public void openEnemy() {
    AssetDescriptor uriFile = new AssetDescriptor(
            fileChooser.getSelectedFile().toURI());
    Enemy enemy = new Enemy(uriFile);
    EnemyEditor enemyEditor = new EnemyEditor(enemy);
    desktopPane.add(enemyEditor);

    this.selectToolkitWindow(enemyEditor);
  }

  /**
   * Creates a character editor window for modifying the specified character file.
   */
  public void openCharacter() {
    System.out.println("openCharacter()");
    AssetDescriptor uriFile = new AssetDescriptor(
        fileChooser.getSelectedFile().toURI());
    Player player = new Player(uriFile);
    CharacterEditor chEditor = new CharacterEditor(player);
    desktopPane.add(chEditor);

    this.selectToolkitWindow(chEditor);
  }

  /**
   * Creates a TileSet editor window for modifying the specified TileSet.
   */
  public void openTile() {
    TileEditor testTileEditor = new TileEditor(
            fileChooser.getSelectedFile());
    desktopPane.add(testTileEditor);
  }

  public void openTileset() {
    TileSet tileSet = new TileSet(fileChooser.getSelectedFile());
    tileSetPanel.addTileSet(tileSet);
  }

  public void openSpecialMove() {
    try {
      SpecialMoveEditor sMoveEditor;
      if (fileChooser.getSelectedFile().canRead()) {
        AssetHandle handle = AssetManager.getInstance().deserialize(
                new AssetDescriptor(fileChooser.getSelectedFile().toURI()));
        SpecialMove move = (SpecialMove) handle.getAsset();
        sMoveEditor = new SpecialMoveEditor(move);
      } else {
        sMoveEditor = new SpecialMoveEditor();
      }
      desktopPane.add(sMoveEditor);
      this.selectToolkitWindow(sMoveEditor);
    } catch (IOException | AssetException ex) {
      Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public String getTypeSubdirectory(Class<? extends BasicType> type) {
    switch (type.getSimpleName()) {
      case "Animation":
        return PropertiesSingleton.getProperty("toolkit.directory.misc");
      case "Board":
        return PropertiesSingleton.getProperty("toolkit.directory.board");
      case "Enemy":
        return PropertiesSingleton.getProperty("toolkit.directory.enemy");
      case "Item":
        return PropertiesSingleton.getProperty("toolkit.directory.item");
      case "Player":
        return PropertiesSingleton.getProperty("toolkit.directory.character");
      case "Program":
        return PropertiesSingleton.getProperty("toolkit.directory.program");
      case "Project":
        return PropertiesSingleton.getProperty("toolkit.directory.main");
      case "StatusEffect":
        return PropertiesSingleton.getProperty("toolkit.directory.statuseffect");
      case "SpecialMove":
        return PropertiesSingleton.getProperty("toolkit.directory.specialmove");
      case "Tileset":
        return PropertiesSingleton.getProperty("toolkit.directory.tileset");
      default:
        return "";
    }
  }

  public String getTypeFilterDescription(Class<? extends BasicType> type) {
    switch (type.getSimpleName()) {
      case "Animation":
        return "Animations";
      case "Board":
        return PropertiesSingleton.getProperty("toolkit.directory.board");
      case "Enemy":
        return "Enemies";
      case "Item":
        return "Items";
      case "Player":
        return "Characters";
      case "Program":
        return "Programs";
      case "Project":
        return "Projects";
      case "StatusEffect":
        return "Status Effects";
      case "Tileset":
        return "Tilesets";
      case "SpecialMove":
        return "Special Moves";
      default:
        return "Toolkit Files";
    }
  }

  public String[] getTypeExtensions(Class<? extends BasicType> type) {
    switch (type.getSimpleName()) {
      case "Animation":
        return new String[]{"anm"};
      case "Board":
        return new String[]{"brd", "brd.json"};
      case "Enemy":
        return new String[]{"ene"};
      case "Item":
        return new String[]{"itm"};
      case "Player":
        return new String[]{"tem"};
      case "Program":
        return new String[]{"prg"};
      case "Project":
        return new String[]{"gam", "gam.json"};
      case "StatusEffect":
        return new String[]{"ste"};
      case "Tileset":
        return new String[]{"tst"};
      case "SpecialMove":
        return new String[]{"spc", "spc.json"};
      default:
        return this.getTKFileExtensions();
    }
  }

  /**
   * Browse for aSystem.setProperty("project.path",
   * this.fileChooser.getCurrentDirectory().getParent() + File.separator +
   * PropertiesSingleton.getProperty("toolkit.directory.game") + File.separator + fileName +
   * File.separator);
   *
   * this.activeProject = new Project(this.fileChooser.getSelectedFile(),
   * System.getProperty("project.path"));
   *
   * ProjectEditor projectEditor = new ProjectEditor(this.activeProject);
   * this.desktopPane.add(projectEditor, BorderLayout.CENTER);
   *
   * projectEditor.addInternalFrameListener(this); projectEditor.setWindowParent(this);
   * projectEditor.toFront();
   *
   * this.selectToolkitWindow(projectEditor); this.setTitle(this.getTitle() + " - " +
   * this.activeProject.getGameTitle());
   *
   * this.menuBar.enableMenus(true); this.toolBar.toggleButtonStates(true); file of the given type,
   * starting in the subdirectory for that type, and return its location relative to the
   * subdirectory for that type. Filters by extensions relevant to that type. This is a shortcut
   * method for browseLocationBySubdir().
   *
   * @param type a BasicType class
   * @return the location of the file the user selects, relative to the subdirectory corresponding
   * to that type; or null if no file or an invalid file is selected (see browseLocationBySubdir())
   */
  public String browseByTypeRelative(Class<? extends BasicType> type) {
    File path = this.browseByType(type);
    if (path == null) {
      return null;
    }
    return this.getRelativePath(path,
            this.getPath(this.getTypeSubdirectory(type)));
  }

  /**
   * Browse for a file of the given type, starting in the subdirectory for that type, and return its
   * location. Filters by extensions relevant to that type. This is a shortcut method for
   * browseLocationBySubdir().
   *
   * @param type a BasicType class
   * @return the location of the file the user selects; or null if no file or an invalid file is
   * selected (see browseLocationBySubdir())
   */
  public File browseByType(Class<? extends BasicType> type) {
    String subdir = this.getTypeSubdirectory(type);
    String desc = getTypeFilterDescription(type);
    String[] exts = getTypeExtensions(type);
    return this.browseLocationBySubdir(subdir, desc, exts);
  }

  /**
   * Browse for a file of the given type, starting in the subdirectory for that type, and return its
   * location relative to the subdirectory for that type. The file may not exist yet if the user
   * types it in. Filters by extensions relevant to that type. This is a shortcut method for
   * saveLocationBySubdir().
   *
   * @param type a BasicType class
   * @return the location of the file the user selects, relative to the subdirectory corresponding
   * to that type; or null if no file or an invalid file is selected (see saveLocationBySubdir())
   */
  public String saveByTypeRelative(Class<? extends BasicType> type) {
    File path = this.saveByType(type);
    if (path == null) {
      return null;
    }
    return this.getRelativePath(path,
            this.getPath(this.getTypeSubdirectory(type)));
  }

  /**
   * Browse for a file of the given type, starting in the subdirectory for that type, and return its
   * location. The file may not exist yet if the user types it in. Filters by extensions relevant to
   * that type. This is a shortcut method for saveLocationBySubdir().
   *
   * @param type a BasicType class
   * @return the location of the file the user selects; or null if no file or an invalid file is
   * selected (see saveLocationBySubdir())
   */
  public File saveByType(Class<? extends BasicType> type) {
    String subdir = this.getTypeSubdirectory(type);
    String desc = getTypeFilterDescription(type);
    String[] exts = getTypeExtensions(type);
    return this.saveLocationBySubdir(subdir, desc, exts);
  }

  /**
   * Browse for a file with one of the given extensions, starting in the given subdirectory of the
   * project, and return its location.
   *
   * @param subdirectory where within the project to start the file chooser
   * @param description what to name the filter (for example, "Program Files")
   * @param extensions the file extensions to filter by (the portion of the file name after the last
   * ".")
   * @return the location of the file the user selects; or null if no file or an invalid file is
   * selected
   */
  public File browseLocationBySubdir(
          String subdirectory, String description, String... extensions) {
    File path = setFileChooserSubdirAndFilters(subdirectory, description, extensions);
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      if (validateFileChoice(path, extensions) == true) {
        return fileChooser.getSelectedFile();
      }
    }
    return null;
  }

  /**
   * Browse for a file with one of the given extensions, starting in the given subdirectory of the
   * project, and return its location. May return a new filename if the user types one rather than
   * selecting an existing file.
   *
   * @param subdirectory where within the project to start the file chooser
   * @param description what to name the filter (for example, "Program Files")
   * @param extensions the file extensions to filter by (the portion of the file name after the last
   * ".")
   * @return the location of the file the user selects; or null if no file or an invalid file is
   * selected
   */
  public File saveLocationBySubdir(
          String subdirectory, String description, String... extensions) {
    File path = setFileChooserSubdirAndFilters(subdirectory, description, extensions);
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      if (validateFileChoice(path, extensions) == true) {
        return fileChooser.getSelectedFile();
      }
    }
    return null;
  }

  public File getPath(String relativePath) {
    return new File(System.getProperty("project.path") + File.separator
            + relativePath);
  }

  public String getRelativePath(File fullPath) {
    return this.getRelativePath(fullPath,
            new File(System.getProperty("project.path") + File.separator));
  }

  public String getRelativePath(File fullPath, File relativeTo) {
    return fullPath.getPath().replace(
            relativeTo.getPath() + File.separator, "");
  }

  public void zoomInOnBoardEditor() {
    if (desktopPane.getSelectedFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) desktopPane.getSelectedFrame();
      editor.zoomIn();
    }
  }

  public void zoomOutOnBoardEditor() {
    if (desktopPane.getSelectedFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) desktopPane.getSelectedFrame();
      editor.zoomOut();
    }
  }

  public void toogleGridOnBoardEditor(boolean isVisible) {
    this.showGrid = isVisible;

    if (this.desktopPane.getSelectedFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) this.desktopPane.getSelectedFrame();
      editor.getBoardView().repaint();
    }
  }

  public void toogleCoordinatesOnBoardEditor(boolean isVisible) {
    this.showCoordinates = isVisible;

    if (desktopPane.getSelectedFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) desktopPane.getSelectedFrame();
      editor.getBoardView().repaint();
    }
  }

  public void toogleVectorsOnBoardEditor(boolean isVisible) {
    this.showVectors = isVisible;

    if (desktopPane.getSelectedFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) desktopPane.getSelectedFrame();
      editor.getBoardView().repaint();
    }
  }

  public void toogleProgramsOnBoardEditor(boolean isVisible) {
    this.showPrograms = isVisible;

    if (desktopPane.getSelectedFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) desktopPane.getSelectedFrame();
      editor.getBoardView().repaint();
    }
  }

  private void setupProject() {
    ProjectEditor projectEditor = new ProjectEditor(this.activeProject);
    this.desktopPane.add(projectEditor, BorderLayout.CENTER);

    projectEditor.addInternalFrameListener(this);
    projectEditor.toFront();

    this.selectToolkitWindow(projectEditor);
    this.setTitle(this.getTitle() + " - "
            + this.activeProject.getGameTitle());

    this.menuBar.enableMenus(true);
    this.toolBar.toggleButtonStates(true);
  }

  private void selectToolkitWindow(ToolkitEditorWindow window) {
    try {
      window.setSelected(true);
    } catch (PropertyVetoException ex) {
      Logger.getLogger(MainWindow.class.getName()).
              log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Sets the file chooser's directory to the given subdirectory of the project, sets its filter to
   * the given description and extensions, and returns its new file path.
   *
   * @param subdirectory
   * @param description
   * @param extensions
   * @return
   */
  private File setFileChooserSubdirAndFilters(
          String subdirectory, String description, String... extensions) {
    fileChooser.resetChoosableFileFilters();
    FileNameExtensionFilter filter = new FileNameExtensionFilter(
            description, extensions);
    fileChooser.setFileFilter(filter);
    File path = this.getPath(subdirectory);
    if (path.exists()) {
      fileChooser.setCurrentDirectory(path);
    }
    return path;
  }

  /**
   * Gets the location, relative to the given path, of the file chooser's currently selected file.
   * Returns null if the file does not end with one of the given extensions.
   *
   * @param extensions the file is required to end with one of these (do not include the dot that
   * comes immediately before the extension)
   * @param path the location will be relative to this path
   * @return the location of the currently selected file of one of the given extensions, relative to
   * the given path; or null if the extension does not match
   */
  private boolean validateFileChoice(File path, String... extensions) {
    String fileName = fileChooser.getSelectedFile().getName().toLowerCase();
    for (String ext : extensions) {
      if (fileName.endsWith("." + ext)) {
        return true;
      }
    }
    return false;
  }

  private class TileSetSelectionListener implements TileSelectionListener {

    @Override
    public void tileSelected(TileSelectionEvent e) {
      if (currentBrush instanceof ShapeBrush) {
        ((ShapeBrush) currentBrush).setTile(e.getTile());
        toolBar.getPencilButton().setSelected(true);
      } else if (currentBrush instanceof BucketBrush) {
        ((BucketBrush) currentBrush).setPourTile(e.getTile());
      } else {
        ShapeBrush shapeBrush = new ShapeBrush();
        shapeBrush.setTile(e.getTile());
        shapeBrush.makeRectangleBrush(
                new Rectangle(0, 0, 1, 1));
        currentBrush = shapeBrush;
        toolBar.getPencilButton().setSelected(true);
      }

      if (lastSelectedTile != e.getTile()) {
        lastSelectedTile = e.getTile();
      }
    }

    @Override
    public void tileRegionSelected(TileRegionSelectionEvent e) {
      if (!(currentBrush instanceof CustomBrush)) {
        currentBrush = new CustomBrush(e.getTiles());
      } else {
        ((CustomBrush) currentBrush).setTiles(e.getTiles());
      }

      toolBar.getPencilButton().setSelected(true);
    }
  }

}
