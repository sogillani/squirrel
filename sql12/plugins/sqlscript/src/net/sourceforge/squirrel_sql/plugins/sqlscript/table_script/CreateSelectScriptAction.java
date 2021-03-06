package net.sourceforge.squirrel_sql.plugins.sqlscript.table_script;

import java.awt.event.ActionEvent;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.action.IObjectTreeAction;
import net.sourceforge.squirrel_sql.fw.resources.IResources;
import net.sourceforge.squirrel_sql.plugins.sqlscript.SQLScriptPlugin;

public class CreateSelectScriptAction extends SquirrelAction  implements IObjectTreeAction
{

   /**
    * Current session.
    */
   private ISession _session;

   /**
    * Current plugin.
    */
   private final SQLScriptPlugin _plugin;

   public CreateSelectScriptAction(IApplication app, IResources resources, SQLScriptPlugin plugin)
   {
      super(app, resources);
      _plugin = plugin;
   }

   public void actionPerformed(ActionEvent evt)
   {
      if (_session != null)
      {
         new CreateSelectScriptCommand(_session, _plugin).execute();
      }
   }

   /**
    * Set the current session.
    *
    * @param   session      The current session.
    */
   public void setSession(ISession session)
   {
      _session = session;
   }

   public void setObjectTree(IObjectTreeAPI tree)
   {
      if (null != tree)
      {
         _session = tree.getSession();
      }
      else
      {
         _session = null;
      }
      setEnabled(null != _session);
   }
}
