package net.sourceforge.squirrel_sql.client.gui.desktopcontainer.docktabdesktop;

import net.sourceforge.squirrel_sql.client.gui.desktopcontainer.*;
import net.sourceforge.squirrel_sql.client.gui.desktopcontainer.docktabdesktop.DockHandle;
import net.sourceforge.squirrel_sql.client.gui.desktopcontainer.docktabdesktop.VerticalToggleButton;
import net.sourceforge.squirrel_sql.client.gui.desktopcontainer.docktabdesktop.TabHandle;
import net.sourceforge.squirrel_sql.client.gui.mainframe.SquirrelDesktopManager;
import net.sourceforge.squirrel_sql.client.IApplication;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

//
public class DockTabDesktopPane extends JComponent implements IDesktopContainer
{
   private HashSet<TabHandle> _handlesInRemoveTab_CloseButton = new HashSet<TabHandle>();
   private HashSet<TabHandle> _handlesInRemoveTab_Dispose = new HashSet<TabHandle>();


   public enum TabClosingMode
   {
      CLOSE_BUTTON,
      DISPOSE
   }

   private IApplication _app;

   private JPanel _pnlButtons = new JPanel();

   private DockPanel _pnlDock = new DockPanel();

   private DesktopTabbedPane _tabbedPane = new DesktopTabbedPane();

   private JSplitPane _split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
   private boolean _inOnToggleOpenDock;
   private ArrayList<DockHandle> _dockHandles = new ArrayList<DockHandle>();
   private ArrayList<TabHandle> _tabHandles = new ArrayList<TabHandle>();
   private int _standardDividerSize;

   private DockTabDesktopManager _dockTabDesktopManager = new DockTabDesktopManager();


   public DockTabDesktopPane(IApplication app)
   {
      _app = app;

      setLayout(new BorderLayout());

      _pnlButtons = new JPanel();
      _pnlButtons.setLayout(new BoxLayout(_pnlButtons, BoxLayout.Y_AXIS));
      add(_pnlButtons, BorderLayout.WEST);

      _split.setLeftComponent(_pnlDock);


      _tabbedPane.addChangeListener(new ChangeListener()
      {
         public void stateChanged(ChangeEvent e)
         {
            onTabStateChanged(e);
         }
      });

      _split.setRightComponent(_tabbedPane);


      add(_split, BorderLayout.CENTER);
      _standardDividerSize = _split.getDividerSize();

      _pnlDock.addComponentListener(new ComponentAdapter()
      {
         public void componentResized(ComponentEvent e)
         {
            onDockPanelResized();
         }
      });

      closeDock();
   }

   private void onTabStateChanged(ChangeEvent e)
   {
      tabActivationChanged();
   }

   private void tabActivationChanged()
   {
      TabPanel tabPanel = (TabPanel) _tabbedPane.getSelectedComponent();

      if (null != tabPanel && tabPanel.getTabHandle().isSelected())
      {
         return;
      }

      for (TabHandle tabHandle : _tabHandles)
      {
         if (null == tabPanel || tabHandle.isSelected() && tabHandle != tabPanel.getTabHandle())
         {
            tabHandle.setSelected(false);
         }
      }

      if (tabPanel != null)
      {
         tabPanel.getTabHandle().setSelected(true);
      }
   }


   public void addWidget(DockWidget widget)
   {
      DockHandle dockHandle = addDock(widget.getContentPane(), widget.getTitle());
      ((DockDelegate) widget.getDelegate()).setDockHandle(dockHandle);
   }

   public void addWidget(DialogWidget client)
   {
   }

   public void addWidget(TabWidget widget)
   {
      final TabHandle tabHandle = new TabHandle(widget, this);
      ((TabDelegate) widget.getDelegate()).setTabHandle(tabHandle);

      tabHandle.addTabHandleListener(_dockTabDesktopManager);

      TabPanel tabPanel = createTabPanel(tabHandle);
      _tabbedPane.addTab(widget.getTitle(), null, tabPanel, widget.getTitle());
      int tabIx = _tabbedPane.indexOfComponent(tabPanel);

      ButtonTabComponent btc = (ButtonTabComponent) _tabbedPane.getTabComponentAt(tabIx);

      btc.getButton().addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            removeTab(tabHandle, e, TabClosingMode.CLOSE_BUTTON);
         }
      });

      _tabHandles.add(tabHandle);
      tabHandle.fireAdded();
      _tabbedPane.setSelectedIndex(tabIx);
   }

   private void onDockPanelResized()
   {
      if (0 == _pnlDock.getComponentCount())
      {
         // Without this the split moves when the window is resized.
         closeDock();
      }
   }

   private void closeDock()
   {
      _split.setDividerLocation(0);
      _split.setDividerSize(0);
   }


   private void onToggleOpenDock(DockHandle handle, JToggleButton btn, ActionEvent e)
   {
      if (_inOnToggleOpenDock)
      {
         return;
      }

      try
      {
         _inOnToggleOpenDock = true;

         for (int i = 0; i < _pnlButtons.getComponents().length; i++)
         {
            JToggleButton btnBuf = (JToggleButton) _pnlButtons.getComponents()[i];

            if (btnBuf != btn)
            {
               btnBuf.setSelected(false);
            }
         }


         if (1 == _pnlDock.getComponentCount())
         {
            DockHandle lastHandle = getDockHandleForComponent(_pnlDock.getComponent(0));
            lastHandle.storeDividerLocation(_split.getDividerLocation());
            _pnlDock.remove(0);
            lastHandle.wasClosedByOtherButton(e);
         }

         if (btn.isSelected())
         {
            _pnlDock.add(handle.getDockFrame());
            _split.setDividerSize(_standardDividerSize);
            _split.setDividerLocation(handle.getDividerLocation());

         }
         else
         {
            closeDock();
         }
      }
      finally
      {
         _inOnToggleOpenDock = false;
      }
   }

   private DockHandle getDockHandleForComponent(Component component)
   {
      for (DockHandle dockHandle : _dockHandles)
      {
         if (dockHandle.getDockFrame() == component)
         {
            return dockHandle;
         }
      }

      return null;
   }

   private DockHandle addDock(Container comp, String title)
   {
      final VerticalToggleButton btn = new VerticalToggleButton(title);

      _pnlButtons.add(btn);

      final DockHandle handle = new DockHandle(_app, comp, title, btn);

      btn.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onToggleOpenDock(handle, btn, e);
         }
      });

      _dockHandles.add(handle);

      return handle;
   }


   private TabPanel createTabPanel(final TabHandle tabHandle)
   {
      return new TabPanel(tabHandle);
   }

   public void selectTab(TabHandle tabHandle)
   {
      TabHandle selectedHandle = getSelectedHandle();
      if(selectedHandle == tabHandle)
      {
         return;
      }
      selectedHandle.fireDeselected(null);
      int tabIx = getTabIndex(tabHandle);
      _tabbedPane.setSelectedIndex(tabIx);
   }


   void removeTab(TabHandle tabHandle, ActionEvent e, TabClosingMode tabClosingMode)
   {


      if(TabClosingMode.CLOSE_BUTTON == tabClosingMode)
      {
         try
         {
            if(_handlesInRemoveTab_CloseButton.contains(tabHandle))
            {
               return;
            }

            _handlesInRemoveTab_CloseButton.add(tabHandle);
            tabHandle.fireClosing(e);
            int tabIndex = getTabIndex(tabHandle);
            if(-1 != tabIndex)
            {
               _tabbedPane.remove(tabIndex);
            }
            tabHandle.fireClosed(e);
            _tabHandles.remove(tabHandle);
            tabHandle.removeTabHandleListener(_dockTabDesktopManager);
         }
         finally
         {
            _handlesInRemoveTab_CloseButton.remove(tabHandle);
         }
      }
      else if(TabClosingMode.DISPOSE == tabClosingMode)
      {
         try
         {
            if(_handlesInRemoveTab_Dispose.contains(tabHandle))
            {
               return;
            }

            _handlesInRemoveTab_Dispose.add(tabHandle);
            tabHandle.getWidget().setVisible(false);
            tabHandle.fireDeselected(e);
            int tabIndex = getTabIndex(tabHandle);
            if(-1 != tabIndex)
            {
               _tabbedPane.remove(tabIndex);
            }

            //tabHandle.fireClosed(e); is done in dispose itself because listeners must befired even in DO_NOTHING_ON_CLOSE mode

            _tabHandles.remove(tabHandle);
            tabHandle.removeTabHandleListener(_dockTabDesktopManager);
         }
         finally
         {
            _handlesInRemoveTab_Dispose.remove(tabHandle);
         }
      }
      else
      {
         throw new IllegalArgumentException("Unknown TabClosingMode: " + tabClosingMode);
      }
   }


   public IWidget[] getAllWidgets()
   {
      IWidget[] ret = new IWidget[_tabHandles.size()];

      for (int i = 0; i < ret.length; i++)
      {
         ret[i] = _tabHandles.get(i).getWidget();
      }

      return ret;
   }

   public IWidget getSelectedWidget()
   {
      TabHandle selectedHandle = getSelectedHandle();

      if(null == selectedHandle)
      {
         return null;
      }

      return selectedHandle.getWidget();
   }

   private TabHandle getSelectedHandle()
   {
      TabPanel selComp = (TabPanel) _tabbedPane.getSelectedComponent();

      if (null == selComp)
      {
         return null;
      }

      for (TabHandle tabHandle : _tabHandles)
      {
         if (tabHandle == selComp.getTabHandle())
         {
            return tabHandle;
         }
      }

      return null;
   }





   private int getTabIndex(TabHandle tabHandle)
   {
      for (int i = 0; i < _tabbedPane.getTabCount(); ++i)
      {
         TabPanel comp = (TabPanel) _tabbedPane.getComponentAt(i);

         if (comp.getTabHandle() == tabHandle)
         {
            return i;
         }
      }

      return -1;
   }


   public Dimension getRequiredSize()
   {
      return getPreferredSize();
   }

   public JComponent getComponent()
   {
      return this;
   }



   public void setDesktopManager(SquirrelDesktopManager squirrelDesktopManager)
   {
      _dockTabDesktopManager.setSquirrelDesktopManager(squirrelDesktopManager);
   }

   public void setTabTitle(TabHandle tabHandle, String title)
   {
      _tabbedPane.setTitleAt(getTabIndex(tabHandle), title);
   }

   public String getTabTitle(TabHandle tabHandle)
   {
      int index = getTabIndex(tabHandle);

      if(-1 == index)
      {
         return null;
      }
      return _tabbedPane.getTitleAt(index);
   }

   public void setTabIcon(TabHandle tabHandle, Icon frameIcon)
   {
      _tabbedPane.setIconAt(getTabIndex(tabHandle), frameIcon);
   }


   public void putClientProperty(String key, String value)
   {
   }

}