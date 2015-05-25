import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.base.Predicates.not;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.options.TemplateOptions.Builder.runScript;

import java.io.*;
import java.util.*;

import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.util.Strings2;
import org.jclouds.compute.*;
import org.jclouds.ContextBuilder;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.compute.domain.*;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

import org.jclouds.compute.domain.*;

public class JCloudsGCE {


    public static String INVALID_SYNTAX = "Invalid number of parameters. Syntax is: provider identity credential groupName (add|exec|run|destroy)";

    public static void jclouds(String[] args) {

        String provider = "google-compute-engine";
        String identity = "969955727877-3q53n9vgjajebj9g7tigdosekedfviat@developer.gserviceaccount.com";
        String credential = "key.pem";
        String groupName = "instance-group-1";
        credential = getPrivateKeyFromFile(credential);

        ComputeService compute = initComputeService(provider, identity, credential);

        listNodes(compute);

    }

    private static void addNodesToGroup(ComputeService compute, String groupName){

        System.out.printf(">> adding node to group %s%n", groupName);

        TemplateBuilder templateBuilder = compute.templateBuilder().osFamily(OsFamily.CENTOS);

        // note this will create a user with the same name as you on the
        // node. ex. you can connect via ssh publicip
        Statement bootInstructions = AdminAccess.standard();
        templateBuilder.options(runScript(bootInstructions));
        NodeMetadata node = null;
        try {
            node = getOnlyElement(compute.createNodesInGroup(groupName, 1, templateBuilder.build()));
        } catch (RunNodesException e) {
            e.printStackTrace();
        }
        System.out.printf("<< node %s: %s%n", node.getId(),
                concat(node.getPrivateAddresses(), node.getPublicAddresses()));

    }
    private static void distroyAllNodesInGroup(ComputeService compute, String groupName){

        System.out.printf(">> destroying nodes in group %s%n", groupName);
        // you can use predicates to select which nodes you wish to destroy.
        Set<? extends NodeMetadata> destroyed = compute.destroyNodesMatching(//
                Predicates.<NodeMetadata> and(not(TERMINATED), inGroup(groupName)));
        System.out.printf("<< destroyed nodes %s%n", destroyed);

    }

    private static void listNodes(ComputeService compute){

        Set<? extends ComputeMetadata> nodeList = compute.listNodes();
        System.out.printf(">> No of nodes/instances %d\n", nodeList.size());
        for(ComputeMetadata nodeentry : nodeList) {
            System.out.println(">>>>  " + nodeentry);
        }
    }

    private static void listImages(ComputeService compute){

        Set<? extends Image> imageList = compute.listImages();
        System.out.printf(">> No of images %d\n", imageList.size());
        for(Image img : imageList) {
            System.out.println(">>>>  " + img);
        }

    }

    private static String getPrivateKeyFromFile(String filename) {
        try {
            return Strings2.toStringAndClose(new FileInputStream(filename));
        } catch (IOException e) {
            System.err.println("Exception : " + e);
            e.printStackTrace();
        }
        return null;
    }

    private static ComputeService initComputeService(String provider, String identity, String credential) {

        //initialize compute service

        Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule());

        ContextBuilder builder = ContextBuilder.newBuilder(provider)
                .credentials(identity, credential)
                .modules(modules);

        System.out.printf(">> initializing %s%n", builder.getApiMetadata());

        return builder.buildView(ComputeServiceContext.class).getComputeService();
    }
}
