package com.hortonworks.simpleyarnapp;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateResponse;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.client.api.NMClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.Apps;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

public class ApplicationMaster {

  public static void main(String[] args) throws Exception {

    final String command = args[0];
    final int n = Integer.valueOf(args[1]);
    final String jarpath = args[2];
    
    // Initialize clients to ResourceManager and NodeManagers
    Configuration conf = new YarnConfiguration();

    AMRMClient<ContainerRequest> rmClient = AMRMClient.createAMRMClient();
    rmClient.init(conf);
    rmClient.start();

    NMClient nmClient = NMClient.createNMClient();
    nmClient.init(conf);
    nmClient.start();

    // Register with ResourceManager
    System.out.println("registerApplicationMaster 0");
    rmClient.registerApplicationMaster("", 0, "");
    System.out.println("registerApplicationMaster 1");
    
    // Priority for worker containers - priorities are intra-application
    Priority priority = Records.newRecord(Priority.class);
    priority.setPriority(0);

    // Resource requirements for worker containers
    Resource capability = Records.newRecord(Resource.class);
    capability.setMemory(128);
    capability.setVirtualCores(1);

    // Make container requests to ResourceManager
    for (int i = 0; i < n; ++i) {
      ContainerRequest containerAsk = new ContainerRequest(capability, null, null, priority);
      System.out.println("Making res-req " + i);
      rmClient.addContainerRequest(containerAsk);
    }

      File packageFile = new File(jarpath);
      Path packagePath = new Path(jarpath);
      //URL packageUrl = ConverterUtils.getYarnUrlFromPath(
      //        FileContext.getFileContext().makeQualified(new Path(jarpath)));
      URL packageUrl = ConverterUtils.getYarnUrlFromPath(new Path(jarpath));

      LocalResource packageResource = Records.newRecord(LocalResource.class);

      FileSystem fs = FileSystem.get(conf);
      System.out.println("FILE SYSTEM : " + fs.getScheme());

      FileStatus jarStat = FileSystem.get(conf).getFileStatus(packagePath);
      packageResource.setResource(packageUrl);
      packageResource.setSize(jarStat.getLen());
      packageResource.setTimestamp(jarStat.getModificationTime());
      packageResource.setType(LocalResourceType.FILE);
      packageResource.setVisibility(LocalResourceVisibility.PUBLIC);

    // Obtain allocated containers, launch and check for responses
    int responseId = 0;
    int completedContainers = 0;
    int containerId = 0;
    while (completedContainers < n) {
        AllocateResponse response = rmClient.allocate(responseId++);
        for (Container container : response.getAllocatedContainers()) {
            // Launch container by create ContainerLaunchContext
            ContainerLaunchContext ctx =
                    Records.newRecord(ContainerLaunchContext.class);
            ctx.setCommands(
                    Collections.singletonList(
                            "$JAVA_HOME/bin/java -Xmx256M " +
                            command + " " + containerId +
                                    " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout" +
                                    " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr"
                    ));
            ctx.setLocalResources(
                    Collections.singletonMap("package.jar", packageResource));

            Map<String, String> appMasterEnv = new HashMap<String, String>();
            for (String c : conf.getStrings(
                    YarnConfiguration.YARN_APPLICATION_CLASSPATH,
                    YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
                Apps.addToEnvironment(appMasterEnv, ApplicationConstants.Environment.CLASSPATH.name(),
                        c.trim());
            }
            Apps.addToEnvironment(appMasterEnv,
                    ApplicationConstants.Environment.CLASSPATH.name(),
                    ApplicationConstants.Environment.PWD.$() + File.separator + "*");
            ctx.setEnvironment(appMasterEnv);

            System.out.println("Launching container " + container.getId() + ", " + container.getNodeHttpAddress());
            System.out.println("Command " + command);
            nmClient.startContainer(container, ctx);
        }
        for (ContainerStatus status : response.getCompletedContainersStatuses()) {
            ++completedContainers;
            System.out.println("Completed container " + status.getContainerId());
	    System.out.println("Diagonostics container " + status.getDiagnostics());
            System.out.println("Exit status container " + status.getExitStatus());
        }
        Thread.sleep(100);
    }

    // Un-register with ResourceManager
    rmClient.unregisterApplicationMaster(
        FinalApplicationStatus.SUCCEEDED, "", "");
  }
}
