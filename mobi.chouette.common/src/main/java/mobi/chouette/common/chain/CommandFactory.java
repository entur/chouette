package mobi.chouette.common.chain;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;

public abstract class CommandFactory {

	   public static final Map<String, CommandFactory> factories = Collections.synchronizedMap(new HashMap<>());

	   protected abstract Command create(InitialContext context) throws IOException;

	   public static Command create(InitialContext context, String name)
	         throws ClassNotFoundException, IOException

	   {
	      if (!factories.containsKey(name))
	      {
	         Class.forName(name);
			 // log.info("[DSU] create : " + name);
	         if (!factories.containsKey(name))
	            throw new ClassNotFoundException(name);
	      }
	      return factories.get(name).create(context);
	   }
}
