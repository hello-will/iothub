package iothub.pubsub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SubTree {

    private Set<String> endpoints;
    private Map<String, SubTree> children;

    public SubTree() {
        endpoints = new HashSet<>();
        children = new HashMap<>();
    }

    public void Subscribe(String topic, String endpoint) throws Exception {
        SubTree tree = this;
        String[] nodes = topic.split("/");

        for (int i = 0; i < nodes.length; ++i) {
            SubTree temp = children.get(nodes[i]);
            if (temp == null) {
                temp = new SubTree();
                children.put(nodes[i], temp);
            }
            tree = temp;
        }

        tree.endpoints.add(endpoint);
    }

    public Set<String> GetEndpoints(String topic) {
        SubTree tree = this;
        String[] nodes = topic.split("/");

        for (int i = 0;
             i < nodes.length && tree != null;
             ++i) {
            tree = tree.children.get(nodes[i]);
        }

        if (tree != null) {
            return tree.endpoints;
        }

        return null;
    }
}
