package org.xdi.oxd.web.ws;

import com.google.inject.Inject;
import junit.framework.Assert;
import org.testng.annotations.Guice;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.web.TestAppModule;

/**
 * Created by yuriy on 8/23/2015.
 */
@Guice(modules = TestAppModule.class)
public class CommandWsTest {

    @Inject
    CommandWS commandWS;

    public void smokeTest() {
        Command command = new Command();
        CommandResponse response = commandWS.execute(command);

        Assert.assertNotNull(response);
    }
}
